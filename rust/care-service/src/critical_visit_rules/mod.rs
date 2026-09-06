//! Clients whose visits are critical: the existence of an active row IS the
//! rule - missing this client's call is a life-safety incident, never an
//! acceptable sacrifice on a short-staffed day. The solver gives these
//! clients the fatal-tier miss penalty. Dated like all rules: begun with a
//! reason, ended early at most once with a reason.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "critical_visit_rules")]
pub struct CriticalVisitRule {
    pub id: i32,
    pub client_id: i32,
    pub from_date: chrono::NaiveDate,
    pub to_date: chrono::NaiveDate,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    /// Why it ended early; null = ran its course. Settable exactly once.
    pub updated_note: Option<String>,
    pub updated_at: Option<chrono::NaiveDateTime>,
}
