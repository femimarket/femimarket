use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use crate::users::route::Me;
use crate::users::{User, UserType};

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/users/{id}", axum::routing::put(id))
}

#[utoipa::path(
    put,
    path = "/users/{id}",
    tag = "users",
    params(("id" = String, Path)),
    request_body = User,
    responses((status = OK, body = User), (status = UNAUTHORIZED, body = String), (status = FORBIDDEN, body = String), (status = NOT_FOUND, body = String)),
)]
#[tracing::instrument(skip_all)]
pub(crate) async fn id(
    State(pool): State<Pool>,
    me: Me,
    Path(id): Path<String>,
    Json(user): Json<User>,
) -> Result<Json<User>, (StatusCode, String)> {
    let target = crate::users::get::id::handle::id(&pool, &id)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, format!("no user {id}")))?;
    let management = me.users.iter().any(|user| user.user_type == UserType::Management);
    let user = if management {
        User {
            user_type: user.user_type,
            first_name: user.first_name,
            last_name: user.last_name,
            ..target
        }
    } else if me.users.iter().any(|mine| mine.id == target.id) {
        User {
            first_name: user.first_name,
            last_name: user.last_name,
            ..target
        }
    } else {
        return Err((StatusCode::FORBIDDEN, "only the owner or management may update a user".to_string()));
    };
    super::handle::id(&pool, user)
        .await
        .map(Json)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))
}
