//! The application's progress log, office-asserted: submitted, delayed,
//! cleared - each row feeds the candidate's app state, and completion is
//! the row whose status says so.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "dbs_update")]
pub struct DbsUpdate {
    pub id: i32,
    pub dbs_id: i32,
    pub status: String,
    pub note: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
