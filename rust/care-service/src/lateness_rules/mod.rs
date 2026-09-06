//! How late a carer may arrive at this client's calls, as a dated rule:
//! the appointment is set by time_rules; this governs arrival slippage
//! against whatever was set. 0 = never late. Append-only, ended early at
//! most once with a reason.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "lateness_rules")]
pub struct LatenessRule {
    pub id: i32,
    pub client_id: i32,
    pub max_late_mins: i32,
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
