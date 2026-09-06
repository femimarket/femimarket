//! Which sub-tasks a top-level activity claims.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "activity_links")]
pub struct ActivityLink {
    pub id: i32,
    pub activity_id: super::activities::Activity,
    pub sub_activity_id: super::activities::Activity,
    pub note: String,
    pub created_at: chrono::NaiveDateTime,
}
