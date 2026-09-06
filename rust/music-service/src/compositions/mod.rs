use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "compositions")]
pub struct Composition {
    pub id: i32,
    pub song_id: i32,
    pub name: String,
}
