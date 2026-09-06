//! The temporal shape of a call, complete in one record: when it may begin
//! and how long it runs. Append-only - changing shape means a NEW rule
//! (note mandatory) and repointing the shifts. Old rules stay forever.

use mysql_async::prelude::{FromRow, FromValue};

#[derive(Clone, Debug, PartialEq, Eq, FromValue, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", is_string, rename_all = "snake_case")]
pub enum CallType {
    Breakfast30,
    Breakfast45,
    Breakfast60,
    Lunch30,
    Lunch45,
    Lunch60,
    Tea30,
    Tea45,
    Tea60,
    Bedtime30,
    Bedtime45,
    Bedtime60,
    LongHours,
    WakingNight,
    LiveIn,
}

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "time_rules")]
pub struct TimeRule {
    pub id: i32,
    pub client_id: i32,
    /// The span of allowed BEGINNINGS - a fixed call has from == until.
    pub earliest_start: chrono::NaiveTime,
    pub latest_start: chrono::NaiveTime,
    /// The preferred minute within the window, if one exists: the solver
    /// pays per minute of drift away from it. Absent = no preference, any
    /// minute of the window is equally fine. Must lie inside the window.
    pub ideal_start: Option<chrono::NaiveTime>,
    /// The outer window usable only with the client's permission (a phone
    /// call): starts between permissioned_from and earliest_start, or
    /// between latest_start and permissioned_to, are allowed but flagged
    /// so the client gets told. Absent = no permissioned stretch.
    pub permissioned_from: Option<chrono::NaiveTime>,
    pub permissioned_to: Option<chrono::NaiveTime>,
    pub duration_mins: i32,
    pub max_late_mins: i32,
    pub call_type: CallType,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
