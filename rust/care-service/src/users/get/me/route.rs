use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use crate::users::route::Me;
use crate::users::User;

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/users/me", axum::routing::get(me))
}

#[utoipa::path(
    get,
    path = "/users/me",
    tag = "users",
    responses((status = OK, body = Vec<User>), (status = UNAUTHORIZED, body = String)),
)]
#[tracing::instrument(
    skip_all,
    fields(
        user = tracing::field::Empty,
        status = tracing::field::Empty,
        message = tracing::field::Empty,
    ),
)]
pub(crate) async fn me(
    State(pool): State<Pool>,
    me: Result<Me, (StatusCode, String)>,
) -> Result<Json<Vec<User>>, (StatusCode, String)> {
    let span = tracing::Span::current();
    let me = me.map_err(|(status, message)| {
        span.record("status", status.as_u16()).record("message", tracing::field::display(&message));
        (status, message)
    })?;
    span.record("user", tracing::field::display(&me.matrix_user.matrix_user_id));
    super::handle::me(&pool, me.matrix_user.id)
        .await
        .map(|users| {
            span.record("status", 200);
            Json(users)
        })
        .map_err(|error| {
            span.record("status", 500).record("message", tracing::field::display(&error));
            (StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
        })
}
