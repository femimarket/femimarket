use crate::assignments::Assignment;
use crate::rota::Rota;
use crate::shifts::ShiftTimeRuleClient;
use crate::staffs::Staff;
use crate::{column, key_cell, retime_shift, run_database_query, shift_assign, split_payload};
use chrono::Timelike;
use mysql_async::params;
use mysql_async::prelude::*;

pub async fn publish_rota(database_url: &str, rota_id: i32, region: &str,
    rockstar_version: &str, cookie: &str, go: bool)
    -> Result<(), Box<dyn std::error::Error>> {
    let pool = mysql_async::Pool::new(database_url);
    let mut conn = pool.get_conn().await?;
    let rota: Rota = conn
        .exec_first(include_str!("rota.sql"), params! { "id" => rota_id })
        .await?
        .ok_or_else(|| format!("rota {rota_id} does not exist"))?;
    let assignments: Vec<Assignment> = conn
        .exec(include_str!("assignments.sql"), params! { "rota_id" => rota_id })
        .await?;
    assignments.first().ok_or("rota has no care assignments to publish")?;
    let shifts: Vec<ShiftTimeRuleClient> = conn
        .exec(include_str!("shifts.sql"), params! { "from_date" => rota.from_date, "to_date" => rota.to_date })
        .await?;
    let staffs: Vec<Staff> = conn.query(include_str!("staffs.sql")).await?;
    drop(conn);
    pool.disconnect().await?;

    let mut covered: Vec<&ShiftTimeRuleClient> = shifts.iter()
        .filter(|s| assignments.iter().any(|a| a.shift_id == s.id))
        .collect();
    let problems: Vec<String> = covered.iter()
        .flat_map(|s| {
            let shift_problem = match s.roundsys_pk.as_deref().map(str::parse::<i64>) {
                Some(Ok(_)) => None,
                _ => Some(format!(
                    "shift {} ({}) has no usable roundsys_pk", s.id, s.client_name)),
            };
            let staff_problems = assignments.iter()
                .filter(|a| a.shift_id == s.id)
                .filter_map(|a| match staffs.iter().find(|st| st.id == a.staff_id) {
                    None => Some(format!("staff {} not found", a.staff_id)),
                    Some(staff) => match staff.roundsys_pk.as_deref().map(str::parse::<i64>) {
                        Some(Ok(_)) => None,
                        _ => Some(format!("{} has no usable roundsys_pk", staff.name)),
                    },
                });
            shift_problem.into_iter().chain(staff_problems).collect::<Vec<_>>()
        })
        .collect();
    match problems.is_empty() {
        true => {}
        false => return Err(problems.join("; ").into()),
    }
    covered.sort_by_key(|s| (s.on_date, assignments.iter()
        .filter(|a| a.shift_id == s.id)
        .map(|a| a.start_time)
        .min()));

    for s in &covered {
        let client = &s.client_name;
        let roundsys_pk: i64 = s.roundsys_pk.as_deref().expect("preflighted")
            .parse().expect("preflighted");
        let mut carers: Vec<&Staff> = assignments.iter()
            .filter(|a| a.shift_id == s.id)
            .map(|a| staffs.iter().find(|st| st.id == a.staff_id).expect("preflighted"))
            .collect();
        carers.sort_by(|a, b| a.name.cmp(&b.name));
        let start_minute = assignments.iter()
            .filter(|a| a.shift_id == s.id)
            .map(|a| a.start_time.hour() as i64 * 60 + a.start_time.minute() as i64)
            .min()
            .expect("covered shift has assignments");
        let start_time = format!("{:02}:{:02}", start_minute / 60, start_minute % 60);
        let end_minute = start_minute + s.duration_mins();
        let end_time = format!("{:02}:{:02}", end_minute / 60, end_minute % 60);
        let on_date = s.on_date.to_string();

        let head_payload = run_database_query(region, rockstar_version, cookie,
            "manager__rota_v3__get_shift_details",
            serde_json::json!({"shift_pk": roundsys_pk})).await?;
        let (head_headers, head_rows) = split_payload(&head_payload)?;
        let head = head_rows.first().and_then(|r| r.as_array())
            .ok_or_else(|| format!("Roundsys has no shift {roundsys_pk}"))?;
        let head_carer: Option<i64> = match key_cell(head, column(&head_headers, "app__user_fk")?, 0) {
            Ok(pk) => Some(pk.parse()?),
            Err(_) => None,
        };
        let head_shift_type_fk: i64 =
            key_cell(head, column(&head_headers, "shift_type_fk")?, 0)?.parse()?;
        let current_start: String = match head[column(&head_headers, "shift_start_time")?].as_str() {
            Some(time) => time.chars().take(5).collect(),
            None => String::new(),
        };

        let double_up_payload = run_database_query(region, rockstar_version, cookie,
            "rotav3__get_double_up_shifts",
            serde_json::json!({"shift_fk": roundsys_pk})).await?;
        let (double_up_headers, double_up_rows) = match double_up_payload.as_array() {
            Some(rows) if rows.len() > 1 => split_payload(&double_up_payload)?,
            _ => (Vec::new(), &[] as &[serde_json::Value]),
        };
        let double_up_shift_pks: Vec<i64> = match double_up_rows.is_empty() {
            true => Vec::new(),
            false => {
                let pk_idx = column(&double_up_headers, "pk")?;
                double_up_rows.iter().enumerate()
                    .map(|(line, row)| -> Result<i64, Box<dyn std::error::Error>> {
                        let row = row.as_array().ok_or("double-up row is not an array")?;
                        Ok(key_cell(row, pk_idx, line)?.parse()?)
                    })
                    .collect::<Result<Vec<_>, _>>()?
            }
        };
        let shift_pks: Vec<i64> = std::iter::once(roundsys_pk)
            .chain(double_up_shift_pks.iter().copied().filter(|pk| *pk != roundsys_pk))
            .collect();

        let retime_needed = current_start != start_time;
        println!(
            "{} {:<24} {} -> {}-{}{}",
            on_date, client, current_start, start_time, end_time,
            match retime_needed { true => "  RETIME", false => "" },
        );
        carers.iter().zip(&shift_pks).for_each(|(c, pk)| {
            println!("        {}  (row {pk})", c.name);
        });
        match carers.len() > shift_pks.len() {
            true => return Err(format!(
                "{} has {} carers but Roundsys only has {} rows",
                client, carers.len(), shift_pks.len()).into()),
            false => {}
        }

        match go {
            false => {}
            true => {
                match retime_needed {
                    false => {}
                    true => {
                        retime_shift(region, rockstar_version, cookie, roundsys_pk,
                            head_carer, head_shift_type_fk,
                            &start_time, &end_time, &on_date).await?;
                        match double_up_rows.is_empty() {
                            true => {}
                            false => {
                                let pk_idx = column(&double_up_headers, "pk")?;
                                let carer_idx = column(&double_up_headers, "app__user_fk")?;
                                let type_idx = column(&double_up_headers, "shift_type_fk")?;
                                for (line, row) in double_up_rows.iter().enumerate() {
                                    let row = row.as_array()
                                        .ok_or("double-up row is not an array")?;
                                    let pk: i64 = key_cell(row, pk_idx, line)?.parse()?;
                                    let carer: Option<i64> =
                                        match key_cell(row, carer_idx, line) {
                                            Ok(previous) => Some(previous.parse()?),
                                            Err(_) => None,
                                        };
                                    let shift_type_fk: i64 =
                                        key_cell(row, type_idx, line)?.parse()?;
                                    match pk == roundsys_pk {
                                        true => {}
                                        false => retime_shift(region, rockstar_version,
                                            cookie, pk, carer, shift_type_fk,
                                            &start_time, &end_time, &on_date).await?,
                                    }
                                }
                            }
                        }
                    }
                }
                for (carer, shift_pk) in carers.iter().zip(&shift_pks) {
                    let detail_payload = run_database_query(region, rockstar_version, cookie,
                        "manager__rota_v3__get_shift_details",
                        serde_json::json!({"shift_pk": shift_pk})).await?;
                    let (detail_headers, detail_rows) = split_payload(&detail_payload)?;
                    let detail = detail_rows.first().and_then(|r| r.as_array())
                        .ok_or_else(|| format!("Roundsys has no shift {shift_pk}"))?;
                    let shift_type_fk: i64 =
                        key_cell(detail, column(&detail_headers, "shift_type_fk")?, 0)?.parse()?;
                    let carer_pk: i64 = carer.roundsys_pk.as_deref().expect("preflighted")
                        .parse().expect("preflighted");
                    match shift_assign(region, rockstar_version, cookie, *shift_pk,
                        carer_pk, shift_type_fk, -1, None).await? {
                        true => println!("        assigned {}", carer.name),
                        false => return Err(format!(
                            "assign of {} to row {shift_pk} failed", carer.name).into()),
                    }
                }
            }
        }
    }
    match go {
        true => println!("--> {} shifts published to Roundsys", covered.len()),
        false => println!(
            "dry run - nothing sent. Add --go to apply, in order, stopping at the first failure."),
    }
    Ok(())
}
