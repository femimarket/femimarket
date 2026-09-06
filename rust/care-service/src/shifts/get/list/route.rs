use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use mysql_async::Pool;

use super::handle::PublicShift;

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/shifts", axum::routing::get(list))
}

#[utoipa::path(get, path = "/shifts", tag = "shifts", responses((status = OK, body = Vec<PublicShift>)))]
#[tracing::instrument(skip_all)]
pub(crate) async fn list(
    State(pool): State<Pool>,
) -> Result<Json<Vec<PublicShift>>, (StatusCode, String)> {
    super::handle::list(&pool)
        .await
        .map(Json)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))
}
