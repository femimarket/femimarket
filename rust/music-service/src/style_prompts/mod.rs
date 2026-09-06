use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "style_prompts")]
pub struct StylePrompt {
    pub id: i32,
    pub song_id: i32,
    pub text: String,
}
