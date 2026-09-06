use crate::postcodes;
use crate::travel_times;
use chrono::{FixedOffset, NaiveDate, NaiveTime, TimeZone, Utc};
use motis_service::apis::configuration::Configuration;
use motis_service::apis::routing_api;
use motis_service::models::Mode;
use sea_orm::{
    ActiveValue::Set, ColumnTrait, Database, DatabaseConnection, EntityTrait, PaginatorTrait,
    QueryFilter,
};
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use tokio::sync::Semaphore;
use tokio::task::JoinSet;

pub(crate) const MAX_TRIP_SECONDS: i64 = 90 * 60;
pub(crate) const ACCESS_MAX_SECONDS: i32 = 30 * 60;
const FIRST_DEPARTURE_MINUTES: u32 = 5 * 60;
const LAST_DEPARTURE_MINUTES: u32 = 23 * 60;
const STEP_MINUTES: u32 = 15;
const TOTAL_SLOTS: usize = 73;
const WALK_SPEED_METERS_PER_SECOND: f64 = 1.0;
const BIKE_SPEED_METERS_PER_SECOND: f64 = 12.0 / 3.6;
const CONCURRENT_REQUESTS: usize = 24;

struct Place {
    id: String,
    coord: String,
}

fn minutes(seconds: i64) -> i32 {
    ((seconds + 30) / 60) as i32
}

async fn insert_rows(
    db: &DatabaseConnection,
    rows: Vec<travel_times::ActiveModel>,
) -> Result<(), String> {
    for chunk in rows.chunks(5000) {
        travel_times::Entity::insert_many(chunk.to_vec())
            .exec(db)
            .await
            .map_err(|e| e.to_string())?;
    }
    Ok(())
}

pub async fn compute_travel_times(
    database_url: &str,
    from_date: &str,
    to_date: &str,
    motis_url: &str,
    from_postcodes: &[String],
    to_postcodes: &[String],
) -> Result<String, String> {
    let from_date = NaiveDate::parse_from_str(from_date, "%Y-%m-%d").map_err(|e| e.to_string())?;
    let to_date = NaiveDate::parse_from_str(to_date, "%Y-%m-%d").map_err(|e| e.to_string())?;
    let db = Database::connect(database_url).await.map_err(|e| e.to_string())?;

    let mut wanted: HashSet<String> = HashSet::new();
    wanted.extend(from_postcodes.iter().cloned());
    wanted.extend(to_postcodes.iter().cloned());
    let postcode_rows = postcodes::Entity::find()
        .filter(postcodes::Column::Id.is_in(wanted.iter().cloned()))
        .all(&db)
        .await
        .map_err(|e| e.to_string())?;
    let coords: HashMap<String, (f64, f64)> = postcode_rows
        .into_iter()
        .map(|p| (p.id, (p.latitude, p.longitude)))
        .collect();
    let mut missing: Vec<String> = wanted
        .iter()
        .filter(|p| !coords.contains_key(*p))
        .cloned()
        .collect();
    missing.sort();
    if !missing.is_empty() {
        return Err(format!(
            "postcodes not found in postcodes: {} - load ONSPD or fix the input",
            missing.join(", ")
        ));
    }
    let place = |id: &String| {
        let (lat, lon) = coords[id];
        Place { id: id.clone(), coord: format!("{lat},{lon}") }
    };
    let dedup = |ids: &[String]| -> Vec<Place> {
        let mut seen = HashSet::new();
        ids.iter().filter(|p| seen.insert((*p).clone())).map(&place).collect()
    };
    let origins: Arc<Vec<Place>> = Arc::new(dedup(from_postcodes));
    let sites: Arc<Vec<Place>> = Arc::new(dedup(to_postcodes));
    println!("Rota span: {from_date} -> {to_date}");
    println!("{} from-postcodes x {} to-postcodes", origins.len(), sites.len());

    let configuration = Arc::new({
        let mut c = Configuration::new();
        c.base_path = motis_url.trim_end_matches('/').to_string();
        c
    });

    for (mode_id, mode, pedestrian_speed, cycling_speed) in [
        ("car", Mode::Car, None, None),
        ("bicycle", Mode::Bike, None, Some(BIKE_SPEED_METERS_PER_SECOND)),
        ("walk", Mode::Walk, Some(WALK_SPEED_METERS_PER_SECOND), None),
    ] {
        let have = travel_times::Entity::find()
            .filter(travel_times::Column::TransportModeId.eq(mode_id))
            .count(&db)
            .await
            .map_err(|e| e.to_string())?;
        if have > 0 {
            println!("{mode_id} already present ({have} rows), skipping");
            continue;
        }
        println!("Computing {mode_id} via motis...");
        let semaphore = Arc::new(Semaphore::new(CONCURRENT_REQUESTS));
        let mut tasks: JoinSet<Result<Option<(usize, i64)>, String>> = JoinSet::new();
        for pair in 0..origins.len() * sites.len() {
            let origins = origins.clone();
            let sites = sites.clone();
            let configuration = configuration.clone();
            let semaphore = semaphore.clone();
            tasks.spawn(async move {
                let origin = &origins[pair / sites.len()];
                let site = &sites[pair % sites.len()];
                if origin.id == site.id {
                    return Ok(None);
                }
                let _permit = semaphore.acquire_owned().await.map_err(|e| e.to_string())?;
                let response = routing_api::plan(
                    &configuration,
                    &origin.coord,
                    &site.coord,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    pedestrian_speed,
                    cycling_speed,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    Some(false),
                    None,
                    None,
                    Some(vec![mode]),
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    Some(MAX_TRIP_SECONDS as i32),
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                    None,
                )
                    .await
                    .map_err(|e| e.to_string())?;
                let best = response.direct.iter().map(|i| i.duration as i64).min();
                Ok(best.map(|secs| (pair, secs)))
            });
        }
        let mut rows: Vec<travel_times::ActiveModel> = Vec::new();
        while let Some(joined) = tasks.join_next().await {
            let Some((pair, secs)) = joined.map_err(|e| e.to_string())?? else {
                continue;
            };
            if secs <= 0 || secs > MAX_TRIP_SECONDS {
                continue;
            }
            rows.push(travel_times::ActiveModel {
                from_postcode_id: Set(origins[pair / sites.len()].id.clone()),
                to_postcode_id: Set(sites[pair % sites.len()].id.clone()),
                transport_mode_id: Set(mode_id.to_string()),
                travel_mins: Set(minutes(secs)),
                departure_time: Set(None),
                ..Default::default()
            });
        }
        let count = rows.len();
        insert_rows(&db, rows).await?;
        println!("   {count} rows");
    }

    let mut day = from_date;
    while day <= to_date {
        let day_start = Utc.from_utc_datetime(&day.and_time(NaiveTime::MIN));
        let day_end = Utc.from_utc_datetime(
            &day.succ_opt().ok_or("date overflow")?.and_time(NaiveTime::MIN),
        );
        let present: HashSet<i64> = travel_times::Entity::find()
            .filter(travel_times::Column::TransportModeId.eq("transit"))
            .filter(travel_times::Column::DepartureTime.gte(day_start))
            .filter(travel_times::Column::DepartureTime.lt(day_end))
            .all(&db)
            .await
            .map_err(|e| e.to_string())?
            .into_iter()
            .filter_map(|r| r.departure_time.map(|t| t.timestamp()))
            .collect();
        if present.len() == TOTAL_SLOTS {
            println!("{day} transit complete ({TOTAL_SLOTS} departures), skipping");
            day = day.succ_opt().ok_or("date overflow")?;
            continue;
        }

        let mut slot_minutes = FIRST_DEPARTURE_MINUTES;
        let mut slot_number = 0usize;
        while slot_minutes <= LAST_DEPARTURE_MINUTES {
            slot_number += 1;
            let departure_utc = Utc.from_utc_datetime(
                &day.and_time(
                    NaiveTime::from_num_seconds_from_midnight_opt(slot_minutes * 60, 0)
                        .ok_or("bad slot time")?,
                ),
            );
            if present.contains(&departure_utc.timestamp()) {
                slot_minutes += STEP_MINUTES;
                continue;
            }
            let departure_fixed = departure_utc
                .with_timezone(&FixedOffset::east_opt(0).ok_or("bad offset")?);
            let semaphore = Arc::new(Semaphore::new(CONCURRENT_REQUESTS));
            let mut tasks: JoinSet<Result<Option<(usize, i64)>, String>> = JoinSet::new();
            for pair in 0..origins.len() * sites.len() {
                let origins = origins.clone();
                let sites = sites.clone();
                let configuration = configuration.clone();
                let semaphore = semaphore.clone();
                tasks.spawn(async move {
                    let origin = &origins[pair / sites.len()];
                    let site = &sites[pair % sites.len()];
                    if origin.id == site.id {
                        return Ok(None);
                    }
                    let _permit = semaphore.acquire_owned().await.map_err(|e| e.to_string())?;
                    let response = routing_api::plan(
                        &configuration,
                        &origin.coord,
                        &site.coord,
                        None,
                        None,
                        None,
                        Some(departure_fixed),
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        Some(WALK_SPEED_METERS_PER_SECOND),
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        Some(false),
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        Some(ACCESS_MAX_SECONDS),
                        Some(ACCESS_MAX_SECONDS),
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                        None,
                    )
                        .await
                        .map_err(|e| e.to_string())?;
                    let best = response
                        .itineraries
                        .iter()
                        .map(|i| (i.end_time.with_timezone(&Utc) - departure_utc).num_seconds())
                        .min();
                    Ok(best.map(|secs| (pair, secs)))
                });
            }
            let mut rows: Vec<travel_times::ActiveModel> = Vec::new();
            while let Some(joined) = tasks.join_next().await {
                let Some((pair, secs)) = joined.map_err(|e| e.to_string())?? else {
                    continue;
                };
                if secs <= 0 || secs > MAX_TRIP_SECONDS {
                    continue;
                }
                rows.push(travel_times::ActiveModel {
                    from_postcode_id: Set(origins[pair / sites.len()].id.clone()),
                    to_postcode_id: Set(sites[pair % sites.len()].id.clone()),
                    transport_mode_id: Set("transit".to_string()),
                    travel_mins: Set(minutes(secs)),
                    departure_time: Set(Some(departure_utc)),
                    ..Default::default()
                });
            }
            let count = rows.len();
            insert_rows(&db, rows).await?;
            if slot_number % 10 == 0 || slot_minutes == LAST_DEPARTURE_MINUTES {
                use std::io::Write;
                println!("   {day}: {slot_number}/{TOTAL_SLOTS} transit slots done ({count} rows this slot)");
                let _ = std::io::stdout().flush();
            }
            slot_minutes += STEP_MINUTES;
        }
        day = day.succ_opt().ok_or("date overflow")?;
    }

    let total = travel_times::Entity::find().count(&db).await.map_err(|e| e.to_string())?;
    Ok(format!(
        "travel times computed for {from_date} -> {to_date}; table holds {total} rows"
    ))
}
