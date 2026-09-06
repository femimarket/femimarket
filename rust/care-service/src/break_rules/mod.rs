//! How a carer's breaks work: how long the break must be (break_mins) and
//! how much work may pass before it is due (break_required_after_mins).
//! Dated like all staff rules. A routable carer without an active row fails
//! the solve loudly - nothing is assumed. A value of 0 means the carer
//! needs no breaks.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "break_rules")]
pub struct BreakRule {
    pub id: i32,
    pub staff_id: i32,
    pub break_mins: i32,
    pub break_required_after_mins: i32,
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
