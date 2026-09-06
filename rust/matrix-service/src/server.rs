use axum::extract::{DefaultBodyLimit, Request};
use axum::middleware::Next;
use axum::response::Response;
use axum::Router;
use clap::{Parser, Subcommand};
use mysql_async::Pool;
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

#[derive(Parser, Clone)]
#[command(name = "matrix-service", about = "Matrix service")]
pub struct Cli {
    #[arg(long, env = "DATABASE_URL")]
    pub database_url: String,
    #[arg(long, env = "MATRIX_URL")]
    pub matrix_url: String,
    #[command(flatten)]
    pub verbosity: clap_verbosity_flag::Verbosity,
    #[command(subcommand)]
    pub command: Command,
}

#[derive(Subcommand, Clone)]
pub enum Command {
    Init,
    Openapi,
    Serve {
        #[arg(long)]
        port: u16,
    },
}

#[derive(Clone)]
pub struct AppState {
    pub cli: Cli,
    pub pool: Pool,
}

pub struct SecurityAddon;

impl utoipa::Modify for SecurityAddon {
    fn modify(&self, openapi: &mut utoipa::openapi::OpenApi) {
        openapi.components.get_or_insert_default().add_security_scheme(
            "bearer",
            utoipa::openapi::security::SecurityScheme::Http(
                utoipa::openapi::security::HttpBuilder::new()
                    .scheme(utoipa::openapi::security::HttpAuthScheme::Bearer)
                    .build(),
            ),
        );
    }
}

#[derive(utoipa::OpenApi)]
#[openapi(servers((url = "https://femi.market/api/matrix")), paths(crate::users::get::me::route::me), modifiers(&SecurityAddon))]
pub struct ApiDoc;

pub async fn start_server(cli: Cli) -> Result<Router, Box<dyn std::error::Error>> {
    tracing::info!("Starting server");
    let pool = Pool::new(
        mysql_async::OptsBuilder::from_opts(mysql_async::Opts::from_url(&cli.database_url)?)
            .setup(vec!["SET time_zone = '+00:00'"]),
    );
    Ok(Router::new()
        .merge(crate::users::route::route())
        .layer(DefaultBodyLimit::max((1024 * 1024) * 30))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        )
        .layer(axum::middleware::from_fn(log_requests))
        .with_state(AppState { cli, pool }))
}
