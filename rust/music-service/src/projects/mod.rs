use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "projects")]
pub struct Project {
    pub id: i32,
    pub user_id: i32,
    pub name: String,
}
