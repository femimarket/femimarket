use crate::streets::{build_street_graph, Mode, SnapPoint, MAX_TRIP_SECONDS, UNREACHABLE};
use crate::tables::{clients, postcodes, staffs, travel_times};
use crate::transit::{day_journeys, finalize_journeys, load_network, travel_at};
use chrono::{NaiveDate, NaiveTime, TimeZone, Utc};
use sea_orm::{
    ActiveValue::Set, ColumnTrait, Database, DatabaseConnection, EntityTrait, PaginatorTrait,
    QueryFilter,
};
use rayon::prelude::*;
use std::collections::{HashMap, HashSet};
use vulture::RaptorCache;

const ACCESS_MAX_SECONDS: u32 = 30 * 60;
const DEPARTURE_WINDOW_SECONDS: u32 = 600;
const SAMPLE_STEP_SECONDS: u32 = 60;
const FIRST_DEPARTURE_MINUTES: u32 = 5 * 60;
const LAST_DEPARTURE_MINUTES: u32 = 23 * 60;
const STEP_MINUTES: u32 = 15;
const TOTAL_SLOTS: usize = 73;

#[derive(Clone)]
struct Place {
    id: String,
    walk: SnapPoint,
    bike: Option<SnapPoint>,
    car: Option<SnapPoint>,
}

fn minutes(seconds: u32) -> i32 {
    ((seconds + 30) / 60) as i32
}

fn percentile_50(samples: &mut [u32]) -> u32 {
    samples.sort_unstable();
    let index = (samples.len() - 1) * 50 * 2 + 100;
    samples[index / 200]
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

pub async fn sync(
    database_url: &str,
    from_date: &str,
    to_date: &str,
    osm: &str,
    gtfs: &[String],
) -> Result<String, String> {
    let from_date = NaiveDate::parse_from_str(from_date, "%Y-%m-%d").map_err(|e| e.to_string())?;
    let to_date = NaiveDate::parse_from_str(to_date, "%Y-%m-%d").map_err(|e| e.to_string())?;
    let db = Database::connect(database_url).await.map_err(|e| e.to_string())?;

    let staff_rows = staffs::Entity::find().all(&db).await.map_err(|e| e.to_string())?;
    let client_rows = clients::Entity::find().all(&db).await.map_err(|e| e.to_string())?;
    let mut wanted: HashSet<String> = HashSet::new();
    wanted.extend(staff_rows.iter().map(|s| s.postcode_id.clone()));
    wanted.extend(client_rows.iter().map(|c| c.postcode_id.clone()));
    let postcode_rows = postcodes::Entity::find()
        .filter(postcodes::Column::Id.is_in(wanted.iter().cloned()))
        .all(&db)
        .await
        .map_err(|e| e.to_string())?;
    let coords: HashMap<String, (f64, f64)> = postcode_rows
        .into_iter()
        .map(|p| (p.id, (p.latitude, p.longitude)))
        .collect();

    println!("Loading map: {osm}");
    let graph = tokio::task::block_in_place(|| build_street_graph(osm))?;
    println!("--> street graph: {} nodes", graph.node_count());

    let snap_place = |id: &String| -> Result<Place, String> {
        let (lat, lon) = coords
            .get(id)
            .ok_or_else(|| format!("{id}: postcode not found in postcodes"))?;
        let walk = graph
            .snap(*lat, *lon, Mode::Walk)
            .ok_or_else(|| format!("{id} could not be snapped to the street network"))?;
        Ok(Place {
            id: id.clone(),
            walk,
            bike: graph.snap(*lat, *lon, Mode::Bike),
            car: graph.snap(*lat, *lon, Mode::Car),
        })
    };

    let home_ids: Vec<String> = {
        let mut seen = HashSet::new();
        staff_rows
            .iter()
            .map(|s| s.postcode_id.clone())
            .filter(|p| coords.contains_key(p))
            .filter(|p| seen.insert(p.clone()))
            .collect()
    };
    let site_ids: Vec<String> = {
        let mut seen = HashSet::new();
        client_rows
            .iter()
            .map(|c| c.postcode_id.clone())
            .filter(|p| coords.contains_key(p))
            .filter(|p| seen.insert(p.clone()))
            .collect()
    };
    let homes: Vec<Place> = home_ids.iter().map(&snap_place).collect::<Result<_, _>>()?;
    let sites: Vec<Place> = site_ids.iter().map(&snap_place).collect::<Result<_, _>>()?;
    let site_id_set: HashSet<String> = sites.iter().map(|s| s.id.clone()).collect();
    let origins: Vec<Place> = homes
        .iter()
        .filter(|h| !site_id_set.contains(&h.id))
        .chain(sites.iter())
        .cloned()
        .collect();
    println!("Rota span: {from_date} -> {to_date}");
    println!(
        "{} staff homes, {} client sites -> {} origins x {} destinations",
        homes.len(),
        sites.len(),
        origins.len(),
        sites.len()
    );

    for (mode_id, mode) in [("car", Mode::Car), ("bicycle", Mode::Bike), ("walk", Mode::Walk)] {
        let have = travel_times::Entity::find()
            .filter(travel_times::Column::TransportModeId.eq(mode_id))
            .count(&db)
            .await
            .map_err(|e| e.to_string())?;
        if have > 0 {
            println!("{mode_id} already present ({have} rows), skipping");
            continue;
        }
        println!("Computing {mode_id} (time-independent)...");
        let rows: Vec<travel_times::ActiveModel> = tokio::task::block_in_place(|| {
            origins
                .par_iter()
                .flat_map_iter(|origin| {
                    let mode_point = match mode {
                        Mode::Walk => Some(origin.walk),
                        Mode::Bike => origin.bike,
                        Mode::Car => origin.car,
                    };
                    let mode_dist = mode_point.and_then(|p| graph.dists(&p, mode));
                    let walk_dist = if mode == Mode::Walk {
                        None
                    } else {
                        graph.dists(&origin.walk, Mode::Walk)
                    };
                    let mut out = Vec::new();
                    for site in &sites {
                        if site.id == origin.id {
                            continue;
                        }
                        let mut d = UNREACHABLE;
                        if let Some(dist) = &mode_dist {
                            let site_point = match mode {
                                Mode::Walk => Some(site.walk),
                                Mode::Bike => site.bike,
                                Mode::Car => site.car,
                            };
                            if let Some(site_point) = site_point {
                                d = graph.arrival(dist, &site_point, mode);
                            }
                        }
                        if let Some(dist) = &walk_dist {
                            d = d.min(graph.arrival(dist, &site.walk, Mode::Walk));
                        }
                        if d > MAX_TRIP_SECONDS {
                            continue;
                        }
                        out.push((origin.id.clone(), site.id.clone(), minutes(d)));
                    }
                    out
                })
                .map(|(from, to, mins)| travel_times::ActiveModel {
                    from_postcode_id: Set(from),
                    to_postcode_id: Set(to),
                    transport_mode_id: Set(mode_id.to_string()),
                    travel_mins: Set(mins),
                    departure_time: Set(None),
                    ..Default::default()
                })
                .collect()
        });
        let count = rows.len();
        insert_rows(&db, rows).await?;
        println!("   {count} rows");
    }

    let network = tokio::task::block_in_place(|| load_network(gtfs, &graph))?;

    let stop_secs_from = |dist: &HashMap<u32, u32>| -> Vec<u32> {
        network
            .stop_nodes
            .iter()
            .map(|(_, point)| {
                let d = graph.arrival(dist, point, Mode::Walk);
                if d > ACCESS_MAX_SECONDS { UNREACHABLE } else { d }
            })
            .collect()
    };
    let walks: Vec<(Vec<u32>, Vec<u32>)> = tokio::task::block_in_place(|| {
        origins
            .par_iter()
            .map(|origin| {
                let Some(dist) = graph.dists(&origin.walk, Mode::Walk) else {
                    return (Vec::new(), vec![UNREACHABLE; sites.len()]);
                };
                let site_walk: Vec<u32> = sites
                    .iter()
                    .map(|s| graph.arrival(&dist, &s.walk, Mode::Walk))
                    .collect();
                (stop_secs_from(&dist), site_walk)
            })
            .collect()
    });
    println!("origin walk tables built for {} origins", walks.len());

    let site_walks: Vec<Vec<u32>> = tokio::task::block_in_place(|| {
        sites
            .par_iter()
            .map(|site| {
                graph
                    .dists(&site.walk, Mode::Walk)
                    .map(|dist| stop_secs_from(&dist))
                    .unwrap_or_default()
            })
            .collect()
    });
    println!("site walk tables built for {} sites", sites.len());

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

        let timetable = tokio::task::block_in_place(|| network.day_timetable(day))?;
        let origin_endpoints: Vec<vulture::Endpoints> = walks
            .iter()
            .map(|(stop_walk, _)| network.endpoints(&timetable, stop_walk))
            .collect();
        let site_endpoints: Vec<vulture::Endpoints> = site_walks
            .iter()
            .map(|stop_walk| network.endpoints(&timetable, stop_walk))
            .collect();

        let samples = (DEPARTURE_WINDOW_SECONDS / SAMPLE_STEP_SECONDS) as usize;
        let mut departures: Vec<u32> = Vec::new();
        let mut slot_minutes = FIRST_DEPARTURE_MINUTES;
        while slot_minutes <= LAST_DEPARTURE_MINUTES {
            for k in 0..samples as u32 {
                departures.push(slot_minutes * 60 + k * SAMPLE_STEP_SECONDS);
            }
            slot_minutes += STEP_MINUTES;
        }

        let pairs_total = origins.len() * sites.len();
        let pairs_done = std::sync::atomic::AtomicUsize::new(0);
        let pair_journeys: Vec<Vec<(u32, u32)>> = tokio::task::block_in_place(|| {
            (0..pairs_total)
                .into_par_iter()
                .map_init(
                    || RaptorCache::for_timetable(&timetable),
                    |cache, pair| {
                        let index = pair / sites.len();
                        let site_index = pair % sites.len();
                        let journeys = if origins[index].id == sites[site_index].id {
                            Vec::new()
                        } else {
                            let mut journeys = Vec::new();
                            for chunk in departures.chunks(120) {
                                journeys.extend(day_journeys(
                                    &timetable,
                                    cache,
                                    &origin_endpoints[index],
                                    &site_endpoints[site_index],
                                    chunk,
                                ));
                            }
                            finalize_journeys(journeys)
                        };
                        let done = pairs_done.fetch_add(1, std::sync::atomic::Ordering::Relaxed) + 1;
                        if done % 100 == 0 || done == pairs_total {
                            use std::io::Write;
                            println!("   {day}: {done}/{pairs_total} pairs routed");
                            let _ = std::io::stdout().flush();
                        }
                        journeys
                    },
                )
                .collect()
        });
        println!("   {day}: day journeys computed for {} pairs", pair_journeys.len());

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
            let slot_seconds = slot_minutes * 60;
            let mut rows: Vec<travel_times::ActiveModel> = Vec::new();
            for (index, origin) in origins.iter().enumerate() {
                let (_, site_walk) = &walks[index];
                for (site_index, site) in sites.iter().enumerate() {
                    if site.id == origin.id {
                        continue;
                    }
                    let journeys = &pair_journeys[index * sites.len() + site_index];
                    let walk = site_walk[site_index];
                    let mut window: Vec<u32> = (0..samples as u32)
                        .map(|k| {
                            let depart = slot_seconds + k * SAMPLE_STEP_SECONDS;
                            travel_at(journeys, depart).min(walk)
                        })
                        .collect();
                    let median = percentile_50(&mut window);
                    if median == UNREACHABLE || median > MAX_TRIP_SECONDS {
                        continue;
                    }
                    rows.push(travel_times::ActiveModel {
                        from_postcode_id: Set(origin.id.clone()),
                        to_postcode_id: Set(site.id.clone()),
                        transport_mode_id: Set("transit".to_string()),
                        travel_mins: Set(minutes(median)),
                        departure_time: Set(Some(departure_utc)),
                        ..Default::default()
                    });
                }
            }
            let count = rows.len();
            insert_rows(&db, rows).await?;
            if slot_number % 10 == 0 || slot_minutes == LAST_DEPARTURE_MINUTES {
                println!("   {day}: {slot_number}/{TOTAL_SLOTS} transit slots done ({count} rows this slot)");
            }
            slot_minutes += STEP_MINUTES;
        }
        day = day.succ_opt().ok_or("date overflow")?;
    }

    let total = travel_times::Entity::find().count(&db).await.map_err(|e| e.to_string())?;
    Ok(format!(
        "travel times synced for {from_date} -> {to_date}; table holds {total} rows"
    ))
}

pub async fn probe(
    database_url: &str,
    from_id: &str,
    to_id: &str,
    date: &str,
    at: &str,
    osm: &str,
    gtfs: &[String],
) -> Result<(), String> {
    let date = NaiveDate::parse_from_str(date, "%Y-%m-%d").map_err(|e| e.to_string())?;
    let mut clock = at.split(':');
    let hours: u32 = clock.next().ok_or("bad time")?.parse().map_err(|_| "bad time")?;
    let mins: u32 = clock.next().ok_or("bad time")?.parse().map_err(|_| "bad time")?;
    let depart = hours * 3600 + mins * 60;

    let db = Database::connect(database_url).await.map_err(|e| e.to_string())?;
    let postcode_rows = postcodes::Entity::find()
        .filter(postcodes::Column::Id.is_in([from_id.to_string(), to_id.to_string()]))
        .all(&db)
        .await
        .map_err(|e| e.to_string())?;
    let coords: HashMap<String, (f64, f64)> = postcode_rows
        .into_iter()
        .map(|p| (p.id, (p.latitude, p.longitude)))
        .collect();

    let graph = tokio::task::block_in_place(|| build_street_graph(osm))?;
    let snap_walk = |id: &str| -> Result<SnapPoint, String> {
        let (lat, lon) = coords.get(id).ok_or_else(|| format!("{id}: not in postcodes"))?;
        graph
            .snap(*lat, *lon, Mode::Walk)
            .ok_or_else(|| format!("{id} could not be snapped"))
    };
    let origin = snap_walk(from_id)?;
    let target = snap_walk(to_id)?;
    let network = tokio::task::block_in_place(|| load_network(gtfs, &graph))?;
    let timetable = tokio::task::block_in_place(|| network.day_timetable(date))?;

    let origin_dist = graph.dists(&origin, Mode::Walk).ok_or("origin not walkable")?;
    let target_dist = graph.dists(&target, Mode::Walk).ok_or("target not walkable")?;
    let stop_secs = |dist: &HashMap<u32, u32>| -> Vec<u32> {
        network
            .stop_nodes
            .iter()
            .map(|(_, point)| {
                let d = graph.arrival(dist, point, Mode::Walk);
                if d > MAX_TRIP_SECONDS { UNREACHABLE } else { d }
            })
            .collect()
    };
    let from_secs = stop_secs(&origin_dist);
    let to_secs = stop_secs(&target_dist);
    let from_endpoints = network.endpoints(&timetable, &from_secs);
    let to_endpoints = network.endpoints(&timetable, &to_secs);
    let direct = graph.arrival(&origin_dist, &target, Mode::Walk);
    println!("direct walk: {} secs = {} mins", direct, minutes(direct));
    println!(
        "access stops: {}, egress stops: {}",
        from_endpoints.len(),
        to_endpoints.len()
    );

    use vulture::Timetable;
    let access: HashMap<u32, u32> = from_endpoints
        .as_slice()
        .iter()
        .map(|(s, d)| (s.get(), d.0))
        .collect();
    let journeys = timetable
        .query()
        .from(&from_endpoints)
        .to(&to_endpoints)
        .max_transfers(crate::transit::MAX_TRANSFERS)
        .depart_at(vulture::SecondOfDay(depart))
        .run();
    for journey in &journeys {
        let arrival = journey.arrival().0;
        println!(
            "journey: {} rides, arrive {:02}:{:02} ({} mins total)",
            journey.plan.len(),
            arrival / 3600,
            arrival % 3600 / 60,
            (arrival - depart + 30) / 60
        );
        let origin_walk = access.get(&journey.origin.get()).copied().unwrap_or(0);
        match journey.with_timing(&timetable, vulture::SecondOfDay(depart), vulture::Duration(origin_walk)) {
            Ok(legs) => {
                println!("  walk {}s to {}", origin_walk, timetable.stop_id(journey.origin));
                for leg in legs {
                    println!(
                        "  route {} {} {:02}:{:02} -> {} {:02}:{:02}",
                        timetable.route_id(leg.route),
                        timetable.stop_id(leg.board),
                        leg.depart.0 / 3600,
                        leg.depart.0 % 3600 / 60,
                        timetable.stop_id(leg.alight),
                        leg.arrive.0 / 3600,
                        leg.arrive.0 % 3600 / 60,
                    );
                }
            }
            Err(e) => println!("  timing failed: {e:?}"),
        }
    }
    Ok(())
}

pub async fn probe_street(
    database_url: &str,
    from_id: &str,
    to_id: &str,
    mode: &str,
    osm: &str,
) -> Result<(), String> {
    let mode = match mode {
        "walk" => Mode::Walk,
        "bicycle" => Mode::Bike,
        other => return Err(format!("{other}: probe supports walk or bicycle")),
    };
    let db = Database::connect(database_url).await.map_err(|e| e.to_string())?;
    let postcode_rows = postcodes::Entity::find()
        .filter(postcodes::Column::Id.is_in([from_id.to_string(), to_id.to_string()]))
        .all(&db)
        .await
        .map_err(|e| e.to_string())?;
    let coords: HashMap<String, (f64, f64)> = postcode_rows
        .into_iter()
        .map(|p| (p.id, (p.latitude, p.longitude)))
        .collect();
    let graph = tokio::task::block_in_place(|| build_street_graph(osm))?;
    let snap = |id: &str| -> Result<SnapPoint, String> {
        let (lat, lon) = coords.get(id).ok_or_else(|| format!("{id}: not in postcodes"))?;
        graph
            .snap(*lat, *lon, mode)
            .ok_or_else(|| format!("{id} could not be snapped"))
    };
    let from = snap(from_id)?;
    let to = snap(to_id)?;
    let Some(route) = graph.route_nodes(&from, &to, mode) else {
        return Err("no route".to_string());
    };
    println!("{} route nodes", route.len());
    let step = (route.len() / 40).max(1);
    for (i, (lat, lon)) in route.iter().enumerate() {
        if i % step == 0 || i == route.len() - 1 {
            println!("  {lat:.5},{lon:.5}");
        }
    }
    Ok(())
}
