//! A worker's care background: one row per worker, owned by a candidate
//! before hire or a staff member after - the "what have you done" anchor
//! that experience_cares rows hang off.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "experience")]
pub struct Experience {
    pub id: i32,
    pub user_id: Option<String>,
    pub staff_id: Option<i32>,
    pub note: String,
    pub created_at: chrono::NaiveDateTime,
}
