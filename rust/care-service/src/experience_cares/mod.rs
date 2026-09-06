//! The "what have you done" picks: one row per care a worker has worked
//! with, hanging off their experience row.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "experience_cares")]
pub struct ExperienceCare {
    pub id: i32,
    pub experience_id: i32,
    pub care_id: String,
    pub note: String,
    pub created_at: chrono::NaiveDateTime,
}
