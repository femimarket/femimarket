//! care-service: the rota database and its solver, served and scripted.

pub mod activities;
pub mod activity_links;
pub mod assignment_types;
pub mod assignments;
pub mod availabilities;
pub mod blacklisted_workers;
pub mod blacklists;
pub mod break_rules;
pub mod bands;
pub mod banned_double_ups;
pub mod back_to_back_rules;
pub mod carer_rules;
pub mod cares;
pub mod dbs;
pub mod dbs_update;
pub mod new_hire_docs;
pub mod clients;
pub mod critical_visit_rules;
pub mod daily_call_rules;
pub mod double_ups;
pub mod early_leave_rules;
pub mod max_daily_hours_rules;
pub mod passenger_rules;
pub mod experience;
pub mod experience_cares;
pub mod preferences;
pub mod prospects;
pub mod weekly_hours;
pub mod preferred_workers;
pub mod public_postcodes;
pub mod packages;
pub mod rota;
pub mod shifts;
pub mod skills;
pub mod staffs;
pub mod supervisions;
pub mod supervisors;
pub mod transport_modes;
pub mod users;
pub mod weight_min_same_client_gap_mins;
pub mod weight_penalty_non_preferred_worker;
pub mod weight_penalty_partial_double_up;
pub mod weight_penalty_per_minute_early_leave;
pub mod weight_penalty_per_minute_late;
pub mod weight_penalty_per_minute_permissioned_stretch;
pub mod weight_penalty_per_minute_start_moved;
pub mod weight_penalty_per_minute_travel;
pub mod weight_penalty_per_taxi_journey;
pub mod weight_penalty_uncovered_high_priority_multiplier;
pub mod weight_penalty_uncovered_visit;
pub mod weight_solver_max_seconds;
pub mod weight_solver_workers;
pub mod whitelisted_workers;
pub mod whitelists;
pub mod time_rules;
pub mod commands;
pub mod server;

pub async fn sync_travel_times(
    server: &str,
    from_date: &str,
    to_date: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(90 * 60))
        .build()?;
    let response = client
        .post(format!("{server}/travel-times/sync"))
        .body(format!("{from_date},{to_date}"))
        .send()
        .await?;
    let status = response.status();
    let text = response.text().await?;
    if !status.is_success() {
        return Err(format!("{status}: {text}").into());
    }
    println!("{text}");
    Ok(())
}


pub fn split_payload(
    payload: &serde_json::Value,
) -> Result<(Vec<&str>, &[serde_json::Value]), Box<dyn std::error::Error>> {
    let payload = payload
        .as_array()
        .ok_or("Unexpected response shape - expected [headers, ...rows]")?;
    let (headers, rows) = payload.split_first().ok_or("No data received")?;
    let headers = headers
        .as_array()
        .ok_or("Unexpected response shape - first element is not a header row")?
        .iter()
        .map(|h| h.as_str().unwrap_or_default())
        .collect();
    Ok((headers, rows))
}

pub fn column(headers: &[&str], name: &str) -> Result<usize, Box<dyn std::error::Error>> {
    headers
        .iter()
        .position(|h| *h == name)
        .ok_or_else(|| format!("Response is missing expected column: {name}").into())
}

pub fn key_cell(row: &[serde_json::Value], index: usize, line: usize) -> Result<String, String> {
    match row.get(index) {
        Some(serde_json::Value::Number(n)) => Ok(n.to_string()),
        Some(serde_json::Value::String(s)) if !s.trim().is_empty() => Ok(s.trim().to_string()),
        other => Err(format!("Row {line}: pk is {other:?}, not a key")),
    }
}

pub async fn run_database_query(
    region: &str,
    rockstar_version: &str,
    cookie: &str,
    procedure: &str,
    arguments: serde_json::Value,
) -> Result<serde_json::Value, Box<dyn std::error::Error>> {
    let body = serde_urlencoded::to_string([
        ("procedure_name", procedure),
        ("procedure_arguments", &arguments.to_string()),
        ("output_format", "list"),
        ("include_all_resultsets", "false"),
        ("rockstar_version_number", rockstar_version),
    ])?;

    let response = reqwest::Client::new()
        .post(format!("https://{region}/public/rockstar/run_database_query"))
        .header("Accept", "*/*")
        .header("Accept-Language", "en-GB,en;q=0.6")
        .header("Content-Type", "text/plain;charset=UTF-8")
        .header("Origin", format!("https://{region}"))
        .header(
            "Referer",
            format!("https://{region}/private/roundsys/manager/index.html?region={region}"),
        )
        .header("Cookie", cookie)
        .body(body)
        .send()
        .await?;

    let status = response.status();
    let text = response.text().await?;
    if !status.is_success() {
        return Err(format!(
            "Roundsys returned HTTP {status}: {}",
            &text[..text.len().min(200)]
        )
        .into());
    }
    serde_json::from_str(&text).map_err(|_| {
        "Response was not JSON - the session cookie has almost certainly expired. \
         Log in again and re-export it."
            .into()
    })
}

#[allow(non_snake_case)]
pub async fn shift_assign(
    region: &str,
    rockstar_version: &str,
    cookie: &str,
    shift_fk: i64,
    app__user_fk: i64,
    shift_type_fk: i64,
    actual_carer_fk: i64,
    prompt: Option<&str>,
) -> Result<bool, Box<dyn std::error::Error>> {
    let mut arguments = serde_json::json!({
        "shift_fk": shift_fk,
        "app__user_fk": app__user_fk,
        "shift_type_fk": shift_type_fk,
        "action": "Assign shift",
        "actual_carer_fk": actual_carer_fk,
    });
    match prompt {
        Some(p) => arguments["prompt"] = serde_json::json!(p),
        None => {}
    }
    let response = run_database_query(region, rockstar_version, cookie,
        "rotav3__shift_assign", arguments).await?;
    Ok(response[1][0] == "ok")
}

pub async fn shift_unassign(
    region: &str,
    rockstar_version: &str,
    cookie: &str,
    shift_fk: i64,
) -> Result<bool, Box<dyn std::error::Error>> {
    let response = run_database_query(region, rockstar_version, cookie, "rotav3__shift_unassign",
        serde_json::json!({
            "shift_fk": shift_fk,
            "prompt": "Change shift time",
            "action": "Un-assign shift (Change shift time)",
        })).await?;
    Ok(response[1][0] == "ok")
}

pub async fn change_shift_time(
    region: &str,
    rockstar_version: &str,
    cookie: &str,
    shift_fk: i64,
    start_time: &str,
    end_time: &str,
    shift_date: &str,
) -> Result<bool, Box<dyn std::error::Error>> {
    let response = run_database_query(region, rockstar_version, cookie, "rotav3__change_shift_time",
        serde_json::json!({
            "shift_fk": shift_fk,
            "new_slot_start_time": start_time,
            "new_slot_end_time": end_time,
            "new_shift_date": shift_date,
            "prompt": "Change shift time",
            "action": "Change shift time",
        })).await?;
    Ok(response[1][0] == "ok")
}

/// Roundsys refuses to retime an assigned shift; the manager UI unassigns,
/// changes the time, then puts the carer back, so this does the same - and
/// restores the carer even when the retime fails.
#[allow(non_snake_case)]
pub async fn retime_shift(
    region: &str,
    rockstar_version: &str,
    cookie: &str,
    shift_fk: i64,
    app__user_fk: Option<i64>,
    shift_type_fk: i64,
    start_time: &str,
    end_time: &str,
    shift_date: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    match app__user_fk {
        None => {}
        Some(_) => match shift_unassign(region, rockstar_version, cookie, shift_fk).await? {
            true => {}
            false => return Err(format!("unassign failed on Roundsys shift {shift_fk}").into()),
        },
    }
    let retimed = change_shift_time(region, rockstar_version, cookie, shift_fk,
        start_time, end_time, shift_date).await?;
    match app__user_fk {
        None => {}
        Some(previous) => {
            match shift_assign(region, rockstar_version, cookie, shift_fk,
                previous, shift_type_fk, previous, Some("Change shift time")).await? {
                true => {}
                false => return Err(format!(
                    "carer restore failed on Roundsys shift {shift_fk}").into()),
            }
        }
    }
    match retimed {
        true => Ok(()),
        false => Err(format!("retime failed on Roundsys shift {shift_fk}").into()),
    }
}
