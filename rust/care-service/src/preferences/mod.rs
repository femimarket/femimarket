//! A client's preference over a span of days - one assertion, which may name
//! several workers (preferred_workers junction), knowing nothing of shifts.
//! Soft guidance: the solver sends the named workers when possible. Exactly
//! two moments in its life, both reasoned: created once, ended early at most
//! once - setting updated_* IS the un-preferring, then the record is closed.
//! to_date keeps the original promise, so intention and reality both show.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "preferences")]
pub struct Preference {
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
