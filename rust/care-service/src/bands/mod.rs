//! Call bands (morning / lunch / tea / evening / nights): default start flex
//! and lateness tolerance for pattern rows that set none of their own.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "bands")]
pub struct Band {
    pub id: i32,
    pub name: String,
    pub from_time: chrono::NaiveTime,
    pub to_time: chrono::NaiveTime,
    pub flex_before_mins: i32,
    pub flex_after_mins: i32,
    pub max_late_mins: i32,
    pub note: String,
}
