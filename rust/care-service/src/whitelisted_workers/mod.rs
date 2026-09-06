//! The workers a whitelist names - the only carers allowed while it stands.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "whitelisted_workers")]
pub struct WhitelistedWorker {
    pub whitelist_id: i32,
    pub staff_id: i32,
}
