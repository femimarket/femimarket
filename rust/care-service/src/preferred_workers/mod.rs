//! The workers a preference names - the junction, mirroring supervisors.
//! "Prefers Sunita and Blessing" = one preference row, two rows here.
//! Knows preferences and staff; knows nothing of shifts.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "preferred_workers")]
pub struct PreferredWorker {
    pub preference_id: i32,
    pub staff_id: i32,
}
