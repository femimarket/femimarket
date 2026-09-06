//! The long list: the specific things done on a visit. Every skill belongs
//! to one care; the care is claimed whole, the skills say what it comprises.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "skills")]
pub struct Skill {
    pub id: String,
    pub care_id: String,
    pub note: String,
}
