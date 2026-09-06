//! The foundation for all worker availabilities: the whole week's offer in
//! one row, asserted at once. A worker's current availability is their
//! latest row; earlier rows are history. False means not offered.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "weekly_hours")]
pub struct WeeklyHours {
    pub id: i32,
    pub user_id: Option<String>,
    pub monday_morning: bool,
    pub monday_lunch: bool,
    pub monday_tea: bool,
    pub monday_evening: bool,
    pub monday_night: bool,
    pub tuesday_morning: bool,
    pub tuesday_lunch: bool,
    pub tuesday_tea: bool,
    pub tuesday_evening: bool,
    pub tuesday_night: bool,
    pub wednesday_morning: bool,
    pub wednesday_lunch: bool,
    pub wednesday_tea: bool,
    pub wednesday_evening: bool,
    pub wednesday_night: bool,
    pub thursday_morning: bool,
    pub thursday_lunch: bool,
    pub thursday_tea: bool,
    pub thursday_evening: bool,
    pub thursday_night: bool,
    pub friday_morning: bool,
    pub friday_lunch: bool,
    pub friday_tea: bool,
    pub friday_evening: bool,
    pub friday_night: bool,
    pub saturday_morning: bool,
    pub saturday_lunch: bool,
    pub saturday_tea: bool,
    pub saturday_evening: bool,
    pub saturday_night: bool,
    pub sunday_morning: bool,
    pub sunday_lunch: bool,
    pub sunday_tea: bool,
    pub sunday_evening: bool,
    pub sunday_night: bool,
    pub note: String,
    pub created_at: chrono::NaiveDateTime,
}
