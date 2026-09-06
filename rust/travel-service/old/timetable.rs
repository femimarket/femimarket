use chrono::{Datelike, NaiveDate, Weekday};
use gtfs_structures::{Exception, Gtfs, PickupDropOffType, StopTime};
use std::collections::{BTreeMap, HashMap};
use vulture::{Duration, RouteIdx, SecondOfDay, StopIdx, Timetable, TripIdx};

const DEFAULT_TRANSFER_TIME: Duration = Duration(300);

pub struct ArrayTimetable {
    stop_ids: Vec<String>,
    stop_by_id: HashMap<String, StopIdx>,
    route_ids: Vec<String>,
    stops_for_route: Vec<Vec<StopIdx>>,
    trip_start_for_route: Vec<u32>,
    trip_count_for_route: Vec<u32>,
    dep_tables: Vec<Vec<SecondOfDay>>,
    arr_tables: Vec<Vec<SecondOfDay>>,
    no_pickup_tables: Vec<Vec<bool>>,
    route_for_trip: Vec<(RouteIdx, u32)>,
    routes_for_stop: Vec<Vec<(RouteIdx, u32)>>,
    footpaths: Vec<Vec<StopIdx>>,
    footpath_times: Vec<Vec<Duration>>,
}

fn service_active(gtfs: &Gtfs, service_id: &str, date: NaiveDate) -> bool {
    if let Some(dates) = gtfs.calendar_dates.get(service_id) {
        if let Some(exception) = dates.iter().find(|d| d.date == date) {
            return matches!(exception.exception_type, Exception::Added);
        }
    }
    if let Some(calendar) = gtfs.calendar.get(service_id) {
        if date < calendar.start_date || date > calendar.end_date {
            return false;
        }
        return match date.weekday() {
            Weekday::Mon => calendar.monday,
            Weekday::Tue => calendar.tuesday,
            Weekday::Wed => calendar.wednesday,
            Weekday::Thu => calendar.thursday,
            Weekday::Fri => calendar.friday,
            Weekday::Sat => calendar.saturday,
            Weekday::Sun => calendar.sunday,
        };
    }
    false
}

fn overtakes(earlier: &[StopTime], later: &[StopTime]) -> bool {
    earlier.iter().zip(later).any(|(e, l)| {
        let (Some(e_dep), Some(l_dep)) = (e.departure_time, l.departure_time) else {
            return true;
        };
        if l_dep < e_dep {
            return true;
        }
        if let (Some(e_arr), Some(l_arr)) = (e.arrival_time, l.arrival_time) {
            if l_arr < e_arr {
                return true;
            }
        }
        false
    })
}

fn split_non_overtaking<'g>(
    trips: &[(&'g str, &'g [StopTime])],
) -> Vec<Vec<(&'g str, &'g [StopTime])>> {
    let mut sub_groups: Vec<Vec<(&'g str, &'g [StopTime])>> = Vec::new();
    'outer: for &entry in trips {
        for sub_group in &mut sub_groups {
            let (_, last) = *sub_group.last().expect("sub_groups seeded non-empty");
            if !overtakes(last, entry.1) {
                sub_group.push(entry);
                continue 'outer;
            }
        }
        sub_groups.push(vec![entry]);
    }
    sub_groups
}

impl ArrayTimetable {
    pub fn new(gtfs: &Gtfs, date: NaiveDate) -> Result<Self, String> {
        let mut stop_ids: Vec<String> = Vec::with_capacity(gtfs.stops.len());
        let mut stop_by_id: HashMap<String, StopIdx> = HashMap::with_capacity(gtfs.stops.len());
        for id in gtfs.stops.keys() {
            stop_by_id.insert(id.clone(), StopIdx::from(stop_ids.len() as u32));
            stop_ids.push(id.clone());
        }

        let mut groups: BTreeMap<(&str, Vec<StopIdx>), Vec<(&str, &[StopTime])>> = BTreeMap::new();
        for (trip_id, trip) in &gtfs.trips {
            if !service_active(gtfs, &trip.service_id, date) {
                continue;
            }
            if trip.stop_times.is_empty() {
                return Err(format!("trip {trip_id}: no stop_times"));
            }
            let mut stop_seq: Vec<StopIdx> = Vec::with_capacity(trip.stop_times.len());
            for stop_time in &trip.stop_times {
                let Some(&idx) = stop_by_id.get(stop_time.stop.id.as_str()) else {
                    return Err(format!("trip {trip_id}: unknown stop {}", stop_time.stop.id));
                };
                if stop_time.departure_time.is_none() {
                    return Err(format!("trip {trip_id}: missing departure time"));
                }
                stop_seq.push(idx);
            }
            groups
                .entry((trip.route_id.as_str(), stop_seq))
                .or_default()
                .push((trip_id.as_str(), trip.stop_times.as_slice()));
        }

        let mut route_ids: Vec<String> = Vec::new();
        let mut stops_for_route: Vec<Vec<StopIdx>> = Vec::new();
        let mut trip_start_for_route: Vec<u32> = Vec::new();
        let mut trip_count_for_route: Vec<u32> = Vec::new();
        let mut dep_tables: Vec<Vec<SecondOfDay>> = Vec::new();
        let mut arr_tables: Vec<Vec<SecondOfDay>> = Vec::new();
        let mut no_pickup_tables: Vec<Vec<bool>> = Vec::new();
        let mut route_for_trip: Vec<(RouteIdx, u32)> = Vec::new();
        let mut routes_for_stop: Vec<Vec<(RouteIdx, u32)>> = vec![Vec::new(); stop_ids.len()];

        for ((gtfs_route_id, stop_seq), mut trips) in groups {
            trips.sort_by_key(|(_, stop_times)| stop_times[0].departure_time);
            for sub_group in split_non_overtaking(&trips) {
                let route = RouteIdx::from(route_ids.len() as u32);
                let stops = stop_seq.len();
                let count = sub_group.len();
                let mut dep_table = vec![SecondOfDay(u32::MAX); stops * count];
                let mut arr_table = vec![SecondOfDay(u32::MAX); stops * count];
                let mut no_pickup_table = vec![false; stops * count];
                let start = route_for_trip.len() as u32;
                for (trip_pos, (_, stop_times)) in sub_group.iter().enumerate() {
                    route_for_trip.push((route, trip_pos as u32));
                    for (stop_pos, stop_time) in stop_times.iter().enumerate() {
                        let dep = stop_time.departure_time.expect("checked above");
                        dep_table[stop_pos * count + trip_pos] = SecondOfDay(dep);
                        if matches!(stop_time.pickup_type, PickupDropOffType::NotAvailable) {
                            no_pickup_table[stop_pos * count + trip_pos] = true;
                        }
                        let no_drop_off =
                            matches!(stop_time.drop_off_type, PickupDropOffType::NotAvailable);
                        if !no_drop_off {
                            if let Some(arr) = stop_time.arrival_time {
                                arr_table[stop_pos * count + trip_pos] = SecondOfDay(arr);
                            }
                        }
                    }
                }
                for (pos, &stop) in stop_seq.iter().enumerate() {
                    let entry = &mut routes_for_stop[stop.get() as usize];
                    if !entry.iter().any(|(r, _)| *r == route) {
                        entry.push((route, pos as u32));
                    }
                }
                route_ids.push(gtfs_route_id.to_owned());
                stops_for_route.push(stop_seq.clone());
                trip_start_for_route.push(start);
                trip_count_for_route.push(count as u32);
                dep_tables.push(dep_table);
                arr_tables.push(arr_table);
                no_pickup_tables.push(no_pickup_table);
            }
        }

        let mut footpaths: Vec<Vec<StopIdx>> = vec![Vec::new(); stop_ids.len()];
        let mut footpath_times: Vec<Vec<Duration>> = vec![Vec::new(); stop_ids.len()];
        for (stop_id, stop) in &gtfs.stops {
            if stop.transfers.is_empty() {
                continue;
            }
            let from = stop_by_id[stop_id.as_str()];
            let mut pairs: Vec<(StopIdx, Duration)> = stop
                .transfers
                .iter()
                .filter_map(|t| {
                    stop_by_id.get(t.to_stop_id.as_str()).map(|&to| {
                        (
                            to,
                            t.min_transfer_time.map(Duration).unwrap_or(DEFAULT_TRANSFER_TIME),
                        )
                    })
                })
                .collect();
            pairs.sort_by_key(|(to, _)| to.get());
            pairs.dedup_by_key(|(to, _)| to.get());
            footpaths[from.get() as usize] = pairs.iter().map(|&(to, _)| to).collect();
            footpath_times[from.get() as usize] = pairs.iter().map(|&(_, d)| d).collect();
        }

        Ok(ArrayTimetable {
            stop_ids,
            stop_by_id,
            route_ids,
            stops_for_route,
            trip_start_for_route,
            trip_count_for_route,
            dep_tables,
            arr_tables,
            no_pickup_tables,
            route_for_trip,
            routes_for_stop,
            footpaths,
            footpath_times,
        })
    }

    pub fn stop_idx(&self, id: &str) -> Option<StopIdx> {
        self.stop_by_id.get(id).copied()
    }

    pub fn stop_id(&self, stop: StopIdx) -> &str {
        &self.stop_ids[stop.get() as usize]
    }

    pub fn route_id(&self, route: RouteIdx) -> &str {
        &self.route_ids[route.get() as usize]
    }

    fn dep_row(&self, route: RouteIdx, pos: u32) -> &[SecondOfDay] {
        let r = route.get() as usize;
        let count = self.trip_count_for_route[r] as usize;
        let base = pos as usize * count;
        &self.dep_tables[r][base..base + count]
    }
}

impl Timetable for ArrayTimetable {
    fn n_stops(&self) -> usize {
        self.stop_ids.len()
    }

    fn n_routes(&self) -> usize {
        self.route_ids.len()
    }

    fn get_routes_serving_stop(&self, stop: StopIdx) -> &[(RouteIdx, u32)] {
        &self.routes_for_stop[stop.get() as usize]
    }

    fn get_stops_after(&self, route: RouteIdx, pos: u32) -> &[StopIdx] {
        &self.stops_for_route[route.get() as usize][pos as usize..]
    }

    fn stop_at(&self, route: RouteIdx, pos: u32) -> StopIdx {
        self.stops_for_route[route.get() as usize][pos as usize]
    }

    fn get_earliest_trip(&self, route: RouteIdx, at: SecondOfDay, pos: u32) -> Option<TripIdx> {
        let r = route.get() as usize;
        let count = self.trip_count_for_route[r] as usize;
        let base = pos as usize * count;
        let row = &self.dep_tables[r][base..base + count];
        let flags = &self.no_pickup_tables[r][base..base + count];
        let mut idx = row.partition_point(|&dep| dep < at);
        while idx < row.len() {
            if !flags[idx] && row[idx].0 != u32::MAX {
                let start = self.trip_start_for_route[r];
                return Some(TripIdx::from(start + idx as u32));
            }
            idx += 1;
        }
        None
    }

    fn get_arrival_time(&self, trip: TripIdx, pos: u32) -> SecondOfDay {
        let (route, trip_pos) = self.route_for_trip[trip.get() as usize];
        let r = route.get() as usize;
        let count = self.trip_count_for_route[r] as usize;
        self.arr_tables[r][pos as usize * count + trip_pos as usize]
    }

    fn get_departure_time(&self, trip: TripIdx, pos: u32) -> SecondOfDay {
        let (route, trip_pos) = self.route_for_trip[trip.get() as usize];
        let r = route.get() as usize;
        let count = self.trip_count_for_route[r] as usize;
        self.dep_tables[r][pos as usize * count + trip_pos as usize]
    }

    fn pickup_allowed(&self, trip: TripIdx, pos: u32) -> bool {
        let (route, trip_pos) = self.route_for_trip[trip.get() as usize];
        let r = route.get() as usize;
        let count = self.trip_count_for_route[r] as usize;
        !self.no_pickup_tables[r][pos as usize * count + trip_pos as usize]
            && self.get_departure_time(trip, pos).0 != u32::MAX
    }

    fn drop_off_allowed(&self, trip: TripIdx, pos: u32) -> bool {
        self.get_arrival_time(trip, pos).0 != u32::MAX
    }

    fn get_footpaths_from(&self, stop: StopIdx) -> &[StopIdx] {
        &self.footpaths[stop.get() as usize]
    }

    fn get_transfer_time(&self, from: StopIdx, to: StopIdx) -> Duration {
        let neighbors = &self.footpaths[from.get() as usize];
        match neighbors.binary_search_by_key(&to.get(), |s| s.get()) {
            Ok(i) => self.footpath_times[from.get() as usize][i],
            Err(_) => DEFAULT_TRANSFER_TIME,
        }
    }

    fn footpaths_are_transitively_closed(&self) -> bool {
        true
    }
}
