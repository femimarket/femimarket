use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "faqs")]
pub struct Faq {
    pub id: i32,
    pub audio_id: i32,
    pub question: String,
    pub answer: Option<String>,
}
