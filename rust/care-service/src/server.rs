use crate::{shifts, users};
use axum::extract::{DefaultBodyLimit, Request};
use axum::middleware::Next;
use axum::response::Response;
use axum::Router;
use std::time::Instant;
use tower_http::cors::{Any, CorsLayer};

async fn log_requests(request: Request, next: Next) -> Response {
    let method = request.method().clone();
    let uri = request.uri().clone();
    let ip = request
        .headers()
        .get("x-forwarded-for")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.split(',').next())
        .map(str::trim)
        .unwrap_or("?")
        .to_string();
    let host = request
        .headers()
        .get("x-forwarded-host")
        .or_else(|| request.headers().get(axum::http::header::HOST))
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.split(':').next())
        .map(str::trim)
        .unwrap_or("?")
        .to_string();

    let start = Instant::now();
    let response = next.run(request).await;
    let duration_ms = start.elapsed().as_millis();
    let status = response.status();

    match status.is_client_error() || status.is_server_error() {
        true => {
            let (parts, body) = response.into_parts();
            let bytes = axum::body::to_bytes(body, usize::MAX).await.unwrap_or_default();
            let error = String::from_utf8_lossy(&bytes);
            match status.is_server_error() {
                true => tracing::event!(
                    tracing::Level::ERROR,
                    path = %uri, host = %host, method = %method,
                    status = status.as_u16(), duration_ms, ip = %ip,
                    error = %error,
                ),
                false => tracing::event!(
                    tracing::Level::WARN,
                    path = %uri, host = %host, method = %method,
                    status = status.as_u16(), duration_ms, ip = %ip,
                    error = %error,
                ),
            }
            Response::from_parts(parts, axum::body::Body::from(bytes))
        }
        false => {
            tracing::event!(
                tracing::Level::INFO,
                path = %uri, host = %host, method = %method,
                status = status.as_u16(), duration_ms, ip = %ip,
            );
            response
        }
    }
}

#[derive(utoipa::OpenApi)]
#[openapi(paths(
    users::post::create::route::create,
    users::put::id::route::id,
    users::get::list::route::list,
    users::get::me::route::me,
    users::get::service_users::route::service_users,
    users::get::id::route::id,
    shifts::get::list::route::list,
    shifts::get::nearby::route::nearby,
))]
pub struct ApiDoc;

pub async fn start_server(database_url: &str) -> Result<Router, Box<dyn std::error::Error>> {
    tracing::info!("Starting server");
    let pool = mysql_async::Pool::new(
        mysql_async::OptsBuilder::from_opts(mysql_async::Opts::from_url(database_url)?)
            .setup(vec!["SET time_zone = '+00:00'"]),
    );
    Ok(Router::new()
        .merge(shifts::route::route())
        .merge(users::route::route())
        .layer(DefaultBodyLimit::max((1024 * 1024) * 30))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        )
        .layer(axum::middleware::from_fn(log_requests))
        .with_state(pool))
}
