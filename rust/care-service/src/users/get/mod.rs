pub mod id;
pub mod list;
pub mod me;
pub mod route;
pub mod service_users;

#[derive(serde::Deserialize, utoipa::IntoParams)]
pub struct UserGetQuery {
    pub matrix_id: Option<i32>,
}
