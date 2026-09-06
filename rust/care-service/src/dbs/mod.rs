//! One row per candidate's DBS position: an existing certificate, or the
//! application we make for them. ref/ref_time is the Wise payment reference
//! the app flow generates; paid_* is the office stamp when the transfer lands.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "dbs")]
pub struct Dbs {
    pub id: i32,
    pub user_id: String,
    pub certificate_number: Option<String>,
    pub update_service: bool,
    #[mysql(rename = "ref")]
    pub r#ref: Option<String>,
    pub ref_time: Option<chrono::NaiveDateTime>,
    pub paid_note: Option<String>,
    pub paid_at: Option<chrono::NaiveDateTime>,
    pub created_at: chrono::NaiveDateTime,
}
