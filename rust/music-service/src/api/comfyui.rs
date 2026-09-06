use std::env::temp_dir;
use crate::api::{Model};
use axum::http::StatusCode;
use base64::Engine;
use uuid::Uuid;

const COMFY_BASE_URL: &str = "https://cloud.comfy.org";

const COMFY_POLL_INTERVAL: std::time::Duration = std::time::Duration::from_secs(2);

pub async fn comfyui_ltx2_3a2v(mut row: Model) -> Result<Model, (StatusCode, String)> {
    let Model::Ltx2_3A2V {
        image,
        audio,
        prompt,
        ..
    } = row.clone()
    else {
        return Err((
            StatusCode::BAD_REQUEST,
            "comfyui_ltx2_3a2v requires an Ltx2_3A2V action".to_string(),
        ));
    };
    let client = reqwest::Client::new();
    let image = comfy_upload_input(&client, &image).await?;
    let audio = comfy_upload_input(&client, &audio).await?;

    let mut wf: serde_json::Value =
        serde_json::from_str(crate::commands::serve::COMFY_LTX2_3A2V_WORKFLOW_LOCK.get().unwrap())
            .map_err(|e| {
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("parsing workflow: {e}"),
                )
            })?;
    wf["269"]["inputs"]["image"] = serde_json::json!(image);
    wf["276"]["inputs"]["audio"] = serde_json::json!(audio);
    wf["276"]["inputs"]["audioUI"] =
        serde_json::json!(format!("/api/view?filename={audio}&type=input&subfolder=&"));
    wf["340:319"]["inputs"]["value"] = serde_json::json!(prompt);
    // Fresh seed per request — the app randomizes seeds on each Generate; over the
    // API the exported workflow's fixed seed makes every run identical, which comfy
    // serves from cache with empty outputs. Randomize so it always executes.
    wf["340:285"]["inputs"]["noise_seed"] = serde_json::json!(Uuid::now_v7().as_u128() as u64);
    wf["340:286"]["inputs"]["noise_seed"] = serde_json::json!(Uuid::now_v7().as_u128() as u64);

    let queued: serde_json::Value = client.post(format!("{COMFY_BASE_URL}/api/prompt"))
        .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
        .json(&serde_json::json!({ "prompt": wf, "extra_data": { "api_key_comfy_org": crate::commands::serve::COMFY_KEY_LOCK.get().unwrap() } }))
        .send().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("POST /api/prompt: {e}")))?
        .json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("decoding prompt response: {e}")))?;
    let req_id = queued
        .get("prompt_id")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("prompt response missing prompt_id: {queued}"),
            )
        })?
        .to_string();
    // Record comfy's prompt_id on the action, then poll comfy until the job is
    // terminal and return the row with the result.
    if let Model::Ltx2_3A2V {
        comfy_request_id, ..
    } = &mut row
    {
        *comfy_request_id = req_id.clone();
    }
    loop {
        let job: serde_json::Value = client
            .get(format!("{COMFY_BASE_URL}/api/jobs/{req_id}"))
            .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
            .send()
            .await
            .and_then(|r| r.error_for_status())
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("GET job: {e}")))?
            .json()
            .await
            .map_err(|e| {
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("decoding job response: {e}"),
                )
            })?;

        match job.get("status").and_then(|v| v.as_str()) {
            Some("completed") => {
                let msgs = job
                    .pointer("/execution_status/messages")
                    .and_then(|v| v.as_array())
                    .ok_or_else(|| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing execution_status.messages".to_string(),
                        )
                    })?;
                let ts = |name: &str| {
                    msgs.iter().find_map(|m| {
                        let a = m.as_array()?;
                        if a.first()?.as_str()? == name {
                            a.get(1)?.get("timestamp")?.as_f64()
                        } else {
                            None
                        }
                    })
                };
                let (_start, _end) = match (ts("execution_start"), ts("execution_success")) {
                    (Some(s), Some(e)) => (s, e),
                    _ => {
                        return Err((
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing execution timestamps".to_string(),
                        ));
                    }
                };

                let output_filename = job
                    .pointer("/preview_output/filename")
                    .and_then(|v| v.as_str())
                    .ok_or_else(|| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing preview_output.filename".to_string(),
                        )
                    })?;
                let bytes = client
                    .get(format!(
                        "{COMFY_BASE_URL}/api/view?filename={output_filename}&type=output"
                    ))
                    .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
                    .send()
                    .await
                    .map_err(|e| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            format!("GET output {output_filename}: {e}"),
                        )
                    })?
                    .bytes()
                    .await
                    .map_err(|e| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            format!("reading output {output_filename}: {e}"),
                        )
                    })?;
                // Return the result as base64 data rather than re-hosting a file on disk.
                let data = base64::engine::general_purpose::STANDARD.encode(&bytes);


                if let Model::Ltx2_3A2V { file, .. } = &mut row {
                    *file = data;
                }
                return Ok(row);
            }
            Some("error") | Some("cancelled") => {
                return Err((
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("comfy job {req_id} ended: {:?}", job.get("status")),
                ));
            }
            _ => tokio::time::sleep(COMFY_POLL_INTERVAL).await,
        }
    }
}

// Flux2 Klein image edit: two reference images (node 76 = subject, node 81 =
// reference) + a positive prompt (node 92:113) → one edited image. Same upload /
// submit / poll / download / base64 shape as the ltx flow above.
pub async fn comfyui_flux2_klein_i2i(mut row: Model) -> Result<Model, (StatusCode, String)> {
    let Model::Flux2KleinI2I {
        image,
        image2,
        prompt,
        ..
    } = row.clone()
    else {
        return Err((
            StatusCode::BAD_REQUEST,
            "comfyui_flux2_klein_i2i requires a Flux2KleinI2I action".to_string(),
        ));
    };
    let client = reqwest::Client::new();
    let image = comfy_upload_input(&client, &image).await?;
    let image2 = comfy_upload_input(&client, &image2).await?;

    let mut wf: serde_json::Value = serde_json::from_str(
        crate::commands::serve::COMFY_FLUX2_KLEIN_I2I_WORKFLOW_LOCK
            .get()
            .unwrap(),
    )
    .map_err(|e| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("parsing workflow: {e}"),
        )
    })?;
    wf["76"]["inputs"]["image"] = serde_json::json!(image);
    wf["81"]["inputs"]["image"] = serde_json::json!(image2);
    wf["92:113"]["inputs"]["text"] = serde_json::json!(prompt);
    // Fresh seed per request (see comfyui_ltx2_3a2v) — avoids comfy's empty-output cache hit.
    wf["92:105"]["inputs"]["noise_seed"] = serde_json::json!(Uuid::now_v7().as_u128() as u64);

    let queued: serde_json::Value = client.post(format!("{COMFY_BASE_URL}/api/prompt"))
        .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
        .json(&serde_json::json!({ "prompt": wf, "extra_data": { "api_key_comfy_org": crate::commands::serve::COMFY_KEY_LOCK.get().unwrap() } }))
        .send().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("POST /api/prompt: {e}")))?
        .json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("decoding prompt response: {e}")))?;
    let req_id = queued
        .get("prompt_id")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("prompt response missing prompt_id: {queued}"),
            )
        })?
        .to_string();
    if let Model::Flux2KleinI2I {
        comfy_request_id, ..
    } = &mut row
    {
        *comfy_request_id = req_id.clone();
    }
    loop {
        let job: serde_json::Value = client
            .get(format!("{COMFY_BASE_URL}/api/jobs/{req_id}"))
            .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
            .send()
            .await
            .and_then(|r| r.error_for_status())
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("GET job: {e}")))?
            .json()
            .await
            .map_err(|e| {
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("decoding job response: {e}"),
                )
            })?;

        match job.get("status").and_then(|v| v.as_str()) {
            Some("completed") => {
                let msgs = job
                    .pointer("/execution_status/messages")
                    .and_then(|v| v.as_array())
                    .ok_or_else(|| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing execution_status.messages".to_string(),
                        )
                    })?;
                let ts = |name: &str| {
                    msgs.iter().find_map(|m| {
                        let a = m.as_array()?;
                        if a.first()?.as_str()? == name {
                            a.get(1)?.get("timestamp")?.as_f64()
                        } else {
                            None
                        }
                    })
                };
                let (_start, _end) = match (ts("execution_start"), ts("execution_success")) {
                    (Some(s), Some(e)) => (s, e),
                    _ => {
                        return Err((
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing execution timestamps".to_string(),
                        ));
                    }
                };

                let output_filename = job
                    .pointer("/preview_output/filename")
                    .and_then(|v| v.as_str())
                    .ok_or_else(|| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing preview_output.filename".to_string(),
                        )
                    })?;
                let bytes = client
                    .get(format!(
                        "{COMFY_BASE_URL}/api/view?filename={output_filename}&type=output"
                    ))
                    .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
                    .send()
                    .await
                    .map_err(|e| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            format!("GET output {output_filename}: {e}"),
                        )
                    })?
                    .bytes()
                    .await
                    .map_err(|e| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            format!("reading output {output_filename}: {e}"),
                        )
                    })?;
                // Return the result as base64 data rather than re-hosting a file on disk.
                let data = base64::engine::general_purpose::STANDARD.encode(&bytes);

                if let Model::Flux2KleinI2I { file, .. } = &mut row {
                    *file = data;
                }
                return Ok(row);
            }
            Some("error") | Some("cancelled") => {
                return Err((
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("comfy job {req_id} ended: {:?}", job.get("status")),
                ));
            }
            _ => tokio::time::sleep(COMFY_POLL_INTERVAL).await,
        }
    }
}

// Flux2 Dev image edit: one input image (node 46) + a positive prompt (node 68:6)
// → one edited image. Same upload / submit / poll / download / base64 shape as above.
pub async fn comfyui_flux2_dev_i2i(mut row: Model) -> Result<axum::body::Body, (StatusCode, String)> {
    let Model::Flux2DevI2I { image, prompt, .. } = row.clone() else {
        return Err((
            StatusCode::BAD_REQUEST,
            "comfyui_flux2_dev_i2i requires a Flux2DevI2I action".to_string(),
        ));
    };
    let client = reqwest::Client::new();

    let image = comfy_upload_input(&client, &image).await?;

    let mut wf: serde_json::Value = serde_json::from_str(
        crate::commands::serve::COMFY_FLUX2_DEV_I2I_WORKFLOW_LOCK
            .get()
            .unwrap(),
    )
    .map_err(|e| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("parsing workflow: {e}"),
        )
    })?;
    wf["46"]["inputs"]["image"] = serde_json::json!(image);
    wf["68:6"]["inputs"]["text"] = serde_json::json!(prompt);
    // Fresh seed per request (see comfyui_ltx2_3a2v) — avoids comfy's empty-output cache hit.
    wf["68:25"]["inputs"]["noise_seed"] = serde_json::json!(Uuid::now_v7().as_u128() as u64);

    let queued: serde_json::Value = client.post(format!("{COMFY_BASE_URL}/api/prompt"))
        .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
        .json(&serde_json::json!({ "prompt": wf, "extra_data": { "api_key_comfy_org": crate::commands::serve::COMFY_KEY_LOCK.get().unwrap() } }))
        .send().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("POST /api/prompt: {e}")))?
        .json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("decoding prompt response: {e}")))?;
    let req_id = queued
        .get("prompt_id")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("prompt response missing prompt_id: {queued}"),
            )
        })?
        .to_string();
    let comfy_request_id = req_id.clone();
    loop {
        let job: serde_json::Value = client
            .get(format!("{COMFY_BASE_URL}/api/jobs/{req_id}"))
            .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
            .send()
            .await
            .and_then(|r| r.error_for_status())
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("GET job: {e}")))?
            .json()
            .await
            .map_err(|e| {
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("decoding job response: {e}"),
                )
            })?;

        match job.get("status").and_then(|v| v.as_str()) {
            Some("completed") => {
                let msgs = job
                    .pointer("/execution_status/messages")
                    .and_then(|v| v.as_array())
                    .ok_or_else(|| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing execution_status.messages".to_string(),
                        )
                    })?;
                let ts = |name: &str| {
                    msgs.iter().find_map(|m| {
                        let a = m.as_array()?;
                        if a.first()?.as_str()? == name {
                            a.get(1)?.get("timestamp")?.as_f64()
                        } else {
                            None
                        }
                    })
                };
                let (_start, _end) = match (ts("execution_start"), ts("execution_success")) {
                    (Some(s), Some(e)) => (s, e),
                    _ => {
                        return Err((
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing execution timestamps".to_string(),
                        ));
                    }
                };

                let output_filename = job
                    .pointer("/preview_output/filename")
                    .and_then(|v| v.as_str())
                    .ok_or_else(|| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            "job missing preview_output.filename".to_string(),
                        )
                    })?;

                let bytes = client
                    .get(format!(
                        "{COMFY_BASE_URL}/api/view?filename={output_filename}&type=output"
                    ))
                    .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
                    .send()
                    .await
                    .map_err(|e| {
                        (
                            StatusCode::INTERNAL_SERVER_ERROR,
                            format!("GET output {output_filename}: {e}"),
                        )
                    })?.bytes_stream();
                return Ok(axum::body::Body::from_stream(bytes))

                // let bytes = client
                //     .get(format!(
                //         "{COMFY_BASE_URL}/api/view?filename={output_filename}&type=output"
                //     ))
                //     .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
                //     .send()
                //     .await
                //     .map_err(|e| {
                //         (
                //             StatusCode::INTERNAL_SERVER_ERROR,
                //             format!("GET output {output_filename}: {e}"),
                //         )
                //     })?
                //     .bytes()
                //     .await
                //     .map_err(|e| {
                //         (
                //             StatusCode::INTERNAL_SERVER_ERROR,
                //             format!("reading output {output_filename}: {e}"),
                //         )
                //     })?;
                // // Return the result as base64 data rather than re-hosting a file on disk.
                // let data = base64::engine::general_purpose::STANDARD.encode(&bytes);
                //
                // if let Model::Flux2DevI2I { file, .. } = &mut row {
                //     *file = data;
                // }
                // return Ok(row);
            }
            Some("error") | Some("cancelled") => {
                return Err((
                    StatusCode::INTERNAL_SERVER_ERROR,
                    format!("comfy job {req_id} ended: {:?}", job.get("status")),
                ));
            }
            _ => tokio::time::sleep(COMFY_POLL_INTERVAL).await,
        }
    }
}

// Input is base64 in whichever form the client's platform produces natively:
// web sends a data URI (`data:<mime>;base64,<b64>`), android/ios send raw
// base64. Drop the data-URI prefix if present, then sniff the real file type
// from the decoded bytes — comfy keys off the upload's extension, so we derive
// it rather than trust any declared mime. Uploads under a fresh name and
// returns comfy's stored name.
async fn comfy_upload_input(
    client: &reqwest::Client,
    image: &str,
) -> Result<String, (StatusCode, String)> {
    let bytes = tokio::fs::read(temp_dir().join(image)).await.map_err(|e| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("could not read file: {e}"),
        )
    })?;
    // let bytes = base64::engine::general_purpose::STANDARD
    //     .decode(image)
    //     .map_err(|e| {
    //         (
    //             StatusCode::BAD_REQUEST,
    //             format!("decoding base64 input: {e}"),
    //         )
    //     })?;
    let ext = infer::get(&bytes)
        .ok_or_else(|| {
            (
                StatusCode::BAD_REQUEST,
                "unrecognized input file type".to_string(),
            )
        })?
        .extension();
    let file_name = format!("{}.{ext}", Uuid::now_v7());
    let part = reqwest::multipart::Part::bytes(bytes).file_name(file_name);
    let form = reqwest::multipart::Form::new().part("image", part);
    let resp = client
        .post(format!("{COMFY_BASE_URL}/api/upload/image"))
        .header("X-API-Key", crate::commands::serve::COMFY_KEY_LOCK.get().unwrap())
        .multipart(form)
        .send()
        .await
        .map_err(|e| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("uploading input: {e}"),
            )
        })?;
    let status = resp.status();
    let payload: serde_json::Value = resp.json().await.map_err(|e| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("decoding upload response: {e}"),
        )
    })?;
    if !status.is_success() {
        return Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("provider upload HTTP {status}: {payload}"),
        ));
    }
    payload
        .get("name")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("upload response missing name: {payload}"),
            )
        })
        .map(str::to_string)
}
