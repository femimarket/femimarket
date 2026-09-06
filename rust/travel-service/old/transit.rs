use crate::timetable::ArrayTimetable;
use crate::valhalla_streets::{Streets, UNREACHABLE};
use rayon::prelude::*;
use chrono::NaiveDate;
use gtfs_structures::Gtfs;
use rstar::primitives::GeomWithData;
use rstar::RTree;
use std::collections::HashMap;
use std::sync::Arc;
use vulture::{Endpoints, SecondOfDay, Timetable};

pub const TRANSFER_MAX_SECONDS: u32 = 1000;
pub const TRANSFER_CANDIDATE_METERS: f64 = 1500.0;
pub const ACCESS_CANDIDATE_METERS: f64 = 2200.0;
pub const WALK_KPH: f32 = 3.6;
pub const STOP_MARGIN_METERS: f64 = 12_000.0;
pub const MAX_TRANSFERS: u8 = 7;

const DEG_M: f64 = 111_320.0;

pub struct Network {
    pub gtfs: Gtfs,
    pub stops: Vec<(String, f64, f64)>,
    tree: RTree<GeomWithData<[f64; 2], u32>>,
    lon_scale: f64,
}

fn stops_near(
    tree: &RTree<GeomWithData<[f64; 2], u32>>,
    lon_scale: f64,
    lat: f64,
    lon: f64,
    radius_m: f64,
) -> Vec<u32> {
    let query = [lon * lon_scale, lat];
    let radius_deg = radius_m / DEG_M;
    tree.locate_within_distance(query, radius_deg * radius_deg)
        .map(|c| c.data)
        .collect()
}

pub fn load_network(gtfs_paths: &[String], tiles: &str, places: &[(f64, f64)]) -> Result<Network, String> {
    let mut merged: Option<Gtfs> = None;
    for (feed_index, path) in gtfs_paths.iter().enumerate() {
        println!("Loading GTFS feed: {path}");
        let feed = Gtfs::new(path).map_err(|e| format!("{path}: {e}"))?;
        merged = Some(match merged {
            None => feed,
            Some(mut base) => {
                let prefix = format!("{feed_index}:");
                let mut stops: HashMap<String, Arc<gtfs_structures::Stop>> = HashMap::new();
                for (id, stop) in feed.stops {
                    let mut stop = (*stop).clone();
                    stop.id = format!("{prefix}{id}");
                    stops.insert(stop.id.clone(), Arc::new(stop));
                }
                for (id, mut trip) in feed.trips {
                    trip.id = format!("{prefix}{id}");
                    trip.service_id = format!("{prefix}{}", trip.service_id);
                    trip.route_id = format!("{prefix}{}", trip.route_id);
                    for stop_time in &mut trip.stop_times {
                        let key = format!("{prefix}{}", stop_time.stop.id);
                        let stop = stops
                            .get(&key)
                            .ok_or_else(|| format!("{path}: stop {key} missing"))?;
                        stop_time.stop = stop.clone();
                    }
                    base.trips.insert(trip.id.clone(), trip);
                }
                for (id, mut calendar) in feed.calendar {
                    calendar.id = format!("{prefix}{id}");
                    base.calendar.insert(calendar.id.clone(), calendar);
                }
                for (id, dates) in feed.calendar_dates {
                    base.calendar_dates.insert(format!("{prefix}{id}"), dates);
                }
                base.stops.extend(stops);
                base
            }
        });
    }
    let mut gtfs = merged.ok_or("no GTFS feeds given")?;

    let stops: Vec<(String, f64, f64)> = gtfs
        .stops
        .iter()
        .filter_map(|(id, stop)| match (stop.latitude, stop.longitude) {
            (Some(lat), Some(lon)) => Some((id.clone(), lat, lon)),
            _ => None,
        })
        .filter(|&(_, lat, lon)| {
            let margin_lat = STOP_MARGIN_METERS / DEG_M;
            let min_lat = places.iter().map(|p| p.0).fold(f64::MAX, f64::min) - margin_lat;
            let max_lat = places.iter().map(|p| p.0).fold(f64::MIN, f64::max) + margin_lat;
            let cos = ((min_lat + max_lat) / 2.0).to_radians().cos().max(0.1);
            let margin_lon = margin_lat / cos;
            let min_lon = places.iter().map(|p| p.1).fold(f64::MAX, f64::min) - margin_lon;
            let max_lon = places.iter().map(|p| p.1).fold(f64::MIN, f64::max) + margin_lon;
            lat >= min_lat && lat <= max_lat && lon >= min_lon && lon <= max_lon
        })
        .collect();
    let mean_lat = stops.iter().map(|(_, lat, _)| *lat).sum::<f64>() / stops.len().max(1) as f64;
    let lon_scale = mean_lat.to_radians().cos();
    let points: Vec<GeomWithData<[f64; 2], u32>> = stops
        .iter()
        .enumerate()
        .map(|(i, (_, lat, lon))| GeomWithData::new([lon * lon_scale, *lat], i as u32))
        .collect();
    let tree = RTree::bulk_load(points);
    println!("{} stops within {}km of the operation, {} trips", stops.len(), STOP_MARGIN_METERS / 1000.0, gtfs.trips.len());

    let cache_dir = std::path::Path::new(&gtfs_paths[0])
        .parent()
        .unwrap_or_else(|| std::path::Path::new("."));
    let feed_names: Vec<String> = gtfs_paths
        .iter()
        .map(|p| {
            std::path::Path::new(p)
                .file_stem()
                .map(|s| s.to_string_lossy().into_owned())
                .unwrap_or_else(|| "feed".to_string())
        })
        .collect();
    let transfer_cache = cache_dir
        .join(format!(
            "{}.transfers.v3.{TRANSFER_MAX_SECONDS}.bin",
            feed_names.join("+")
        ))
        .to_string_lossy()
        .into_owned();
    let inputs_modified = gtfs_paths
        .iter()
        .filter_map(|p| std::fs::metadata(p).and_then(|m| m.modified()).ok())
        .max();
    let cached: Option<Vec<(String, Vec<(String, u32)>)>> = std::fs::metadata(&transfer_cache)
        .ok()
        .filter(|meta| match (meta.modified().ok(), inputs_modified) {
            (Some(cache_time), Some(input_time)) => cache_time > input_time,
            _ => false,
        })
        .and_then(|_| std::fs::read(&transfer_cache).ok())
        .and_then(|bytes| bincode::deserialize(&bytes).ok());
    let street_transfers: Vec<(String, Vec<(String, u32)>)> = match cached {
        Some(transfers) => {
            println!("street transfers loaded from {transfer_cache}");
            transfers
        }
        None => {
            println!("computing street transfers via valhalla...");
            let done = std::sync::atomic::AtomicUsize::new(0);
            let total = stops.len();
            let computed: Vec<(String, Vec<(String, u32)>)> = stops
                .par_iter()
                .enumerate()
                .map_init(
                    || Streets::new(tiles).expect("tiles opened once already in this run"),
                    |streets, (index, (id, lat, lon))| {
                        let mut candidates =
                            stops_near(&tree, lon_scale, *lat, *lon, TRANSFER_CANDIDATE_METERS);
                        candidates.retain(|&c| c as usize != index);
                        let coords: Vec<(f64, f64)> = candidates
                            .iter()
                            .map(|&s| {
                                let (_, slat, slon) = stops[s as usize];
                                (slat, slon)
                            })
                            .collect();
                        let times = streets
                            .walk_times((*lat, *lon), &coords, WALK_KPH, TRANSFER_MAX_SECONDS)
                            .unwrap_or_else(|_| vec![UNREACHABLE; coords.len()]);
                        let transfers: Vec<(String, u32)> = candidates
                            .iter()
                            .zip(times.iter())
                            .filter(|entry| *entry.1 != UNREACHABLE)
                            .map(|(&c, &secs)| (stops[c as usize].0.clone(), secs))
                            .collect();
                        let n = done.fetch_add(1, std::sync::atomic::Ordering::Relaxed) + 1;
                        if n % 4000 == 0 || n == total {
                            use std::io::Write;
                            println!("   transfers: {n}/{total} stops");
                            let _ = std::io::stdout().flush();
                        }
                        (id.clone(), transfers)
                    },
                )
                .collect();
            match bincode::serialize(&computed) {
                Ok(bytes) => {
                    if let Err(e) = std::fs::write(&transfer_cache, bytes) {
                        println!("transfer cache not written: {e}");
                    } else {
                        println!("street transfers cached to {transfer_cache}");
                    }
                }
                Err(e) => println!("transfer cache not serialized: {e}"),
            }
            computed
        }
    };
    let mut transfer_count = 0usize;
    for (id, transfers) in street_transfers {
        if transfers.is_empty() {
            continue;
        }
        transfer_count += transfers.len();
        if let Some(stop) = gtfs.stops.get(&id) {
            let mut stop = (**stop).clone();
            stop.transfers = transfers
                .into_iter()
                .map(|(to, secs)| gtfs_structures::StopTransfer {
                    to_stop_id: to,
                    transfer_type: gtfs_structures::TransferType::MinTime,
                    min_transfer_time: Some(secs),
                })
                .collect();
            gtfs.stops.insert(id, Arc::new(stop));
        }
    }
    println!("{transfer_count} street-walked transfers injected");

    Ok(Network {
        gtfs,
        stops,
        tree,
        lon_scale,
    })
}

impl Network {
    pub fn stop_walk_times(
        &self,
        streets: &mut Streets,
        lat: f64,
        lon: f64,
        cap_seconds: u32,
    ) -> Result<Vec<u32>, String> {
        let candidates = stops_near(&self.tree, self.lon_scale, lat, lon, ACCESS_CANDIDATE_METERS);
        let coords: Vec<(f64, f64)> = candidates
            .iter()
            .map(|&s| {
                let (_, slat, slon) = self.stops[s as usize];
                (slat, slon)
            })
            .collect();
        let times = streets.walk_times((lat, lon), &coords, WALK_KPH, cap_seconds)?;
        let mut out = vec![UNREACHABLE; self.stops.len()];
        for (i, &stop) in candidates.iter().enumerate() {
            out[stop as usize] = times[i];
        }
        Ok(out)
    }

    pub fn day_timetable(&self, date: NaiveDate) -> Result<ArrayTimetable, String> {
        ArrayTimetable::new(&self.gtfs, date)
    }

    pub fn endpoints(&self, timetable: &ArrayTimetable, stop_secs: &[u32]) -> Endpoints {
        let mut endpoints = Endpoints::new();
        for ((id, _, _), &d) in self.stops.iter().zip(stop_secs.iter()) {
            if d == UNREACHABLE {
                continue;
            }
            if let Some(stop) = timetable.stop_idx(id) {
                endpoints.push(stop, vulture::Duration(d));
            }
        }
        endpoints
    }
}

pub fn day_journeys(
    timetable: &ArrayTimetable,
    cache: &mut vulture::RaptorCache<vulture::ArrivalTime>,
    from: &Endpoints,
    to: &Endpoints,
    departures: &[u32],
) -> Vec<(u32, u32)> {
    if from.is_empty() || to.is_empty() {
        return Vec::new();
    }
    timetable
        .query()
        .from(from)
        .to(to)
        .max_transfers(MAX_TRANSFERS)
        .depart_in_window(departures.iter().map(|&d| SecondOfDay(d)))
        .run_with_cache(cache)
        .into_iter()
        .map(|j| (j.depart.0, j.journey.arrival().0))
        .collect()
}

pub fn finalize_journeys(mut journeys: Vec<(u32, u32)>) -> Vec<(u32, u32)> {
    journeys.sort_unstable();
    let mut best = u32::MAX;
    for entry in journeys.iter_mut().rev() {
        best = best.min(entry.1);
        entry.1 = best;
    }
    journeys
}

pub fn travel_at(journeys: &[(u32, u32)], depart: u32) -> u32 {
    let from = journeys.partition_point(|&(d, _)| d < depart);
    journeys
        .get(from)
        .map(|&(_, arrival)| arrival.saturating_sub(depart))
        .unwrap_or(UNREACHABLE)
}
