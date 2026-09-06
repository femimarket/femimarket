use crate::public_postcodes::PublicPostcode;
use mysql_async::params;
use mysql_async::prelude::*;
use std::collections::HashSet;

struct Picks {
    rows: Vec<PublicPostcode>,
    taken: HashSet<String>,
}

pub async fn publish_postcodes(database_url: &str) -> Result<(), Box<dyn std::error::Error>> {
    let pool = mysql_async::Pool::new(database_url);
    let mut conn = pool.get_conn().await?;

    let client_postcodes: HashSet<String> = conn
        .query::<String, _>(include_str!("clients.sql"))
        .await?
        .into_iter()
        .collect();
    let published: Vec<PublicPostcode> = conn.query(include_str!("public_postcodes.sql")).await?;
    let mapped: HashSet<String> = published.iter()
        .map(|m| m.postcode_id.clone())
        .collect();
    let taken: HashSet<String> = published.iter()
        .map(|m| m.public_postcode_id.clone())
        .collect();
    let pending: Vec<&String> = client_postcodes.iter()
        .filter(|p| !mapped.contains(*p))
        .collect();

    let candidates: Vec<String> = pending.iter()
        .flat_map(|p| {
            let prefix = &p[..p.len() - 1];
            let last = p.chars().last().expect("postcode is empty");
            ('A'..='Z')
                .filter(|&letter| letter != last)
                .map(|letter| format!("{prefix}{letter}"))
                .collect::<Vec<_>>()
        })
        .collect();
    let mut real_candidates: HashSet<String> = HashSet::new();
    for chunk in candidates.chunks(500) {
        let rows: Vec<String> = conn
            .exec(
                include_str!("postcodes.sql"),
                params! { "candidates" => serde_json::to_string(chunk)? },
            )
            .await?;
        real_candidates.extend(rows);
    }

    let now = chrono::Utc::now().naive_utc();
    let picks = pending.iter().try_fold(
        Picks { rows: Vec::new(), taken },
        |mut picks, p| {
            let prefix = &p[..p.len() - 1];
            let last = p.chars().last().expect("postcode is empty");
            ('A'..='Z')
                .filter(|&letter| letter != last)
                .map(|letter| format!("{prefix}{letter}"))
                .find(|c| real_candidates.contains(c)
                    && !client_postcodes.contains(c)
                    && !picks.taken.contains(c))
                .map(|public| {
                    picks.taken.insert(public.clone());
                    picks.rows.push(PublicPostcode {
                        postcode_id: (*p).clone(),
                        public_postcode_id: public,
                        note: "masks the client postcode on jobs.femi.market".to_string(),
                        user_id: "@femi:femi.market".to_string(),
                        created_at: now,
                    });
                    picks
                })
                .ok_or_else(|| format!("no valid public postcode exists for {p}"))
        },
    )?;

    let written = picks.rows.len();
    match picks.rows.is_empty() {
        true => {}
        false => {
            conn.exec_batch(
                include_str!("public_postcode.sql"),
                picks.rows.iter().map(|row| params! {
                    "postcode_id" => &row.postcode_id,
                    "public_postcode_id" => &row.public_postcode_id,
                    "note" => &row.note,
                    "user_id" => &row.user_id,
                    "created_at" => row.created_at,
                }),
            )
            .await?;
        }
    }
    drop(conn);
    pool.disconnect().await?;
    println!("{written} postcodes mapped, {} already had masks", mapped.len());
    Ok(())
}
