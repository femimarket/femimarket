use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "alignments")]
pub struct Alignment {
    pub id: i32,
    pub audio_id: i32,
    pub text: String,
    pub start: f64,
    pub end: f64,
}
