pub mod get;
pub mod route;

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "users")]
pub struct User {
    pub id: i32,
    // "@name:server" — the identity; auth itself stays in matrix
    pub matrix_user_id: String,
}
