use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use crate::users::route::Me;
use crate::users::{User, UserType};

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/users", axum::routing::post(create))
}

#[utoipa::path(post, path = "/users", tag = "users", request_body = User, responses((status = OK, body = User), (status = UNAUTHORIZED, body = String), (status = FORBIDDEN, body = String), (status = CONFLICT, body = String)))]
#[tracing::instrument(skip_all)]
pub(crate) async fn create(
    State(pool): State<Pool>,
    me: Me,
    Json(user): Json<User>,
) -> Result<Json<User>, (StatusCode, String)> {
    match user.user_type {
        UserType::Management => {
            return Err((StatusCode::FORBIDDEN, "management is created by code".to_string()));
        }
        UserType::Candidate => {
            if me.users.iter().any(|user| user.user_type == UserType::Candidate) {
                return Err((StatusCode::CONFLICT, "already a candidate".to_string()));
            }
            super::handle::create(&pool, User {
                user_type: UserType::Candidate,
                matrix_id: Some(me.matrix_user.id),
                parent_id: None,
                first_name: None,
                last_name: None,
                ..user
            })
        }
        UserType::Staff | UserType::ServiceUser => {
            let Some(management) = me.users.iter().find(|user| user.user_type == UserType::Management) else {
                return Err((StatusCode::FORBIDDEN, "staff and service users are created by management".to_string()));
            };
            super::handle::create(&pool, User { parent_id: Some(management.id.clone()), ..user })
        }
    }
    .await
    .map(Json)
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))
}
