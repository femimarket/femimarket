//! The scratch regime: one row per candidate, born at their first funnel
//! answer and freely overwritten until the name step makes them serious.
//! Everything here is a lead's half-formed interest - aggregate value only,
//! no per-fact provenance, no supersession. The fixed vocabularies (the six
//! cares, the 7x5 work grid) are bool columns toggled by overwrite; false
//! means not picked yet.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "prospects")]
pub struct Prospect {
    pub id: i32,
    pub user_id: String,
    pub postcode: Option<String>,
    pub experience: Option<bool>,
    pub personal_care: bool,
    pub hoisting: bool,
    pub stoma: bool,
    pub peg: bool,
    pub catheter: bool,
    pub support: bool,
    pub mon_morning: bool,
    pub mon_lunch: bool,
    pub mon_tea: bool,
    pub mon_evening: bool,
    pub mon_night: bool,
    pub tue_morning: bool,
    pub tue_lunch: bool,
    pub tue_tea: bool,
    pub tue_evening: bool,
    pub tue_night: bool,
    pub wed_morning: bool,
    pub wed_lunch: bool,
    pub wed_tea: bool,
    pub wed_evening: bool,
    pub wed_night: bool,
    pub thu_morning: bool,
    pub thu_lunch: bool,
    pub thu_tea: bool,
    pub thu_evening: bool,
    pub thu_night: bool,
    pub fri_morning: bool,
    pub fri_lunch: bool,
    pub fri_tea: bool,
    pub fri_evening: bool,
    pub fri_night: bool,
    pub sat_morning: bool,
    pub sat_lunch: bool,
    pub sat_tea: bool,
    pub sat_evening: bool,
    pub sat_night: bool,
    pub sun_morning: bool,
    pub sun_lunch: bool,
    pub sun_tea: bool,
    pub sun_evening: bool,
    pub sun_night: bool,
    pub note: String,
    pub created_at: chrono::NaiveDateTime,
}
