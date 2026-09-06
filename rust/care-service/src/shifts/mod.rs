//! The running list of shifts: every shift that exists or will exist is a
//! real, dated row - never a blueprint standing in for future shifts.
//!
//! A shift holds no opinions of its own about time, length or staffing: it
//! is a dated instantiation that points at the rules governing it. Changing
//! anything means appending a new rule (note mandatory) and repointing.

pub mod get;
pub mod route;

use chrono::Timelike;
use mysql_async::prelude::{FromRow, FromValue};

#[derive(Clone, Debug, PartialEq, Eq, FromValue, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", is_string)]
pub enum ShiftType {
    Blueprint,
    Assigned,
}

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "shifts")]
pub struct Shift {
    pub id: i32,
    pub client_id: i32,
    pub on_date: chrono::NaiveDate,
    /// When the call may begin and how long it runs.
    pub time_rule_id: i32,
    /// The preference governing this call, like its other rules.
    pub whitelist_id: Option<i32>,
    pub blacklist_id: Option<i32>,
    pub preference_id: Option<i32>,
    /// Rows of one double-up share the leader row's id here; the leader
    /// points at itself. Null = a single.
    pub double_up_id: Option<i32>,
    /// Roundsys shift pk, while that system is still pushed to.
    pub roundsys_pk: Option<String>,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    /// Cancellation is the one sanctioned update. Null = live; set = the
    /// why and when of the cancellation, in fields that say so themselves.
    pub cancelled_note: Option<String>,
    pub cancelled_at: Option<chrono::NaiveDateTime>,
}

/// A shift joined with the time rule it points at and the client it serves:
/// one row from any query that JOINs care.shifts, care.time_rules and
/// care.clients on their ids.
#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async")]
pub struct ShiftTimeRuleClient {
    pub id: i32,
    pub client_id: i32,
    pub on_date: chrono::NaiveDate,
    pub time_rule_id: i32,
    pub whitelist_id: Option<i32>,
    pub blacklist_id: Option<i32>,
    pub preference_id: Option<i32>,
    pub double_up_id: Option<i32>,
    pub roundsys_pk: Option<String>,
    pub note: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    pub cancelled_note: Option<String>,
    pub cancelled_at: Option<chrono::NaiveDateTime>,
    pub earliest_start: chrono::NaiveTime,
    pub latest_start: chrono::NaiveTime,
    pub ideal_start: Option<chrono::NaiveTime>,
    pub permissioned_from: Option<chrono::NaiveTime>,
    pub permissioned_to: Option<chrono::NaiveTime>,
    pub duration_mins: i32,
    pub max_late_mins: i32,
    pub call_type: super::time_rules::CallType,
    pub client_name: String,
    pub client_postcode_id: String,
}

fn minutes(t: chrono::NaiveTime) -> i64 {
    t.hour() as i64 * 60 + t.minute() as i64
}

impl ShiftTimeRuleClient {
    pub fn earliest_start(&self) -> i64 {
        minutes(self.earliest_start)
    }

    pub fn permissioned_from(&self) -> i64 {
        match self.permissioned_from {
            Some(t) => minutes(t),
            None => self.earliest_start(),
        }
    }

    pub fn permissioned_to(&self) -> i64 {
        match self.permissioned_to {
            Some(t) => minutes(t),
            None => self.latest_start(),
        }
    }

    pub fn latest_start(&self) -> i64 {
        minutes(self.latest_start)
    }

    pub fn ideal_start(&self) -> Option<i64> {
        self.ideal_start.map(minutes)
    }

    pub fn duration_mins(&self) -> i64 {
        self.duration_mins as i64
    }

    pub fn max_late_mins(&self) -> i64 {
        self.max_late_mins as i64
    }

    pub fn carers_required(&self, carer_rules: &[super::carer_rules::CarerRule]) -> i64 {
        carer_rules.iter()
            .filter(|r| r.shift_id == self.id && r.cancelled_at.is_none())
            .count() as i64
    }

    pub fn postcode_id(&self) -> &str {
        &self.client_postcode_id
    }

    pub fn critical_visit_rule(&self, rules: &[super::critical_visit_rules::CriticalVisitRule]) -> bool {
        rules.iter().any(|r| r.client_id == self.client_id
            && r.from_date <= self.on_date && self.on_date <= r.to_date)
    }

    pub fn carers_may_leave(&self, early_leave_rules: &[super::early_leave_rules::EarlyLeaveRule]) -> i64 {
        early_leave_rules.iter()
            .filter(|r| r.shift_id == self.id)
            .count() as i64
    }

    pub fn max_early_leave_mins(&self, early_leave_rules: &[super::early_leave_rules::EarlyLeaveRule]) -> i64 {
        early_leave_rules.iter()
            .filter(|r| r.shift_id == self.id)
            .map(|r| r.max_early_leave_mins as i64)
            .max()
            .unwrap_or(0)
    }
}
