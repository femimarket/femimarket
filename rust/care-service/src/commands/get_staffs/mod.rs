use crate::staffs::Staff;
use crate::{column, key_cell, run_database_query, split_payload};
use mysql_async::params;
use mysql_async::prelude::*;
use std::collections::HashSet;

pub async fn get_staffs(
    database_url: &str,
    region: &str,
    rockstar_version: &str,
    cookie: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("Fetching manager__staff__list from {region} (version {rockstar_version})...");
    let payload = run_database_query(
        region,
        rockstar_version,
        cookie,
        "manager__staff__list",
        serde_json::json!({"search_string": "-1", "status": "No"}),
    )
    .await?;
    let (headers, rows) = split_payload(&payload)?;
    let pk_idx = column(&headers, "pk")?;
    let name_idx = column(&headers, "Name")?;
    let role_idx = column(&headers, "Role")?;
    let postcode_idx = column(&headers, "Postcode")?;

    let pool = mysql_async::Pool::new(database_url);
    let mut conn = pool.get_conn().await?;
    let mut seen: HashSet<String> = HashSet::new();
    let mut updated = 0usize;
    let mut inserted: Vec<String> = Vec::new();
    let mut skipped: Vec<String> = Vec::new();

    for (line, row) in rows.iter().enumerate() {
        let row = row
            .as_array()
            .ok_or_else(|| format!("Row {line} is not an array"))?;
        let pk = key_cell(row, pk_idx, line)?;
        let text = |idx: usize, what: &str| -> Result<String, String> {
            match row.get(idx) {
                Some(serde_json::Value::String(s)) => Ok(s.clone()),
                other => Err(format!("Row {line} (pk {pk}): {what} is {other:?}, not a string")),
            }
        };
        let name = text(name_idx, "Name")?;
        if name.trim().is_empty() {
            return Err(format!("Row {line} (pk {pk}): Name is empty").into());
        }
        let role = text(role_idx, "Role")?;
        let postcode = text(postcode_idx, "Postcode")?;
        seen.insert(pk.clone());

        let existing: Option<Staff> = conn
            .exec_first(include_str!("staff.sql"), params! { "roundsys_pk" => &pk })
            .await?;
        match existing {
            Some(existing) => {
                let postcode_id = super::resolve_postcode(&mut conn, &postcode, &name).await?;
                conn.exec_drop(
                    include_str!("update.sql"),
                    params! { "name" => name, "postcode_id" => postcode_id, "id" => existing.id },
                )
                .await?;
                updated += 1;
            }
            None => {
                if !role.trim().eq_ignore_ascii_case("careworker") {
                    skipped.push(format!("{name} ({role})"));
                    continue;
                }
                let postcode_id = super::resolve_postcode(&mut conn, &postcode, &name).await?;
                conn.exec_drop(
                    include_str!("insert.sql"),
                    params! { "name" => &name, "postcode_id" => postcode_id, "roundsys_pk" => pk },
                )
                .await?;
                inserted.push(name);
            }
        }
    }

    let gone: Vec<String> = conn
        .query::<Staff, _>(include_str!("staffs.sql"))
        .await?
        .into_iter()
        .filter(|s| {
            s.roundsys_pk
                .as_ref()
                .is_some_and(|pk| !seen.contains(pk))
        })
        .map(|s| s.name)
        .collect();
    drop(conn);
    pool.disconnect().await?;

    println!(
        "{} staff from Roundsys | {updated} updated (name/postcode only)",
        seen.len()
    );
    if !inserted.is_empty() {
        println!(
            "NEW ({}): {} - transport untold; a human must set it (and gender) \
             before they can be rostered",
            inserted.len(),
            inserted.join(", ")
        );
    }
    if !skipped.is_empty() {
        println!(
            "NOT INSERTED, non-careworker ({}): {}",
            skipped.len(),
            skipped.join(", ")
        );
    }
    if !gone.is_empty() {
        println!("NO LONGER IN ROUNDSYS ({}): {}", gone.len(), gone.join(", "));
    }
    Ok(())
}
