//! How a client's day may be staffed as a whole, across every call they
//! receive: at most max_carers distinct faces per day, and at most
//! max_calls_per_carer of those calls held by any one carer.
//! Dated like all client rules; no row = no cap.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "daily_call_rules")]
pub struct DailyCallRule {
    pub id: i32,
    pub client_id: i32,
    pub max_carers: i32,
    pub max_calls_per_carer: i32,
    pub from_date: chrono::NaiveDate,
    pub to_date: chrono::NaiveDate,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
