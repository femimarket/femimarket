//! Two carers who may never share a call: while a row spans the solve date,
//! the named pair is mutually exclusive on any shared (2-carer) shift.
//! Symmetric - one row bans the pair both ways round. Dated like all rules:
//! begun with a reason, ended early at most once with a reason.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "banned_double_ups")]
pub struct BannedDoubleUp {
    pub id: i32,
    pub staff_id: i32,
    /// The other half of the banned pair.
    pub partner_id: i32,
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
