//! Two shifts one carer: whoever is assigned shift1 is assigned shift2.
//! Pinned to concrete shifts, one row per pair - Julie F's waking night
//! and her next-morning call.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "back_to_back_rules")]
pub struct BackToBackRule {
    pub id: i32,
    pub shift1_id: i32,
    pub shift2_id: i32,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
