use axum::extract::{Multipart, State};
use axum::http::StatusCode;
use axum::Json;
use axum::Router;
use axum_extra::headers::authorization::Bearer;
use axum_extra::headers::Authorization;
use axum_extra::TypedHeader;
use futures::TryStreamExt;

use crate::assets::Asset;
use crate::server::AppState;

pub(crate) fn route() -> Router<AppState> {
    Router::new().route("/assets", axum::routing::post(create))
}

// schema-only: the OpenAPI object for the multipart form, never constructed at runtime
#[derive(utoipa::ToSchema)]
pub struct UploadAsset {
    #[schema(value_type = String, format = Binary)]
    #[allow(dead_code)]
    pub file: String,
}

#[utoipa::path(
    post,
    path = "/assets",
    tag = "assets",
    security(("bearer" = [])),
    request_body(content = UploadAsset, content_type = "multipart/form-data"),
    responses((status = OK, body = Asset), (status = UNAUTHORIZED), (status = BAD_REQUEST)),
)]
#[tracing::instrument(
    skip_all,
    fields(
        user = tracing::field::Empty,
        token = tracing::field::Empty,
        file = tracing::field::Empty,
        status = tracing::field::Empty,
        message = tracing::field::Empty,
    ),
)]
pub(crate) async fn create(
    State(state): State<AppState>,
    bearer: Option<TypedHeader<Authorization<Bearer>>>,
    mut multipart: Multipart,
) -> Result<Json<Asset>, StatusCode> {
    let span = tracing::Span::current();
    let TypedHeader(Authorization(bearer)) = bearer.ok_or_else(|| {
        span.record("status", 401).record("message", "no bearer token on the request");
        StatusCode::UNAUTHORIZED
    })?;
    span.record("token", tracing::field::display(bearer.token()));
    let configuration = matrix_client::apis::configuration::Configuration {
        bearer_access_token: Some(bearer.token().to_string()),
        ..Default::default()
    };
    let user = matrix_client::apis::users_api::me(&configuration)
        .await
        .map_err(|error| {
            span.record("status", 401).record("message", tracing::field::display(&error));
            StatusCode::UNAUTHORIZED
        })?;
    span.record("user", tracing::field::display(&user.matrix_user_id));

    while let Ok(Some(field)) = multipart.next_field().await {
        if field.name() == Some("file") {
            let name = field.file_name().ok_or_else(|| {
                span.record("status", 400).record("message", "the 'file' part has no filename");
                StatusCode::BAD_REQUEST
            })?.to_string();
            span.record("file", tracing::field::display(&name));
            let reader = tokio_util::io::StreamReader::new(field.map_err(std::io::Error::other));
            return super::handle::create(&state, user.id, name, reader)
                .await
                .map(|asset| {
                    span.record("status", 200);
                    Json(asset)
                })
                .map_err(|error| {
                    span.record("status", 500).record("message", tracing::field::display(&error));
                    StatusCode::INTERNAL_SERVER_ERROR
                });
        }
    }
    span.record("status", 400).record("message", "no 'file' field in the form");
    Err(StatusCode::BAD_REQUEST)
}
