//! The declared care package: each row says "these calls on these days" -
//! the row's true days crossed with its true calls. A client is one row
//! when their week is uniform, more when it isn't. Weeks are generated and
//! audited from this - never copied from a previous week.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "packages")]
pub struct Package {
    pub id: i32,
    pub client_id: i32,
    pub monday: bool,
    pub tuesday: bool,
    pub wednesday: bool,
    pub thursday: bool,
    pub friday: bool,
    pub saturday: bool,
    pub sunday: bool,
    pub morning: bool,
    pub lunch: bool,
    pub tea: bool,
    pub evening: bool,
    pub night: bool,
    pub note: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    pub updated_note: Option<String>,
    pub updated_at: Option<chrono::NaiveDateTime>,
}
