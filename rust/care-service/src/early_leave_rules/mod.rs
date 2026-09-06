//! A client-agreed permission for one carer to leave a shift early, up to
//! the given minutes. Pinned to the shift: one row per carer allowed to
//! go - a double-up with one row keeps its second carer to the end.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "early_leave_rules")]
pub struct EarlyLeaveRule {
    pub id: i32,
    pub shift_id: i32,
    pub max_early_leave_mins: i32,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
