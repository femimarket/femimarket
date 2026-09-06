use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use crate::users::route::Me;
use crate::users::{User, UserType};

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/users/{id}", axum::routing::get(id))
}

#[utoipa::path(
    get,
    path = "/users/{id}",
    tag = "users",
    params(("id" = String, Path)),
    responses((status = OK, body = User), (status = UNAUTHORIZED, body = String), (status = FORBIDDEN, body = String), (status = NOT_FOUND, body = String)),
)]
#[tracing::instrument(skip_all)]
pub(crate) async fn id(
    State(pool): State<Pool>,
    me: Me,
    Path(id): Path<String>,
) -> Result<Json<User>, (StatusCode, String)> {
    let user = super::handle::id(&pool, &id)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, format!("no user {id}")))?;
    let management = me.users.iter().any(|user| user.user_type == UserType::Management);
    if !management && !me.users.iter().any(|mine| mine.id == user.id) {
        return Err((StatusCode::FORBIDDEN, "only the owner or management may read a user".to_string()));
    }
    Ok(Json(user))
}
