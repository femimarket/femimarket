//! The workers a blacklist names - never sent to this client while it stands.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "blacklisted_workers")]
pub struct BlacklistedWorker {
    pub blacklist_id: i32,
    pub staff_id: i32,
}
