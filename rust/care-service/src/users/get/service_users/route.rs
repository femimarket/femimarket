use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use crate::users::route::Me;
use crate::users::{User, UserType};

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/users/service-users", axum::routing::get(service_users))
}

#[utoipa::path(
    get,
    path = "/users/service-users",
    tag = "users",
    responses((status = OK, body = Vec<User>), (status = UNAUTHORIZED, body = String), (status = FORBIDDEN, body = String)),
)]
#[tracing::instrument(
    skip_all,
    fields(
        user = tracing::field::Empty,
        status = tracing::field::Empty,
        message = tracing::field::Empty,
    ),
)]
pub(crate) async fn service_users(
    State(pool): State<Pool>,
    me: Result<Me, (StatusCode, String)>,
) -> Result<Json<Vec<User>>, (StatusCode, String)> {
    let span = tracing::Span::current();
    let me = me.map_err(|(status, message)| {
        span.record("status", status.as_u16()).record("message", tracing::field::display(&message));
        (status, message)
    })?;
    span.record("user", tracing::field::display(&me.matrix_user.matrix_user_id));
    if !me.users.iter().any(|user| user.user_type == UserType::Management) {
        span.record("status", 403).record("message", "only management may list service users");
        return Err((StatusCode::FORBIDDEN, "only management may list service users".to_string()));
    }
    super::handle::service_users(&pool)
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
