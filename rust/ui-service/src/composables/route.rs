use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use sea_orm::DatabaseConnection;

use super::{Model, ModelEx};

pub fn route() -> Router<DatabaseConnection> {
    Router::new()
        .route("/composables", axum::routing::get(list))
        .route("/composables/{id}", axum::routing::get(get))
}

#[utoipa::path(get, path = "/composables", tag = "composables", responses((status = OK, body = Vec<ModelEx>)))]
pub async fn list(State(db): State<DatabaseConnection>) -> Result<Json<Vec<Model>>, StatusCode> {
    super::list::list(&db).await.map(Json).map_err(|error| {
        tracing::error!(%error, "composables list failed");
        StatusCode::INTERNAL_SERVER_ERROR
    })
}

#[utoipa::path(
    get,
    path = "/composables/{id}",
    tag = "composables",
    params(("id" = i32, Path)),
    responses((status = OK, body = ModelEx), (status = NOT_FOUND)),
)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path(id): Path<i32>,
) -> Result<Json<ModelEx>, StatusCode> {
    super::get::get(&db, id)
        .await
        .map_err(|error| {
            tracing::error!(%error, id, "composable get failed");
            StatusCode::INTERNAL_SERVER_ERROR
        })?
        .map(Json)
        .ok_or(StatusCode::NOT_FOUND)
}
