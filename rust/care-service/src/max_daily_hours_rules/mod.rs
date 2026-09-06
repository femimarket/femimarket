use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "max_daily_hours_rules")]
pub struct MaxDailyHoursRule {
    pub id: i32,
    pub staff_id: i32,
    pub max_minutes: i32,
    pub from_date: chrono::NaiveDate,
    pub to_date: chrono::NaiveDate,
    pub note: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    pub updated_note: Option<String>,
    pub updated_at: Option<chrono::NaiveDateTime>,
}
