use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "lines")]
pub struct Line {
    pub id: i32,
    pub composition_id: i32,
    pub sort: String,
    pub text: String,
    pub start_ms: f64,
    pub context: Option<String>,
    pub goal: Option<String>,
}
