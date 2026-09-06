use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use axum_extra::headers::authorization::Bearer;
use axum_extra::headers::Authorization;
use axum_extra::TypedHeader;

use crate::server::AppState;
use crate::users::User;

pub(crate) fn route() -> Router<AppState> {
    Router::new().route("/users/me", axum::routing::get(me))
}

#[utoipa::path(
    get,
    path = "/users/me",
    tag = "users",
    security(("bearer" = [])),
    responses((status = OK, body = User), (status = UNAUTHORIZED), (status = NOT_FOUND)),
)]
pub(crate) async fn me(
    State(state): State<AppState>,
    bearer: Option<TypedHeader<Authorization<Bearer>>>,
) -> Result<Json<User>, StatusCode> {
    let TypedHeader(Authorization(bearer)) = bearer.ok_or(StatusCode::UNAUTHORIZED)?;
    super::handle::me(&state.pool, &state.cli.matrix_url, bearer.token())
        .await
        .map_err(|error| {
            tracing::error!(%error, "users get failed");
            StatusCode::INTERNAL_SERVER_ERROR
        })?
        .map(Json)
        .ok_or(StatusCode::NOT_FOUND)
}
