use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "themes")]
pub struct Theme {
    pub id: i32,
    pub line_id: i32,
    pub theme: String,
    // the base of ideas grown from the theme, then the scene written from it
    pub expand: Option<String>,
    pub scene: Option<String>,
}
