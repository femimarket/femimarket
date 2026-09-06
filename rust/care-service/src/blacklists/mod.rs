//! A hard blacklist over a span of days: while active, the workers it names
//! (blacklisted_workers junction) may NEVER serve this client. A wall, not
//! guidance. Append-only, ended early at most once with a reason - a ban
//! lifted is a dated, reasoned event like the ban itself.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "blacklists")]
pub struct Blacklist {
    pub id: i32,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    /// Why it ended early; null = ran its course. Settable exactly once.
    pub updated_note: Option<String>,
    pub updated_at: Option<chrono::NaiveDateTime>,
}
