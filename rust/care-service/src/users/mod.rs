//! Every person the system knows - usertype says which kind:
//! ServiceUser or CareWorker.

pub mod get;
pub mod post;
pub mod put;
pub mod route;

use mysql_async::prelude::{FromRow, FromValue};

#[derive(Clone, Debug, PartialEq, Eq, FromValue, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", is_string)]
pub enum UserType {
    ServiceUser,
    Staff,
    Candidate,
    Management,
}

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "users")]
pub struct User {
    pub id: String,
    pub user_type: UserType,
    pub first_name: Option<String>,
    pub last_name: Option<String>,
    pub matrix_id: Option<i32>,
    pub parent_id: Option<String>,
    pub created_at: chrono::NaiveDateTime,
}
