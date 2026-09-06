use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Router;
use mysql_async::Pool;

pub(crate) fn route() -> Router<Pool> {
    Router::new().route("/shifts/nearby/{postcode}/{min}/{max}", axum::routing::get(nearby))
}

#[utoipa::path(
    get,
    path = "/shifts/nearby/{postcode}/{min}/{max}",
    tag = "shifts",
    params(("postcode" = String, Path), ("min" = i32, Path), ("max" = i32, Path)),
    responses((status = OK, body = String)),
)]
#[tracing::instrument(skip_all)]
pub(crate) async fn nearby(
    State(pool): State<Pool>,
    Path((postcode, min, max)): Path<(String, i32, i32)>,
) -> Result<String, (StatusCode, String)> {
    let travel_url = std::env::var("TRAVEL_URL")
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    super::handle::nearby(&pool, &travel_url, &postcode, min, max).await
}
