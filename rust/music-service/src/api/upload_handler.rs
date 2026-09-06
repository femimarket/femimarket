use std::collections::HashMap;
use std::env::temp_dir;
use std::path::{Path, PathBuf};
use axum::extract::Multipart;
use axum::extract::multipart::Field;
use axum::http::StatusCode;
use serde::de::DeserializeOwned;
use serde::Serialize;

#[macro_export]
macro_rules! upload_handler {
    (
        $handler_name:ident,
        $struct_in:ty,
        $struct_out:ty,
        { $($hdr_name:ident : $hdr_key:literal),* $(,)? },
        [$($form_file:literal),* $(,)?]
        $(, $sub_route:expr)?
    ) => {
        #[utoipa::path(
            post,
            path = concat!("/", stringify!($handler_name)),
            request_body(content_type = "multipart/form-data", content = $struct_in),
            responses(
                (status = 200, description = "Upload successful", body = $struct_out),
                (status = 500, description = "Internal server error", body = String)
            )
        )]
        pub async fn $handler_name(
            _headers: axum::http::HeaderMap,
            multipart: axum::extract::Multipart,
        ) -> impl axum::response::IntoResponse  {
            $(
                let $hdr_name = _headers
                    .get($hdr_key)
                    .and_then(|v| v.to_str().ok())
                    .map(str::to_string)
                    .unwrap_or_default();
            )*
            let v = handle_form::<$struct_in>(multipart, &[$($form_file),*])
                .await
                .map_err(|err| {
                    tracing::error!("{}: {}", err.0, err.1);
                    (axum::http::StatusCode::INTERNAL_SERVER_ERROR, err.1)
                })?;
                let response_data = upload_handler!(@result v, [$($hdr_name),*] $(, $sub_route)?);
            Ok(axum::Json(response_data))
        }
    };
    (@result $v:ident, [ $($hdr_name:ident),* $(,)? ]) => {
        serde_json::to_value(&$v).unwrap_or_default()
    };
    (@result $v:ident, [ $($hdr_name:ident),* $(,)? ], $sub_route:expr) => {
        match $sub_route($v $(, $hdr_name)*).await {
            Ok(result) => result,
            Err(err) => {
                tracing::error!("{}: {}", err.0, err.1);
                return Err((axum::http::StatusCode::INTERNAL_SERVER_ERROR, err.1));
            }
        }
    };
}


pub async fn handle_form<T: Serialize + DeserializeOwned + std::fmt::Debug>(
    mut form: Multipart,
    file_fields: &[&str],
) -> Result<T, (StatusCode, String)> {
    let dir = temp_dir();

    let mut data = HashMap::<String, String>::new();

    while let Ok(Some(field)) = form.next_field().await {
        let field_name = field.name().unwrap_or_default().to_owned();

        let is_file = file_fields.iter().any(|x| field_name.contains(x));

        if is_file {
            let file_name = field.file_name().clone().unwrap_or_default().to_string();

            // THIS ONLY INSERTS IF
            // - HIDDEN FIELD IS NOT EMPTY
            // - HASHMAP ENTRY IS EMPTY
            if file_name.is_empty() {
                let raw = field
                    .text()
                    .await
                    .map_err(|err| (StatusCode::BAD_REQUEST, err.to_string()))?;
                // log_field(&field_name, &raw);
                if !raw.is_empty() && data.get(&field_name).map_or(true, |v| v.is_empty()) {
                    data.insert(field_name.clone(), raw);
                }
                continue;
            }

            // PROCESS 2ND
            // FILE PICKER USED
            data.insert(field_name.clone(), file_name.clone());

            if field.content_type().is_none() {
                tracing::error!("Missing content type for {}", field_name);
                return Err((
                    StatusCode::BAD_REQUEST,
                    format!("Missing content type for {}", field_name),
                ));
            }

            download_form_file(field, &dir).await?;

            // File::s3_upload_file(path.clone(), content_type)
            //     .await
            //     .map_err(|err| internal_error(err.to_string()))?;

            // log_field(&field_name, &file_name);
            continue;
        } else {
            let raw = field
                .text()
                .await
                .map_err(|err| (StatusCode::BAD_REQUEST, err.to_string()))?;

            let trimmed = raw.as_str();
            // log_field(&field_name, &trimmed);
            match trimmed {
                "on" => {
                    data.insert(field_name.clone(), "true".to_string());
                }
                "off" => {
                    data.insert(field_name.clone(), "false".to_string());
                }
                _ => {
                    data.insert(field_name.into(), trimmed.to_string());
                }
            };
        }
    }

    // Values go into a query-string for serde_qs to parse. Reserved chars in
    // the value (`&`, `=`, `+`, `%`, space) MUST be escaped or they'll be
    // mis-interpreted as syntax. Use form-urlencoding — the SAME standard
    // serde_qs's decoder (`form_urlencoded::parse`) expects, so encode and
    // decode are the matched halves of one scheme. Encode VALUES ONLY; keys
    // keep their literal brackets — serde_qs needs `upsert[data][0][summary]`
    // unescaped to rebuild the nesting.
    let query_string = data
        .iter()
        .map(|(k, v)| {
            let ev: String = url::form_urlencoded::byte_serialize(v.as_bytes()).collect();
            format!("{k}={ev}")
        })
        .collect::<Vec<_>>()
        .join("&");

    let qs = serde_qs::Config::new(20, true);

    // 2. USE THE CORRECT PUBLIC EXPORT: `serde_qs::Deserializer`
    // with_config takes the Config by value, and the bytes of the query string.
    let deserializer = serde_qs::Deserializer::with_config(&qs, query_string.as_bytes())
        .map_err(|err| {
            (
                StatusCode::BAD_REQUEST,
                format!("Invalid query string format: {}", err),
            )
        })?;

    // 3. Pass the deserializer directly to serde_path_to_error
    match serde_path_to_error::deserialize::<_, T>(deserializer) {
        Ok(parsed) => Ok(parsed),
        Err(err) => {
            let path = err.path().to_string();
            let inner_err = err.into_inner();

            tracing::error!("Deserialization failed at path '{}': {}", path, inner_err);

            Err((
                StatusCode::BAD_REQUEST,
                format!("Form parsing failed at field '{}': {}", path, inner_err),
            ))
        }
    }
}

pub async fn download_form_file(
    mut field: Field<'_>,
    dir: impl AsRef<Path>,
) -> Result<PathBuf, (StatusCode, String)> {
    use tokio::io::AsyncWriteExt;
    let filename = field.file_name().unwrap();
    let path = dir.as_ref().join(filename);
    let mut file = match tokio::fs::File::create(&path).await {
        Ok(f) => f,
        Err(err) => {
            tracing::error!("{err}");
            return Err((StatusCode::INTERNAL_SERVER_ERROR, err.to_string()));
        }
    };
    while let Ok(Some(bytes)) = field.chunk().await {
        if file.write_all(&bytes).await.is_err() {
            break;
        }
    }
    Ok(path)
}