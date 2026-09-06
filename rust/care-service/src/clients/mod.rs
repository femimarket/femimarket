//! Service users: permanent identity and standing care rules.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "clients")]
pub struct Client {
    pub id: i32,
    pub name: String,
    pub postcode_id: String,
    pub roundsys_pk: Option<String>,
}
