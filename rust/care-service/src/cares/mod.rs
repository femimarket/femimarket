//! The kinds of care work a visit involves and a carer claims - doing-based,
//! claimed whole: you have done a care or you have not.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "cares")]
pub struct Care {
    pub id: String,
    pub note: String,
}
