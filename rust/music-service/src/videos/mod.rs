use mysql_async::prelude::{FromRow, FromValue};

#[derive(Clone, Debug, PartialEq, Eq, FromValue, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", is_string, rename_all = "lowercase")]
pub enum VideoModel {
    Veo31,
    Unknown,
}

#[derive(Clone, Debug, PartialEq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "videos")]
pub struct Video {
    pub id: i32,
    pub line_id: i32,
    pub model: VideoModel,
    pub name: String,
    pub raw: String,
    pub prompt: Option<String>,
    // the clip's own trim, in its own time
    pub start_ms: f64,
    pub end_ms: f64,
    pub duration_ms: f64,
    // 100 = normal
    pub speed: i64,
    pub width: i32,
    pub height: i32,
    // one clip per line carries the final cut
    pub export: bool,
    pub selected: bool,
    pub sort: Option<i32>,
    // timeline placement, recomputed on load
    pub lane: i32,
    pub timeline_disabled: bool,
    pub timeline_duration_ms: f64,
    #[schema(value_type = String)]
    pub created_at: chrono::NaiveDateTime,
}
