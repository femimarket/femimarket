//! Who may supervise, per supervision row - a proper junction, so the list
//! of supervisors is never a name-list squashed into a text column.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "supervisors")]
pub struct Supervisor {
    pub supervision_id: i32,
    pub supervisor_staff_id: i32,
}
