use axum::extract::{Query, State};
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use crate::users::get::UserGetQuery;
use crate::users::route::Me;
use crate::users::{User, UserType};

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/users", axum::routing::get(list))
}

#[utoipa::path(
    get,
    path = "/users",
    tag = "users",
    params(UserGetQuery),
    responses((status = OK, body = Vec<User>), (status = UNAUTHORIZED, body = String), (status = FORBIDDEN, body = String)),
)]
#[tracing::instrument(skip_all)]
pub(crate) async fn list(
    State(pool): State<Pool>,
    me: Me,
    Query(query): Query<UserGetQuery>,
) -> Result<Json<Vec<User>>, (StatusCode, String)> {
    let management = me.users.iter().any(|user| user.user_type == UserType::Management);
    if !management && query.matrix_id != Some(me.matrix_user.id) {
        return Err((StatusCode::FORBIDDEN, "only management may list other users".to_string()));
    }
    super::handle::list(&pool, query)
        .await
        .map(Json)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))
}
