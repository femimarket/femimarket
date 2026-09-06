use crate::clients::Client;
use crate::{column, key_cell, run_database_query, split_payload};
use mysql_async::params;
use mysql_async::prelude::*;
use std::collections::{HashMap, HashSet};

pub async fn get_clients(
    database_url: &str,
    region: &str,
    rockstar_version: &str,
    cookie: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("Fetching manager__resident__list from {region} (version {rockstar_version})...");
    let payload = run_database_query(
        region,
        rockstar_version,
        cookie,
        "manager__resident__list",
        serde_json::json!({"search_string": "-1", "status": "No"}),
    )
    .await?;
    let (headers, rows) = split_payload(&payload)?;
    let pk_idx = column(&headers, "pk")?;
    let name_idx = column(&headers, "Name")?;

    println!("Fetching user__resident_list (locations)...");
    let enrich = run_database_query(
        region,
        rockstar_version,
        cookie,
        "user__resident_list",
        serde_json::json!([]),
    )
    .await?;
    let (eheaders, erows) = split_payload(&enrich)?;
    let epk_idx = column(&eheaders, "pk")?;
    let epostcode_idx = column(&eheaders, "postcode")?;

    let mut postcodes: HashMap<String, String> = HashMap::new();
    for (line, row) in erows.iter().enumerate() {
        let row = row
            .as_array()
            .ok_or_else(|| format!("user__resident_list row {line} is not an array"))?;
        let pk = key_cell(row, epk_idx, line)?;
        let postcode = match row.get(epostcode_idx) {
            Some(serde_json::Value::String(s)) => s.clone(),
            Some(serde_json::Value::Null) | None => String::new(),
            other => {
                return Err(format!(
                    "user__resident_list row {line} (pk {pk}): postcode is {other:?}, not a string"
                )
                .into());
            }
        };
        postcodes.insert(pk, postcode);
    }
    println!("    {} residents available to join on", postcodes.len());

    let pool = mysql_async::Pool::new(database_url);
    let mut conn = pool.get_conn().await?;
    let mut seen: HashSet<String> = HashSet::new();
    let mut updated = 0usize;
    let mut inserted: Vec<String> = Vec::new();

    for (line, row) in rows.iter().enumerate() {
        let row = row
            .as_array()
            .ok_or_else(|| format!("Row {line} is not an array"))?;
        let pk = key_cell(row, pk_idx, line)?;
        let name = match row.get(name_idx) {
            Some(serde_json::Value::String(s)) if !s.trim().is_empty() => s.clone(),
            other => {
                return Err(format!("Row {line} (pk {pk}): Name is {other:?}, not a name").into());
            }
        };
        seen.insert(pk.clone());

        let existing: Option<Client> = conn
            .exec_first(include_str!("client.sql"), params! { "roundsys_pk" => &pk })
            .await?;
        match existing {
            Some(existing) => {
                let postcode = postcodes
                    .get(&pk)
                    .ok_or_else(|| format!("{name} (pk {pk}) has no row in user__resident_list"))?;
                let postcode_id = super::resolve_postcode(&mut conn, postcode, &name).await?;
                conn.exec_drop(
                    include_str!("update.sql"),
                    params! { "name" => name, "postcode_id" => postcode_id, "id" => existing.id },
                )
                .await?;
                updated += 1;
            }
            None => {
                let postcode = postcodes
                    .get(&pk)
                    .ok_or_else(|| format!("{name} (pk {pk}) has no row in user__resident_list"))?;
                let postcode_id = super::resolve_postcode(&mut conn, postcode, &name).await?;
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
        .query::<Client, _>(include_str!("clients.sql"))
        .await?
        .into_iter()
        .filter(|c| {
            c.roundsys_pk
                .as_ref()
                .is_some_and(|pk| !seen.contains(pk))
        })
        .map(|c| c.name)
        .collect();
    drop(conn);
    pool.disconnect().await?;

    println!(
        "{} clients from Roundsys | {updated} updated (name/postcode only)",
        seen.len()
    );
    if !inserted.is_empty() {
        println!("NEW ({}): {}", inserted.len(), inserted.join(", "));
    }
    if !gone.is_empty() {
        println!("NO LONGER IN ROUNDSYS ({}): {}", gone.len(), gone.join(", "));
    }
    Ok(())
}
