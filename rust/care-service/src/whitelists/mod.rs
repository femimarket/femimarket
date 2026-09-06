//! A hard whitelist over a span of days: while active, ONLY the workers it
//! names (whitelisted_workers junction) may serve this client. A wall, not
//! guidance - the solver may never staff outside it. Append-only, ended
//! early at most once with a reason.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "whitelists")]
pub struct Whitelist {
    pub id: i32,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    /// Why it ended early; null = ran its course. Settable exactly once.
    pub updated_note: Option<String>,
    pub updated_at: Option<chrono::NaiveDateTime>,
}
