use crate::assignments::Assignment;
use crate::availabilities::Availability;
use crate::banned_double_ups::BannedDoubleUp;
use crate::blacklisted_workers::BlacklistedWorker;
use crate::break_rules::BreakRule;
use crate::carer_rules::CarerRule;
use crate::critical_visit_rules::CriticalVisitRule;
use crate::daily_call_rules::DailyCallRule;
use crate::double_ups::DoubleUp;
use crate::early_leave_rules::EarlyLeaveRule;
use crate::passenger_rules::PassengerRule;
use crate::preferred_workers::PreferredWorker;
use crate::rota::Rota;
use crate::shifts::ShiftTimeRuleClient;
use crate::staffs::Staff;
use crate::supervisors::Supervisor;
use crate::whitelisted_workers::WhitelistedWorker;
use chrono::Timelike;
use cp_sat::builder::{BoolVar, CpModelBuilder, IntVar, LinearExpr};
use cp_sat::proto::{CpSolverStatus, SatParameters};
use mysql_async::params;
use mysql_async::prelude::*;
use std::collections::{BTreeMap, HashMap, HashSet};

#[derive(PartialEq, Eq, Hash)]
struct Journey {
    transport_mode_id: String,
    from_postcode_id: String,
    to_postcode_id: String,
}

#[derive(PartialEq, Eq, PartialOrd, Ord)]
struct BusDeparture {
    from_postcode_id: String,
    to_postcode_id: String,
    date: chrono::NaiveDate,
    travel_time_departure_minute: i64,
}

#[derive(PartialEq, Eq, Hash)]
struct NextShift {
    staff: usize,
    from_shift: usize,
    to_shift: usize,
}

#[derive(PartialEq, Eq, Hash)]
struct StaffShift {
    staff: usize,
    shift: usize,
}

#[derive(PartialEq, Eq, Hash)]
struct StaffDay {
    staff_id: i32,
    date: chrono::NaiveDate,
}

#[derive(Clone)]
struct DayHours {
    from: i64,
    to: i64,
}

#[derive(PartialEq, Eq, Hash)]
struct PinnedCare {
    staff_id: i32,
    shift_id: i32,
}

struct BannedPair {
    staff_id: i32,
    partner_id: i32,
}

struct BannedCell {
    first: usize,
    second: usize,
    shift: usize,
}

struct HourWall {
    staff: usize,
    shift: usize,
    windows: Vec<DayHours>,
}

#[derive(PartialEq, Eq, Hash)]
struct ClientDay {
    client_id: i32,
    date: chrono::NaiveDate,
}

struct ClientGap {
    earlier: usize,
    later: usize,
    gap: i64,
}

struct CandidateStep {
    from_shift: usize,
    to_shift: usize,
    taken: BoolVar,
}

struct Ride {
    driver: usize,
    chosen: BoolVar,
}

struct BusTime {
    travel_time_departure_minute: i64,
    travel_mins: i64,
}

struct BusOption {
    travel_time_departure_minute: i64,
    travel_mins: i64,
    chosen: BoolVar,
}

struct RideLink {
    ride: BoolVar,
    driver_step: Option<BoolVar>,
}

struct BreakCandidate {
    from_shift: usize,
    to_shift: usize,
    taken: BoolVar,
    bus_travel_mins: Option<IntVar>,
    own_travel_mins: Option<i64>,
    car_travel_mins: Option<i64>,
}

struct Placement {
    staff: usize,
    shift: usize,
    start_minute: i64,
    late_mins: i64,
    left_early_mins: i64,
}

fn hhmm(m: i64) -> String {
    format!("{:02}:{:02}:00", m / 60, m % 60)
}

pub async fn solve(
    database_url: &str,
    rota_id: i32,
    max_seconds: i64,
    travel_times_json: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    let pool = mysql_async::Pool::new(database_url);
    let mut conn = pool.get_conn().await?;
    let started = std::time::Instant::now();

    let rota: Rota = conn
        .exec_first(include_str!("rota.sql"), params! { "id" => rota_id })
        .await?
        .ok_or_else(|| format!("rota {rota_id} does not exist"))?;

    let weight_penalty_uncovered_visit = conn
        .query_first::<i32, _>(include_str!("weight_penalty_uncovered_visit.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_uncovered_visit is empty")?;
    let weight_penalty_uncovered_high_priority_multiplier = conn
        .query_first::<f64, _>(include_str!("weight_penalty_uncovered_high_priority_multiplier.sql")).await?
        .ok_or("critical multiplier missing")?;
    let weight_penalty_partial_double_up = conn
        .query_first::<i32, _>(include_str!("weight_penalty_partial_double_up.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_partial_double_up is empty")?;
    let weight_penalty_per_taxi_journey = conn
        .query_first::<i32, _>(include_str!("weight_penalty_per_taxi_journey.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_per_taxi_journey is empty")?;
    let weight_penalty_per_minute_late = conn
        .query_first::<i32, _>(include_str!("weight_penalty_per_minute_late.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_per_minute_late is empty")?;
    let weight_penalty_per_minute_permissioned_stretch = conn
        .query_first::<i32, _>(include_str!("weight_penalty_per_minute_permissioned_stretch.sql")).await?
        .map(|penalty| penalty as i64)
        .ok_or("weight_penalty_per_minute_permissioned_stretch is empty")?;
    let weight_penalty_per_minute_start_moved = conn
        .query_first::<i32, _>(include_str!("weight_penalty_per_minute_start_moved.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_per_minute_start_moved is empty")?;
    let weight_penalty_per_minute_travel = conn
        .query_first::<i32, _>(include_str!("weight_penalty_per_minute_travel.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_per_minute_travel is empty")?;
    let weight_penalty_per_minute_early_leave = conn
        .query_first::<i32, _>(include_str!("weight_penalty_per_minute_early_leave.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_per_minute_early_leave is empty")?;
    let weight_penalty_non_preferred_worker = conn
        .query_first::<i32, _>(include_str!("weight_penalty_non_preferred_worker.sql")).await?
        .map(|penalty| penalty as i64).ok_or("weight_penalty_non_preferred_worker is empty")?;
    let weight_min_same_client_gap_mins = conn
        .query_first::<i32, _>(include_str!("weight_min_same_client_gap_mins.sql")).await?
        .map(|mins| mins as i64).ok_or("weight_min_same_client_gap_mins is empty")?;
    let weight_solver_workers = conn
        .query_first::<i32, _>(include_str!("weight_solver_workers.sql")).await?
        .ok_or("weight_solver_workers is empty")?;

    let span = params! { "from_date" => rota.from_date, "to_date" => rota.to_date };
    let shifts: Vec<ShiftTimeRuleClient> = conn.exec(include_str!("shifts.sql"), span.clone()).await?;
    shifts.first().ok_or_else(|| format!("rota {rota_id} has no shifts in its span"))?;
    let span_dates: HashSet<chrono::NaiveDate> = shifts.iter().map(|s| s.on_date).collect();

    let critical_visit_rules: Vec<CriticalVisitRule> =
        conn.exec(include_str!("critical_visit_rules.sql"), span.clone()).await?;
    let passenger_rules: Vec<PassengerRule> =
        conn.exec(include_str!("passenger_rules.sql"), span.clone()).await?;
    let carer_rules: Vec<CarerRule> = conn.query(include_str!("carer_rules.sql")).await?;
    let early_leave_rules: Vec<EarlyLeaveRule> = conn.query(include_str!("early_leave_rules.sql")).await?;
    let daily_call_rules: Vec<DailyCallRule> =
        conn.exec(include_str!("daily_call_rules.sql"), span.clone()).await?;
    let break_rules: Vec<BreakRule> = conn.exec(include_str!("break_rules.sql"), span.clone()).await?;

    let staffs: Vec<Staff> = conn.query(include_str!("staffs.sql")).await?;

    let availability: HashMap<StaffDay, Vec<DayHours>> =
        conn.exec::<Availability, _, _>(include_str!("availabilities.sql"), span.clone()).await?
            .iter()
            .flat_map(|a| span_dates.iter()
                .filter(|d| a.from_date <= **d && **d <= a.to_date)
                .map(|d| (StaffDay { staff_id: a.staff_id, date: *d },
                    DayHours {
                        from: a.start_time.hour() as i64 * 60 + a.start_time.minute() as i64,
                        to: a.end_time.hour() as i64 * 60 + a.end_time.minute() as i64,
                    }))
                .collect::<Vec<_>>())
            .fold(HashMap::new(), |mut acc, (day, hours)| {
                acc.entry(day).or_insert_with(Vec::new).push(hours);
                acc
            });

    let span_from = chrono::NaiveDateTime::new(rota.from_date, chrono::NaiveTime::MIN).and_utc();
    let span_to = chrono::NaiveDateTime::new(rota.to_date,
        chrono::NaiveTime::from_hms_opt(23, 59, 59).expect("time")).and_utc();
    #[derive(serde::Deserialize)]
    struct TravelTime {
        from_postcode_id: String,
        to_postcode_id: String,
        transport_mode_id: String,
        travel_mins: i32,
        departure_time: Option<chrono::DateTime<chrono::Utc>>,
    }
    let travel_times: Vec<TravelTime> =
        serde_json::from_str::<Vec<TravelTime>>(&std::fs::read_to_string(travel_times_json)?)?
            .into_iter()
            .filter(|t| match t.departure_time {
                None => true,
                Some(dt) => span_from <= dt && dt <= span_to,
            })
            .collect();
    travel_times.first()
        .ok_or_else(|| format!("{travel_times_json}: no travel times cover {} -> {}",
            rota.from_date, rota.to_date))?;
    let street_tables: HashMap<Journey, i64> = travel_times.iter()
        .filter(|r| r.departure_time.is_none())
        .fold(HashMap::new(), |mut acc, r| {
            let entry = acc
                .entry(Journey {
                    transport_mode_id: r.transport_mode_id.clone(),
                    from_postcode_id: r.from_postcode_id.clone(),
                    to_postcode_id: r.to_postcode_id.clone(),
                })
                .or_insert(0);
            *entry = (*entry).max(r.travel_mins as i64);
            acc
        });
    let transit_profiles: BTreeMap<BusDeparture, i64> = travel_times.iter()
        .fold(BTreeMap::new(), |mut acc, r| match r.departure_time {
            None => acc,
            Some(dt) => {
                let entry = acc
                    .entry(BusDeparture {
                        from_postcode_id: r.from_postcode_id.clone(),
                        to_postcode_id: r.to_postcode_id.clone(),
                        date: dt.date_naive(),
                        travel_time_departure_minute:
                            (dt.hour() as i64 * 60 + dt.minute() as i64) / 15 * 15,
                    })
                    .or_insert(0);
                *entry = (*entry).max(r.travel_mins as i64);
                acc
            }
        });
    let lookup = |table: &str, from: &str, to: &str| -> Option<i64> {
        (from == to).then_some(0).or_else(|| {
            street_tables.get(&Journey {
                transport_mode_id: table.to_string(),
                from_postcode_id: from.to_string(),
                to_postcode_id: to.to_string(),
            }).copied()
        })
    };
    let reach = |w: &Staff, from: &str, to: &str, date: chrono::NaiveDate| -> bool {
        match w.transport_mode_id() {
            "transit" => from == to
                || transit_profiles
                    .range(BusDeparture {
                            from_postcode_id: from.to_string(),
                            to_postcode_id: to.to_string(),
                            date,
                            travel_time_departure_minute: 0,
                        }
                        ..=BusDeparture {
                            from_postcode_id: from.to_string(),
                            to_postcode_id: to.to_string(),
                            date,
                            travel_time_departure_minute: 1439,
                        })
                    .next().is_some(),
            _ => lookup(w.transport_mode_id(), from, to).is_some(),
        }
    };

    let whitelisted_workers: Vec<WhitelistedWorker> = conn.query(include_str!("whitelisted_workers.sql")).await?;
    let blacklisted_workers: Vec<BlacklistedWorker> = conn.query(include_str!("blacklisted_workers.sql")).await?;
    let preferred_workers: Vec<PreferredWorker> = conn.query(include_str!("preferred_workers.sql")).await?;

    let double_ups: HashSet<i32> = conn
        .exec::<DoubleUp, _, _>(include_str!("double_ups.sql"), span.clone()).await?
        .into_iter().map(|d| d.staff_id).collect();

    let supervisors: Vec<Supervisor> = conn.query(include_str!("supervisors.sql")).await?;
    let supervisions: Vec<crate::supervisions::Supervision> =
        conn.exec(include_str!("supervisions.sql"), span.clone()).await?;

    let banned_double_ups: Vec<BannedPair> = conn
        .exec::<BannedDoubleUp, _, _>(include_str!("banned_double_ups.sql"), span.clone()).await?
        .into_iter()
        .map(|b| BannedPair { staff_id: b.staff_id, partner_id: b.partner_id })
        .collect();

    let assignments: Vec<Assignment> = conn
        .exec(include_str!("assignments.sql"), params! { "rota_id" => rota_id }).await?;
    let pinned_care: HashSet<PinnedCare> = assignments.iter()
        .filter(|a| a.assignment_type_id == "care")
        .map(|a| PinnedCare { staff_id: a.staff_id, shift_id: a.shift_id })
        .collect();
    let present_non_care: Vec<&Assignment> = assignments.iter()
        .filter(|a| a.assignment_type_id != "care")
        .collect();

    println!(
        "rota {rota_id} {} -> {}: {} shifts across {} days, {} routable carers",
        rota.from_date, rota.to_date, shifts.len(), span_dates.len(), staffs.len()
    );

    let mut model = CpModelBuilder::default();
    let nv = shifts.len();
    let nw = staffs.len();

    let assign: Vec<Vec<BoolVar>> = (0..nw)
        .map(|_| (0..nv).map(|_| model.new_bool_var()).collect())
        .collect();
    let start: Vec<IntVar> = shifts.iter()
        .map(|v| model.new_int_var([(v.permissioned_from(), v.permissioned_to())]))
        .collect();
    let late: Vec<Vec<IntVar>> = (0..nw)
        .map(|wi| (0..nv).map(|vi| {
            let _ = wi;
            model.new_int_var([(0, shifts[vi].max_late_mins())])
        }).collect())
        .collect();
    let uncovered: Vec<IntVar> = shifts.iter()
        .map(|v| model.new_int_var([(0, v.carers_required(&carer_rules))]))
        .collect();
    let early: Vec<Vec<IntVar>> = (0..nw)
        .map(|wi| (0..nv).map(|vi| {
            let cap = shifts[vi]
                .max_early_leave_mins(&early_leave_rules);
            let _ = wi;
            model.new_int_var([(0, cap)])
        }).collect())
        .collect();

    shifts.iter().enumerate().for_each(|(vi, v)| {
        let sum = assign.iter().fold(LinearExpr::from(uncovered[vi]), |acc, row| acc + row[vi]);
        model.add_eq(sum, v.carers_required(&carer_rules));
    });

    shifts.iter().enumerate().for_each(|(vi, v)| {
        ["f", "m"].iter().for_each(|g| {
            let required = carer_rules.iter()
                .filter(|r| r.shift_id == v.id && r.cancelled_at.is_none()
                    && r.requires_gender.as_deref() == Some(*g))
                .count() as i64;
            match required > 0 {
                false => {}
                true => {
                    let compatible = staffs.iter().enumerate()
                        .filter(|(_, w)| w.gender.as_deref().map(|wg| wg == *g).unwrap_or(true))
                        .fold(LinearExpr::from(uncovered[vi]), |acc, (wi, _)| acc + assign[wi][vi]);
                    model.add_ge(compatible, required);
                }
            }
        });
    });

    let blacklist: HashSet<StaffShift> = staffs.iter().enumerate()
        .flat_map(|(wi, w)| shifts.iter().enumerate()
            .filter(|(_, v)| {
                v.whitelist_id
                    .map(|id| !whitelisted_workers.iter()
                        .any(|j| j.whitelist_id == id && j.staff_id == w.id))
                    .unwrap_or(false)
                    || v.blacklist_id
                        .map(|id| blacklisted_workers.iter()
                            .any(|j| j.blacklist_id == id && j.staff_id == w.id))
                        .unwrap_or(false)
                    || (double_ups.contains(&w.id) && v.carers_required(&carer_rules) < 2)
                    || availability.get(&StaffDay { staff_id: w.id, date: v.on_date })
                        .is_none_or(|windows| windows.is_empty())
                    || !std::iter::once(w.postcode_id.as_str())
                        .chain(shifts.iter()
                            .filter(|o| o.on_date == v.on_date && o.id != v.id)
                            .map(|o| o.postcode_id()))
                        .any(|from| reach(w, from, v.postcode_id(), v.on_date)
                            || lookup("car", from, v.postcode_id()).is_some())
            })
            .map(move |(vi, _)| StaffShift { staff: wi, shift: vi })
            .collect::<Vec<_>>())
        .collect();
    blacklist.iter().for_each(|c| { model.add_eq(assign[c.staff][c.shift] * 1, 0); });

    let hour_walls: Vec<HourWall> = staffs.iter().enumerate()
        .flat_map(|(wi, w)| shifts.iter().enumerate()
            .filter(|(vi, _)| !blacklist.contains(&StaffShift { staff: wi, shift: *vi }))
            .filter_map(|(vi, v)| availability
                .get(&StaffDay { staff_id: w.id, date: v.on_date })
                .map(|windows| HourWall { staff: wi, shift: vi, windows: windows.clone() }))
            .collect::<Vec<_>>())
        .collect();
    hour_walls.iter().for_each(|wall| {
        let cell = assign[wall.staff][wall.shift];
        let fits_window = |model: &mut CpModelBuilder, h: &DayHours, guards: Vec<BoolVar>| {
            let starts_after = model.add_ge(LinearExpr::from(start[wall.shift]), h.from);
            model.only_enforce_if(starts_after, guards.clone());
            let ends_before = model.add_le(
                LinearExpr::from(start[wall.shift]) + shifts[wall.shift].duration_mins(), h.to);
            model.only_enforce_if(ends_before, guards);
        };
        match wall.windows.as_slice() {
            [only] => fits_window(&mut model, only, vec![cell]),
            windows => {
                let chosen: Vec<BoolVar> = windows.iter()
                    .map(|h| {
                        let inside = model.new_bool_var();
                        fits_window(&mut model, h, vec![cell, inside]);
                        inside
                    })
                    .collect();
                let one_of_them = model.add_or(chosen);
                model.only_enforce_if(one_of_them, [cell]);
            }
        }
    });

    let banned_cells: Vec<BannedCell> = banned_double_ups.iter()
        .filter_map(|p| staffs.iter().position(|w| w.id == p.staff_id)
            .zip(staffs.iter().position(|w| w.id == p.partner_id)))
        .flat_map(|(ai, bi)| (0..nv).map(move |vi| BannedCell { first: ai, second: bi, shift: vi }))
        .collect();
    banned_cells.iter().for_each(|c| {
        model.add_le(LinearExpr::from(assign[c.first][c.shift]) + assign[c.second][c.shift], 1);
    });

    enum Supervision {
        Impossible { staff: usize, shift: usize },
        Watched { staff: usize, shift: usize, supervisors: Vec<usize> },
    }
    let supervision_plans: Vec<Supervision> = supervisions.iter()
        .filter_map(|s| staffs.iter().position(|w| w.id == s.staff_id).map(|wi| (wi, s)))
        .flat_map(|(wi, s)| {
            let sups: Vec<usize> = staffs.iter().enumerate()
                .filter(|(_, w)| supervisors.iter()
                    .any(|j| j.supervision_id == s.id && j.supervisor_staff_id == w.id))
                .map(|(swi, _)| swi)
                .collect();
            shifts.iter().enumerate()
                .filter(|(_, v)| !present_non_care.iter()
                    .any(|a| a.shift_id == v.id && supervisors.iter()
                        .any(|j| j.supervision_id == s.id && j.supervisor_staff_id == a.staff_id)))
                .map(|(vi, _)| match sups.is_empty() {
                    true => Supervision::Impossible { staff: wi, shift: vi },
                    false => Supervision::Watched { staff: wi, shift: vi, supervisors: sups.clone() },
                })
                .collect::<Vec<_>>()
        })
        .collect();
    supervision_plans.iter().for_each(|plan| match plan {
        Supervision::Impossible { staff, shift } => {
            model.add_eq(assign[*staff][*shift] * 1, 0);
        }
        Supervision::Watched { staff, shift, supervisors } => {
            let or = model.add_or(supervisors.iter()
                .map(|&swi| assign[swi][*shift])
                .collect::<Vec<_>>());
            model.only_enforce_if(or, [assign[*staff][*shift]]);
        }
    });

    assignments.iter().filter(|a| a.assignment_type_id == "care").try_for_each(|a| {
        staffs.iter().position(|w| w.id == a.staff_id)
            .zip(shifts.iter().position(|v| v.id == a.shift_id))
            .map(|(wi, vi)| {
                model.add_and([assign[wi][vi]]);
                model.add_eq(
                    LinearExpr::from(start[vi]),
                    a.start_time.hour() as i64 * 60 + a.start_time.minute() as i64,
                );
            })
            .ok_or_else(|| format!("pinned assignment ({}, {}) not solvable", a.staff_id, a.shift_id))
    })?;

    shifts.iter().enumerate().for_each(|(vi, v)| {
        let max_early_leave_mins = v
            .max_early_leave_mins(&early_leave_rules);
        let leaver_bools: Vec<BoolVar> = staffs.iter().enumerate()
            .filter(|(wi, _)| max_early_leave_mins > 0
                && !blacklist.contains(&StaffShift { staff: *wi, shift: vi }))
            .map(|(wi, _)| {
                let uses = model.new_bool_var();
                let capped = model.add_le(LinearExpr::from(early[wi][vi]),
                    LinearExpr::from(uses) * max_early_leave_mins);
                let _ = capped;
                let only_if_assigned = model.add_le(LinearExpr::from(early[wi][vi]),
                    LinearExpr::from(assign[wi][vi]) * max_early_leave_mins);
                let _ = only_if_assigned;
                uses
            })
            .collect();
        match leaver_bools.is_empty() {
            true => {}
            false => {
                let total = leaver_bools.iter().fold(LinearExpr::from(0), |acc, b| acc + *b);
                model.add_le(total, v.carers_may_leave(&early_leave_rules));
            }
        }
    });

    let client_sequences: Vec<ClientGap> = shifts.iter().enumerate()
        .fold(HashMap::<ClientDay, Vec<usize>>::new(), |mut acc, (vi, v)| {
            acc.entry(ClientDay { client_id: v.client_id, date: v.on_date }).or_default().push(vi);
            acc
        })
        .into_values()
        .flat_map(|mut indexes| {
            indexes.sort_by_key(|&vi| shifts[vi].earliest_start());
            indexes.windows(2)
                .map(|pair| {
                    let booked_gap =
                        shifts[pair[1]].earliest_start() - shifts[pair[0]].earliest_start();
                    ClientGap {
                        earlier: pair[0],
                        later: pair[1],
                        gap: weight_min_same_client_gap_mins.min(booked_gap),
                    }
                })
                .collect::<Vec<_>>()
        })
        .collect();
    client_sequences.iter().for_each(|g| {
        model.add_ge(LinearExpr::from(start[g.later]), LinearExpr::from(start[g.earlier]) + g.gap);
    });

    let client_triples: Vec<[usize; 3]> = shifts.iter().enumerate()
        .fold(HashMap::<ClientDay, Vec<usize>>::new(), |mut acc, (vi, v)| {
            acc.entry(ClientDay { client_id: v.client_id, date: v.on_date }).or_default().push(vi);
            acc
        })
        .into_values()
        .flat_map(|mut indexes| {
            indexes.sort_by_key(|&vi| shifts[vi].earliest_start());
            indexes.windows(3).map(|w| [w[0], w[1], w[2]]).collect::<Vec<_>>()
        })
        .collect();
    client_triples.iter().for_each(|t| {
        staffs.iter().enumerate().for_each(|(wi, _)| {
            let in_a_row = LinearExpr::from(assign[wi][t[0]])
                + assign[wi][t[1]] + assign[wi][t[2]];
            model.add_le(in_a_row, 2);
        });
    });

    daily_call_rules.iter().for_each(|rule| {
        span_dates.iter()
            .filter(|date| rule.from_date <= **date && **date <= rule.to_date)
            .for_each(|date| {
                let day_shifts: Vec<usize> = shifts.iter().enumerate()
                    .filter(|(_, v)| v.client_id == rule.client_id && v.on_date == *date)
                    .map(|(vi, _)| vi)
                    .collect();
                staffs.iter().enumerate().for_each(|(wi, _)| {
                    let held = day_shifts.iter()
                        .fold(LinearExpr::from(0), |acc, &vi| acc + assign[wi][vi]);
                    model.add_le(held, rule.max_calls_per_carer as i64);
                });
                let faces = staffs.iter().enumerate()
                    .map(|(wi, _)| {
                        let attends = model.new_bool_var();
                        day_shifts.iter().for_each(|&vi| {
                            model.add_implication(assign[wi][vi], attends);
                        });
                        let none = day_shifts.iter()
                            .fold(LinearExpr::from(0), |acc, &vi| acc + assign[wi][vi]);
                        model.add_le(LinearExpr::from(attends), none);
                        attends
                    })
                    .fold(LinearExpr::from(0), |acc, a| acc + a);
                model.add_le(faces, rule.max_carers as i64);
            });
    });

    let driver_indexes: Vec<usize> = staffs.iter().enumerate()
        .filter(|(_, w)| w.transport_mode_id() == "car" && w.passengers(&passenger_rules) > 0)
        .map(|(wi, _)| wi)
        .collect();

    struct JourneyOptions {
        taken: BoolVar,
        rides: Vec<Ride>,
        own_travel_mins: Option<i64>,
        car_travel_mins: Option<i64>,
        bus_travel_mins: Option<IntVar>,
    }
    let mut next_shifts: HashMap<NextShift, JourneyOptions> = HashMap::new();
    let mut objective = LinearExpr::from(0);

    staffs.iter().enumerate().for_each(|(wi, w)| {
        (0..nv)
            .flat_map(|a| ((a + 1)..nv).map(move |b| (a, b)))
            .filter(|&(a, b)| shifts[a].on_date == shifts[b].on_date)
            .filter(|&(a, b)| !blacklist.contains(&StaffShift { staff: wi, shift: a })
                && !blacklist.contains(&StaffShift { staff: wi, shift: b }))
            .collect::<Vec<_>>()
            .into_iter()
            .for_each(|(a, b)| {
                let ab = model.new_bool_var();
                let ba = model.new_bool_var();
                model.add_le(LinearExpr::from(ab) + ba, 1);
                [CandidateStep { from_shift: a, to_shift: b, taken: ab },
                    CandidateStep { from_shift: b, to_shift: a, taken: ba }]
                    .into_iter().for_each(|CandidateStep { from_shift: first, to_shift: second, taken: seq }| {
                    let same_site = shifts[first].postcode_id() == shifts[second].postcode_id();
                    let own = match same_site {
                        true => Some(0),
                        false => lookup(w.transport_mode_id(),
                            shifts[first].postcode_id(), shifts[second].postcode_id()),
                    };
                    let car = match same_site {
                        true => None,
                        false => lookup("car", shifts[first].postcode_id(), shifts[second].postcode_id()),
                    };
                    let arrival = LinearExpr::from(start[second]) + late[wi][second];
                    let depart = LinearExpr::from(start[first]) + shifts[first].duration_mins()
                        + LinearExpr::from(early[wi][first]) * -1;
                    model.add_implication(seq, assign[wi][first]);
                    model.add_implication(seq, assign[wi][second]);
                    let sequential = model.add_ge(LinearExpr::from(start[second]), depart.clone());
                    model.only_enforce_if(sequential, [seq]);
                    let lo = shifts[first].permissioned_from() + shifts[first].duration_mins()
                        - shifts[first].max_early_leave_mins(&early_leave_rules);
                    let hi = shifts[second].permissioned_to() + shifts[second].max_late_mins();
                    let profile: Option<Vec<BusTime>> =
                        match (w.transport_mode_id(), same_site) {
                            ("transit", false) => Some(transit_profiles
                                .range(BusDeparture {
                                        from_postcode_id: shifts[first].postcode_id().to_string(),
                                        to_postcode_id: shifts[second].postcode_id().to_string(),
                                        date: shifts[first].on_date,
                                        travel_time_departure_minute: 0,
                                    }
                                    ..=BusDeparture {
                                        from_postcode_id: shifts[first].postcode_id().to_string(),
                                        to_postcode_id: shifts[second].postcode_id().to_string(),
                                        date: shifts[first].on_date,
                                        travel_time_departure_minute: 1439,
                                    })
                                .map(|(k, &t)| BusTime {
                                    travel_time_departure_minute: k.travel_time_departure_minute,
                                    travel_mins: t,
                                })
                                .filter(|b| b.travel_time_departure_minute + 14 >= lo
                                    && b.travel_time_departure_minute <= hi)
                                .collect::<Vec<_>>())
                                .filter(|kept| !kept.is_empty()),
                            _ => None,
                        };
                    let carpool_possible = w.transport_mode_id() != "car"
                        && car.is_some() && !driver_indexes.is_empty();
                    let taxi_ok = matches!(w.transport_mode_id(), "transit" | "bicycle" | "walk");
                    let taxi = match profile.is_some() {
                        true => (taxi_ok && car.is_some()).then(|| model.new_bool_var()),
                        false => (taxi_ok && car.is_some() && own != car)
                            .then(|| model.new_bool_var()),
                    };
                    taxi.iter().for_each(|&t| { model.add_implication(t, seq); });
                    let rides: Vec<Ride> =
                        match carpool_possible && shifts[first].carers_required(&carer_rules) >= 2 {
                            false => Vec::new(),
                            true => driver_indexes.iter()
                                .filter(|&&di| di != wi
                                    && !blacklist.contains(&StaffShift { staff: di, shift: first })
                                    && !blacklist.contains(&StaffShift { staff: di, shift: second }))
                                .map(|&di| {
                                    let ride = model.new_bool_var();
                                    model.add_implication(ride, seq);
                                    model.add_implication(ride, assign[di][first]);
                                    model.add_implication(ride, assign[di][second]);
                                    Ride { driver: di, chosen: ride }
                                })
                                .collect(),
                        };
                    let alternatives: Vec<BoolVar> = taxi.iter().copied()
                        .chain(rides.iter().map(|r| r.chosen))
                        .collect();
                    let travel = match &profile {
                        Some(kept) => {
                            let depart_at = model.new_int_var([(lo.max(0), hi + 14)]);
                            let travel = model.new_int_var([(0, 24 * 60)]);
                            let bus = model.new_bool_var();
                            let bus_options: Vec<BusOption> = kept.iter()
                                .map(|b| BusOption {
                                    travel_time_departure_minute: b.travel_time_departure_minute,
                                    travel_mins: b.travel_mins,
                                    chosen: model.new_bool_var(),
                                })
                                .collect();
                            let chosen = bus_options.iter()
                                .fold(LinearExpr::from(0), |acc, o| acc + o.chosen);
                            model.add_eq(chosen, LinearExpr::from(bus));
                            bus_options.iter().for_each(|o| {
                                let after = model.add_ge(LinearExpr::from(depart_at),
                                    o.travel_time_departure_minute);
                                model.only_enforce_if(after, [o.chosen]);
                                let within = model.add_le(LinearExpr::from(depart_at),
                                    o.travel_time_departure_minute + 14);
                                model.only_enforce_if(within, [o.chosen]);
                                let timed = model.add_eq(LinearExpr::from(travel), o.travel_mins);
                                model.only_enforce_if(timed, [o.chosen]);
                            });
                            car.iter().for_each(|&car_t| {
                                alternatives.iter().for_each(|&alt| {
                                    let by_car = model.add_eq(LinearExpr::from(travel), car_t);
                                    model.only_enforce_if(by_car, [alt]);
                                    objective += LinearExpr::from(alt)
                                        * (car_t * weight_penalty_per_minute_travel);
                                });
                            });
                            let conveyances: Vec<BoolVar> = std::iter::once(bus)
                                .chain(alternatives.iter().copied())
                                .collect();
                            let one_of = model.add_or(conveyances.clone());
                            model.only_enforce_if(one_of, [seq]);
                            let load = conveyances.iter()
                                .fold(LinearExpr::from(0), |acc, &v| acc + v);
                            model.add_le(load, 1);
                            let waits = model.add_ge(LinearExpr::from(depart_at), depart);
                            model.only_enforce_if(waits, [seq]);
                            let reaches = model.add_ge(arrival, LinearExpr::from(depart_at) + travel);
                            model.only_enforce_if(reaches, [seq]);
                            bus_options.iter().for_each(|o| {
                                objective += LinearExpr::from(o.chosen)
                                    * (o.travel_mins * weight_penalty_per_minute_travel);
                            });
                            Some(travel)
                        }
                        None => {
                            match (own, car) {
                                (None, None) => { model.add_eq(seq * 1, 0); }
                                (Some(own_t), None) => {
                                    let c = model.add_ge(arrival, depart + own_t);
                                    model.only_enforce_if(c, [seq]);
                                }
                                (own_t, Some(car_t)) => {
                                    alternatives.iter().for_each(|&alt| {
                                        let by_car = model.add_ge(arrival.clone(), depart.clone() + car_t);
                                        model.only_enforce_if(by_car, [alt]);
                                    });
                                    match own_t {
                                        Some(own_t) => {
                                            let slack = (own_t - car_t).max(0);
                                            let relaxed = alternatives.iter()
                                                .fold(LinearExpr::from(0), |acc, alt| acc + *alt) * slack;
                                            let by_own = model.add_ge(arrival,
                                                depart + own_t + relaxed * -1);
                                            model.only_enforce_if(by_own, [seq]);
                                        }
                                        None => {
                                            match alternatives.is_empty() {
                                                true => { model.add_eq(seq * 1, 0); }
                                                false => {
                                                    let one_of = model.add_or(alternatives.clone());
                                                    model.only_enforce_if(one_of, [seq]);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            match alternatives.is_empty() {
                                true => {}
                                false => {
                                    let conveyances = alternatives.iter()
                                        .fold(LinearExpr::from(0), |acc, alt| acc + *alt);
                                    model.add_le(conveyances, 1);
                                }
                            }
                            match (own, car) {
                                (Some(own_t), Some(car_t)) => {
                                    objective += LinearExpr::from(seq) * (own_t * weight_penalty_per_minute_travel);
                                    let discount = (own_t - car_t).max(0) * weight_penalty_per_minute_travel;
                                    alternatives.iter().for_each(|alt| {
                                        objective += LinearExpr::from(*alt) * -discount;
                                    });
                                }
                                (Some(own_t), None) => {
                                    objective += LinearExpr::from(seq) * (own_t * weight_penalty_per_minute_travel);
                                }
                                (None, Some(car_t)) => {
                                    objective += LinearExpr::from(seq) * (car_t * weight_penalty_per_minute_travel);
                                }
                                (None, None) => {}
                            }
                            None
                        }
                    };
                    taxi.iter().for_each(|t| {
                        objective += LinearExpr::from(*t) * weight_penalty_per_taxi_journey;
                    });
                    next_shifts.insert(
                        NextShift { staff: wi, from_shift: first, to_shift: second },
                        JourneyOptions {
                            taken: seq,
                            rides,
                            own_travel_mins: own,
                            car_travel_mins: car,
                            bus_travel_mins: travel,
                        });
                });
            });
    });


    staffs.iter().enumerate().for_each(|(wi, w)| {
        let break_required_after_mins = w.break_required_after_mins(&break_rules);
        let break_mins = w.break_mins(&break_rules);
        span_dates.iter().for_each(|date| {
            let day_shifts: Vec<usize> = shifts.iter().enumerate()
                .filter(|(vi, v)| v.on_date == *date
                    && !blacklist.contains(&StaffShift { staff: wi, shift: *vi }))
                .map(|(vi, _)| vi)
                .collect();
            let pos: Vec<IntVar> = day_shifts.iter()
                .map(|_| model.new_int_var([(0, day_shifts.len() as i64 - 1)]))
                .collect();
            let stretch_start: Vec<IntVar> = day_shifts.iter()
                .map(|_| model.new_int_var([(0, 24 * 60)]))
                .collect();
            let firsts: Vec<BoolVar> = day_shifts.iter()
                .map(|&vi| {
                    let is_first = model.new_bool_var();
                    let incoming = day_shifts.iter()
                        .filter(|&&u| u != vi)
                        .filter_map(|&u| next_shifts
                            .get(&NextShift { staff: wi, from_shift: u, to_shift: vi })
                            .map(|lv| lv.taken))
                        .fold(LinearExpr::from(is_first), |acc, s| acc + s);
                    model.add_eq(incoming, LinearExpr::from(assign[wi][vi]));
                    let outgoing = day_shifts.iter()
                        .filter(|&&u| u != vi)
                        .filter_map(|&u| next_shifts
                            .get(&NextShift { staff: wi, from_shift: vi, to_shift: u })
                            .map(|lv| lv.taken))
                        .fold(LinearExpr::from(0), |acc, s| acc + s);
                    model.add_le(outgoing, LinearExpr::from(assign[wi][vi]));
                    let hours = &availability[&StaffDay { staff_id: w.id, date: *date }];
                    let home = w.postcode_id.as_str();
                    let site = shifts[vi].postcode_id();
                    let same_site = home == site;
                    let own = match same_site {
                        true => Some(0),
                        false => lookup(w.transport_mode_id(), home, site),
                    };
                    let car = match same_site {
                        true => None,
                        false => lookup("car", home, site),
                    };
                    let arrival = LinearExpr::from(start[vi]) + late[wi][vi];
                    let lo = hours.iter().map(|h| h.from).min().expect("availability window");
                    let hi = shifts[vi].permissioned_to() + shifts[vi].max_late_mins();
                    let profile: Option<Vec<BusTime>> = match (w.transport_mode_id(), same_site) {
                        ("transit", false) => Some(transit_profiles
                            .range(BusDeparture {
                                    from_postcode_id: home.to_string(),
                                    to_postcode_id: site.to_string(),
                                    date: *date,
                                    travel_time_departure_minute: 0,
                                }
                                ..=BusDeparture {
                                    from_postcode_id: home.to_string(),
                                    to_postcode_id: site.to_string(),
                                    date: *date,
                                    travel_time_departure_minute: 1439,
                                })
                            .map(|(k, &t)| BusTime {
                                travel_time_departure_minute: k.travel_time_departure_minute,
                                travel_mins: t,
                            })
                            .filter(|b| b.travel_time_departure_minute + 14 >= lo
                                && b.travel_time_departure_minute <= hi)
                            .collect::<Vec<_>>())
                            .filter(|kept| !kept.is_empty()),
                        _ => None,
                    };
                    let taxi_ok = matches!(w.transport_mode_id(), "transit" | "bicycle" | "walk");
                    let taxi = match profile.is_some() {
                        true => (taxi_ok && car.is_some()).then(|| model.new_bool_var()),
                        false => (taxi_ok && car.is_some() && own != car)
                            .then(|| model.new_bool_var()),
                    };
                    taxi.iter().for_each(|&t| { model.add_implication(t, is_first); });
                    let alternatives: Vec<BoolVar> = taxi.iter().copied().collect();
                    match &profile {
                        Some(kept) => {
                            let depart_home = model.new_int_var([(lo.max(0), hi + 14)]);
                            let travel = model.new_int_var([(0, 24 * 60)]);
                            let bus = model.new_bool_var();
                            let bus_options: Vec<BusOption> = kept.iter()
                                .map(|b| BusOption {
                                    travel_time_departure_minute: b.travel_time_departure_minute,
                                    travel_mins: b.travel_mins,
                                    chosen: model.new_bool_var(),
                                })
                                .collect();
                            let chosen = bus_options.iter()
                                .fold(LinearExpr::from(0), |acc, o| acc + o.chosen);
                            model.add_eq(chosen, LinearExpr::from(bus));
                            bus_options.iter().for_each(|o| {
                                let after = model.add_ge(LinearExpr::from(depart_home),
                                    o.travel_time_departure_minute);
                                model.only_enforce_if(after, [o.chosen]);
                                let within = model.add_le(LinearExpr::from(depart_home),
                                    o.travel_time_departure_minute + 14);
                                model.only_enforce_if(within, [o.chosen]);
                                let timed = model.add_eq(LinearExpr::from(travel), o.travel_mins);
                                model.only_enforce_if(timed, [o.chosen]);
                            });
                            car.iter().for_each(|&car_t| {
                                alternatives.iter().for_each(|&alt| {
                                    let by_car = model.add_eq(LinearExpr::from(travel), car_t);
                                    model.only_enforce_if(by_car, [alt]);
                                    objective += LinearExpr::from(alt)
                                        * (car_t * weight_penalty_per_minute_travel);
                                });
                            });
                            let conveyances: Vec<BoolVar> = std::iter::once(bus)
                                .chain(alternatives.iter().copied())
                                .collect();
                            let one_of = model.add_or(conveyances.clone());
                            model.only_enforce_if(one_of, [is_first]);
                            let load = conveyances.iter()
                                .fold(LinearExpr::from(0), |acc, &c| acc + c);
                            model.add_le(load, 1);
                            let reaches = model.add_ge(arrival.clone(),
                                LinearExpr::from(depart_home) + travel);
                            model.only_enforce_if(reaches, [is_first]);
                            bus_options.iter().for_each(|o| {
                                objective += LinearExpr::from(o.chosen)
                                    * (o.travel_mins * weight_penalty_per_minute_travel);
                            });
                        }
                        None => {
                            match (own, car) {
                                (None, None) => { model.add_eq(is_first * 1, 0); }
                                (Some(own_t), None) => {
                                    let c = model.add_ge(arrival.clone(), lo + own_t);
                                    model.only_enforce_if(c, [is_first]);
                                }
                                (own_t, Some(car_t)) => {
                                    alternatives.iter().for_each(|&alt| {
                                        let by_car = model.add_ge(arrival.clone(), lo + car_t);
                                        model.only_enforce_if(by_car, [alt]);
                                    });
                                    match own_t {
                                        Some(own_t) => {
                                            let slack = (own_t - car_t).max(0);
                                            let relaxed = alternatives.iter()
                                                .fold(LinearExpr::from(0), |acc, alt| acc + *alt)
                                                * slack;
                                            let by_own = model.add_ge(arrival.clone(),
                                                relaxed * -1 + lo + own_t);
                                            model.only_enforce_if(by_own, [is_first]);
                                        }
                                        None => {
                                            match alternatives.is_empty() {
                                                true => { model.add_eq(is_first * 1, 0); }
                                                false => {
                                                    let one_of = model.add_or(alternatives.clone());
                                                    model.only_enforce_if(one_of, [is_first]);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            match alternatives.is_empty() {
                                true => {}
                                false => {
                                    let load = alternatives.iter()
                                        .fold(LinearExpr::from(0), |acc, alt| acc + *alt);
                                    model.add_le(load, 1);
                                }
                            }
                            match (own, car) {
                                (Some(own_t), Some(car_t)) => {
                                    objective += LinearExpr::from(is_first)
                                        * (own_t * weight_penalty_per_minute_travel);
                                    let discount = (own_t - car_t).max(0)
                                        * weight_penalty_per_minute_travel;
                                    alternatives.iter().for_each(|alt| {
                                        objective += LinearExpr::from(*alt) * -discount;
                                    });
                                }
                                (Some(own_t), None) => {
                                    objective += LinearExpr::from(is_first)
                                        * (own_t * weight_penalty_per_minute_travel);
                                }
                                (None, Some(car_t)) => {
                                    objective += LinearExpr::from(is_first)
                                        * (car_t * weight_penalty_per_minute_travel);
                                }
                                (None, None) => {}
                            }
                        }
                    }
                    taxi.iter().for_each(|t| {
                        objective += LinearExpr::from(*t) * weight_penalty_per_taxi_journey;
                    });
                    is_first
                })
                .collect();
            match firsts.is_empty() {
                true => {}
                false => {
                    let first_count = firsts.iter().fold(LinearExpr::from(0), |acc, f| acc + *f);
                    model.add_le(first_count, 1);
                }
            }
            match break_required_after_mins > 0 && break_mins > 0 {
                false => {}
                true => day_shifts.iter().enumerate().for_each(|(i, &vi)| {
                    let anchored = model.add_eq(LinearExpr::from(stretch_start[i]),
                        LinearExpr::from(start[vi]));
                    model.only_enforce_if(anchored, [firsts[i]]);
                }),
            }
            day_shifts.iter().enumerate().for_each(|(ai, &a)| day_shifts.iter().enumerate()
                .filter(|&(bi, _)| bi != ai)
                .for_each(|(bi, &b)| match next_shifts.get(&NextShift { staff: wi, from_shift: a, to_shift: b }) {
                    None => {}
                    Some(lv) => {
                        let onward = model.add_ge(LinearExpr::from(pos[bi]),
                            LinearExpr::from(pos[ai]) + 1);
                        model.only_enforce_if(onward, [lv.taken]);
                        match break_required_after_mins > 0
                            && break_mins > 0 {
                            false => {}
                            true => {
                                let travel_mins = match lv.bus_travel_mins {
                                    Some(tv) => LinearExpr::from(tv),
                                    None => LinearExpr::from(
                                        lv.own_travel_mins.or(lv.car_travel_mins).unwrap_or(0)),
                                };
                                let rested = model.new_bool_var();
                                let working = model.new_bool_var();
                                model.add_or([rested, working]);
                                let resting = model.add_ge(LinearExpr::from(start[b]),
                                    LinearExpr::from(start[a]) + shifts[a].duration_mins()
                                        + travel_mins.clone() + break_mins);
                                model.only_enforce_if(resting, [lv.taken, rested]);
                                let still_working = model.add_le(LinearExpr::from(start[b]),
                                    LinearExpr::from(start[a]) + shifts[a].duration_mins()
                                        + travel_mins + break_mins - 1);
                                model.only_enforce_if(still_working, [lv.taken, working]);
                                let fresh = model.add_eq(LinearExpr::from(stretch_start[bi]),
                                    LinearExpr::from(start[b]));
                                model.only_enforce_if(fresh, [lv.taken, rested]);
                                let carried = model.add_eq(LinearExpr::from(stretch_start[bi]),
                                    LinearExpr::from(stretch_start[ai]));
                                model.only_enforce_if(carried, [lv.taken, working]);
                                let capped = model.add_le(
                                    LinearExpr::from(start[b]) + shifts[b].duration_mins(),
                                    LinearExpr::from(stretch_start[ai])
                                        + break_required_after_mins);
                                model.only_enforce_if(capped, [lv.taken, working]);
                            }
                        }
                    }
                }));
        });
    });

    let ride_direction_links: Vec<RideLink> = next_shifts.iter()
        .flat_map(|(key, lv)| lv.rides.iter()
            .map(|r| RideLink {
                ride: r.chosen,
                driver_step: next_shifts
                    .get(&NextShift { staff: r.driver, from_shift: key.from_shift, to_shift: key.to_shift })
                    .map(|dlv| dlv.taken),
            })
            .collect::<Vec<_>>())
        .collect();
    ride_direction_links.iter().for_each(|l| match l.driver_step {
        Some(dseq) => { model.add_implication(l.ride, dseq); }
        None => { model.add_eq(l.ride * 1, 0); }
    });

    driver_indexes.iter().for_each(|&di| {
        (0..nv)
            .flat_map(|a| (0..nv).map(move |b| (a, b)))
            .filter(|&(a, b)| a != b && shifts[a].on_date == shifts[b].on_date)
            .for_each(|(a, b)| {
                let passengers: Vec<BoolVar> = next_shifts.iter()
                    .filter(|(key, lv)| key.from_shift == a && key.to_shift == b
                        && lv.rides.iter().any(|r| r.driver == di))
                    .flat_map(|(_, lv)| lv.rides.iter()
                        .filter(|r| r.driver == di)
                        .map(|r| r.chosen))
                    .collect();
                match passengers.is_empty() {
                    true => {}
                    false => {
                        let load = passengers.iter().fold(LinearExpr::from(0), |acc, p| acc + *p);
                        model.add_le(load, staffs[di].passengers(&passenger_rules));
                    }
                }
            });
    });

    staffs.iter().enumerate().for_each(|(wi, w)| {
        let break_required_after_mins = w.break_required_after_mins(&break_rules);
        let break_mins = w.break_mins(&break_rules);
        match break_required_after_mins > 0 && break_mins > 0 {
            false => {}
            true => span_dates.iter().for_each(|date| {
                let day_shifts: Vec<usize> = shifts.iter().enumerate()
                    .filter(|(vi, v)| v.on_date == *date
                        && !blacklist.contains(&StaffShift { staff: wi, shift: *vi }))
                    .map(|(vi, _)| vi)
                    .collect();
                match day_shifts.len() < 2 {
                    true => {}
                    false => {
                        let care_minutes = day_shifts.iter()
                            .fold(LinearExpr::from(0), |acc, &vi| {
                                acc + assign[wi][vi] * shifts[vi].duration_mins()
                            });
                        let over = model.new_bool_var();
                        let under = model.new_bool_var();
                        model.add_or([over, under]);
                        let above = model.add_ge(care_minutes.clone(), break_required_after_mins + 1);
                        model.only_enforce_if(above, [over]);
                        let below = model.add_le(care_minutes, break_required_after_mins);
                        model.only_enforce_if(below, [under]);
                        let n_assigned = day_shifts.iter()
                            .fold(LinearExpr::from(0), |acc, &vi| acc + assign[wi][vi]);
                        let multi = model.new_bool_var();
                        let single = model.new_bool_var();
                        model.add_or([multi, single]);
                        let at_least_two = model.add_ge(n_assigned.clone(), 2);
                        model.only_enforce_if(at_least_two, [multi]);
                        let at_most_one = model.add_le(n_assigned, 1);
                        model.only_enforce_if(at_most_one, [single]);
                        let break_candidates: Vec<BoolVar> = day_shifts.iter()
                            .flat_map(|&a| day_shifts.iter().filter(move |&&b| b != a).map(move |&b| (a, b)))
                            .filter(|&(a, b)| shifts[b].latest_start()
                                - (shifts[a].earliest_start() + shifts[a].duration_mins())
                                >= break_mins)
                            .filter_map(|(a, b)| next_shifts
                                .get(&NextShift { staff: wi, from_shift: a, to_shift: b })
                                .map(|lv| BreakCandidate {
                                    from_shift: a,
                                    to_shift: b,
                                    taken: lv.taken,
                                    bus_travel_mins: lv.bus_travel_mins,
                                    own_travel_mins: lv.own_travel_mins,
                                    car_travel_mins: lv.car_travel_mins,
                                }))
                            .map(|c| {
                                let brk = model.new_bool_var();
                                model.add_implication(brk, c.taken);
                                let arrival = LinearExpr::from(start[c.to_shift]);
                                let rested = match c.bus_travel_mins {
                                    Some(tv) => LinearExpr::from(start[c.from_shift])
                                        + shifts[c.from_shift].duration_mins()
                                        + LinearExpr::from(tv) + break_mins,
                                    None => LinearExpr::from(start[c.from_shift])
                                        + shifts[c.from_shift].duration_mins()
                                        + c.own_travel_mins.or(c.car_travel_mins).unwrap_or(0)
                                        + break_mins,
                                };
                                let gap = model.add_ge(arrival, rested);
                                model.only_enforce_if(gap, [brk]);
                                brk
                            })
                            .collect();
                        match break_candidates.is_empty() {
                            true => { model.add_or([under, single]); }
                            false => {
                                let rest = model.add_or(break_candidates);
                                model.only_enforce_if(rest, [over, multi]);
                            }
                        }
                    }
                }
            }),
        }
    });

    objective = shifts.iter().enumerate().fold(objective, |obj, (vi, v)| {
        let stretched = model.new_int_var([(0,
            (v.earliest_start() - v.permissioned_from()).max(v.permissioned_to() - v.latest_start()))]);
        model.add_ge(LinearExpr::from(stretched),
            LinearExpr::from(v.earliest_start()) + LinearExpr::from(start[vi]) * -1);
        model.add_ge(LinearExpr::from(stretched),
            LinearExpr::from(start[vi]) + (-v.latest_start()));
        let obj = obj + LinearExpr::from(stretched) * weight_penalty_per_minute_permissioned_stretch;
        let uncovered_price = match v.critical_visit_rule(&critical_visit_rules) {
            true => (weight_penalty_uncovered_visit as f64
                * weight_penalty_uncovered_high_priority_multiplier) as i64,
            false => weight_penalty_uncovered_visit,
        };
        let with_core = staffs.iter().enumerate().fold(
            obj + LinearExpr::from(uncovered[vi]) * uncovered_price,
            |acc, (wi, _)| acc + LinearExpr::from(late[wi][vi]) * weight_penalty_per_minute_late,
        );
        let with_partial = match v.carers_required(&carer_rules) >= 2 {
            false => with_core,
            true => {
                let partial = model.new_bool_var();
                let lower = model.add_ge(LinearExpr::from(uncovered[vi]), 1);
                model.only_enforce_if(lower, [partial]);
                let upper = model.add_le(LinearExpr::from(uncovered[vi]), v.carers_required(&carer_rules) - 1);
                model.only_enforce_if(upper, [partial]);
                let all_covered = model.new_bool_var();
                let zero = model.add_eq(LinearExpr::from(uncovered[vi]) * 1, 0);
                model.only_enforce_if(zero, [all_covered]);
                let all_missed = model.new_bool_var();
                let full = model.add_eq(LinearExpr::from(uncovered[vi]) * 1, v.carers_required(&carer_rules));
                model.only_enforce_if(full, [all_missed]);
                model.add_or([partial, all_covered, all_missed]);
                with_core + LinearExpr::from(partial) * weight_penalty_partial_double_up
            }
        };
        let with_ideal = match v.ideal_start() {
            None => with_partial,
            Some(ideal) => {
                let drift = model.new_int_var([(0,
                    (v.permissioned_to() - ideal).max(ideal - v.permissioned_from()))]);
                model.add_ge(LinearExpr::from(drift), LinearExpr::from(start[vi]) + (-ideal));
                model.add_ge(LinearExpr::from(drift), LinearExpr::from(ideal) + LinearExpr::from(start[vi]) * -1);
                with_partial + LinearExpr::from(drift) * weight_penalty_per_minute_start_moved
            }
        };
        let with_preference = match v.preference_id
            .filter(|&id| preferred_workers.iter().any(|j| j.preference_id == id)) {
            None => with_ideal,
            Some(id) => staffs.iter().enumerate()
                .filter(|(_, w)| !preferred_workers.iter()
                    .any(|j| j.preference_id == id && j.staff_id == w.id))
                .fold(with_ideal, |acc, (wi, _)| {
                    acc + LinearExpr::from(assign[wi][vi]) * weight_penalty_non_preferred_worker
                }),
        };
        staffs.iter().enumerate().fold(with_preference, |acc, (wi, _)| {
            acc + LinearExpr::from(early[wi][vi]) * weight_penalty_per_minute_early_leave
        })
    });

    model.minimize(objective);

    let params = match max_seconds <= 30 {
        false => SatParameters {
            max_time_in_seconds: Some(max_seconds as f64),
            num_workers: Some(weight_solver_workers),
            ..Default::default()
        },
        true => SatParameters {
            max_time_in_seconds: Some(max_seconds as f64),
            num_workers: Some(weight_solver_workers),
            cp_model_probing_level: Some(0),
            linearization_level: Some(0),
            symmetry_level: Some(0),
            ..Default::default()
        },
    };

    let response = model.solve_with_parameters(&params);
    let status = response.status();
    match status {
        CpSolverStatus::Optimal | CpSolverStatus::Feasible => (),
        other => return Err(format!("solver ended {other:?}").into()),
    }
    println!("status: {status:?}  objective: {}", response.objective_value);
    let placements: Vec<Placement> = shifts.iter().enumerate()
        .flat_map(|(vi, _)| staffs.iter().enumerate()
            .filter(|(wi, _)| assign[*wi][vi].solution_value(&response))
            .map(|(wi, _)| Placement {
                staff: wi,
                shift: vi,
                start_minute: start[vi].solution_value(&response),
                late_mins: late[wi][vi].solution_value(&response),
                left_early_mins: early[wi][vi].solution_value(&response),
            })
            .collect::<Vec<_>>())
        .collect();
    let mut ordered: Vec<usize> = (0..nv).collect();
    ordered.sort_by_key(|&vi| (shifts[vi].on_date, start[vi].solution_value(&response)));
    ordered.iter().for_each(|&vi| {
        let v = &shifts[vi];
        let names = placements.iter()
            .filter(|p| p.shift == vi)
            .map(|p| {
                let late_tag = match p.late_mins > 0 {
                    true => format!(" (+{}m late)", p.late_mins),
                    false => String::new(),
                };
                let early_tag = match p.left_early_mins > 0 {
                    true => format!(" (-{}m)", p.left_early_mins),
                    false => String::new(),
                };
                format!("{}{late_tag}{early_tag}", staffs[p.staff].name)
            })
            .collect::<Vec<_>>()
            .join(" + ");
        let u = uncovered[vi].solution_value(&response);
        println!(
            "{} {:<30} start {} {} {}",
            v.on_date, v.client_name,
            hhmm(start[vi].solution_value(&response)), names,
            match u > 0 { true => format!("UNCOVERED {u}"), false => String::new() },
        );
    });

    let now = chrono::Utc::now().naive_utc();
    let rows: Vec<mysql_async::Params> = placements.iter()
        .filter(|p| !pinned_care.contains(&PinnedCare {
            staff_id: staffs[p.staff].id,
            shift_id: shifts[p.shift].id,
        }))
        .map(|p| params! {
            "rota_id" => rota_id,
            "shift_id" => shifts[p.shift].id,
            "staff_id" => staffs[p.staff].id,
            "assignment_type_id" => "care",
            "start_time" => hhmm(p.start_minute).parse::<chrono::NaiveTime>().expect("time"),
            "end_time" => hhmm((p.start_minute + shifts[p.shift].duration_mins() - p.left_early_mins) % 1440).parse::<chrono::NaiveTime>().expect("time"),
            "note" => format!("solved, objective {}", response.objective_value),
            "user_id" => "@femi:femi.market",
            "created_at" => now,
        })
        .collect();
    let written = rows.len();
    match rows.is_empty() {
        true => {}
        false => { conn.exec_batch(include_str!("assignment.sql"), rows).await?; }
    }

    conn.exec_drop(
        include_str!("wall_seconds.sql"),
        params! { "wall_seconds" => started.elapsed().as_secs_f64(), "id" => rota.id },
    )
    .await?;
    drop(conn);
    pool.disconnect().await?;

    println!("--> {written} care assignments written, wall {:.1}s", started.elapsed().as_secs_f64());
    Ok(())
}
