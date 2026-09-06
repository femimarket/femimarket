//! Working hours declared over spans - one table, one question answered
//! ("is this carer available at this time on this date?"). Off is the
//! absence of a live declaration, never a row of its own.
//! Append-only; a record may be terminated at most once, with a reason -
//! setting updated_* IS the termination, after which the record is closed.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "availabilities")]
pub struct Availability {
    pub id: i32,
    pub staff_id: i32,
    pub from_date: chrono::NaiveDate,
    pub to_date: chrono::NaiveDate,
    pub start_time: chrono::NaiveTime,
    pub end_time: chrono::NaiveTime,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    /// Why it was terminated early; null = ran its course. Settable once.
    pub updated_note: Option<String>,
    pub updated_at: Option<chrono::NaiveDateTime>,
    pub approved_note: Option<String>,
    pub approved: Option<chrono::NaiveDateTime>,
}
