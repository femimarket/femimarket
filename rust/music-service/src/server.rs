use crate::api;
use axum::http::Uri;
use axum::middleware::Next;
use axum::routing::post;
use axum::{
    extract::Request,
    http::{header, StatusCode},
    response::{IntoResponse, Response},
    Router,
};
use axum_core::extract::DefaultBodyLimit;
use clap::{Parser, Subcommand};
use mysql_async::Pool;
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Instant;
use tower_http::cors::{Any, CorsLayer};

#[derive(Parser, Clone)]
#[command(name = "music-service", about = "Music service")]
pub struct Cli {
    #[arg(long, env = "DATABASE_URL")]
    pub database_url: String,
    #[arg(long, env = "MATRIX_URL")]
    pub matrix_url: String,
    #[arg(long, env = "FS_URL")]
    pub fs_url: String,
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
        #[arg(long,)]
        web_dir: String,
        #[arg(long,)]
        alibaba_studio_api: String,
        #[arg(long,)]
        alibaba_studio_base: String,
        #[arg(long,)]
        fal_key: String,
        #[arg(long,)]
        comfy_key: String,
        #[arg(long,)]
        ngrok_ai: String,
        #[arg(long,)]
        ngrok_ai_url: String,
        #[arg(long,)] qwen_asr_0_6b_dir: String,
        #[arg(long,)] comfy_ltx2_3a2v_workflow: String,
        #[arg(long,)] comfy_flux2_klein_i2i_workflow: String,
        #[arg(long,)] comfy_flux2_dev_i2i_workflow: String,
        #[arg(long,)] sslpub: String,
        #[arg(long,)] sslpriv: String,
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

// One INFO event per HTTP request, in logfmt style:
// `INFO access: method=GET path=/foo status=200 duration_ms=4 ip=1.2.3.4`
// Uses `target: "access"` instead of the module path so the line stays short
// and grep-friendly. Behind ngrok the real client IP is in X-Forwarded-For;
// peer-level it'd be 127.0.0.1.
async fn log_requests(request: Request, next: Next) -> Response {
    let method = request.method().clone();
    let uri = request.uri().clone();
    // Behind ngrok: X-Forwarded-For for real client IP, X-Forwarded-Host for
    // the public hostname (api.earnfemi.com etc.). Fall back to Host header
    // if the proxy headers aren't set (direct hit to :3000).
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

    // On any error — including the extractor rejections (bad params, malformed
    // body) that never reach api_handler — buffer the response body, which holds
    // the reason, and put it on the line. Success: log without touching the body.
    if status.is_client_error() || status.is_server_error() {
        let (parts, body) = response.into_parts();
        let bytes = axum::body::to_bytes(body, usize::MAX).await.unwrap_or_default();
        let error = String::from_utf8_lossy(&bytes);
        // 5xx is our fault (ERROR); 4xx is the client's (WARN).
        if status.is_server_error() {
            tracing::event!(
                tracing::Level::ERROR,
                path = %uri, host = %host, method = %method,
                status = status.as_u16(), duration_ms, ip = %ip,
                error = %error,
            );
        } else {
            tracing::event!(
                tracing::Level::WARN,
                path = %uri, host = %host, method = %method,
                status = status.as_u16(), duration_ms, ip = %ip,
                error = %error,
            );
        }
        axum::response::Response::from_parts(parts, axum::body::Body::from(bytes))
    } else {
        tracing::event!(
            tracing::Level::INFO,
            path = %uri, host = %host, method = %method,
            status = status.as_u16(), duration_ms, ip = %ip,
        );
        response
    }
}

#[derive(utoipa::OpenApi)]
#[openapi(paths(crate::assets::post::create::route::create), modifiers(&SecurityAddon))]
pub struct ApiDoc;

pub async fn start_server(cli: Cli) -> Result<Router, Box<dyn std::error::Error>> {
    tracing::info!("Starting server");
    let pool = Pool::new(
        mysql_async::OptsBuilder::from_opts(mysql_async::Opts::from_url(&cli.database_url)?)
            .setup(vec!["SET time_zone = '+00:00'"]),
    );

    let femi_market_dir = crate::commands::serve::WEB_DIR_LOCK.get().unwrap().clone();

    let root = Path::new(&femi_market_dir);
    let mut map = HashMap::new();
    let mut stack: Vec<PathBuf> = vec![root.to_path_buf()];
    while let Some(dir) = stack.pop() {
        let Ok(entries) = std::fs::read_dir(&dir) else {
            continue;
        };
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_dir() {
                stack.push(path);
            } else if let Ok(bytes) = std::fs::read(&path) {
                let rel = path
                    .strip_prefix(root)
                    .expect("strip_prefix")
                    .components()
                    .map(|c| c.as_os_str().to_string_lossy())
                    .collect::<Vec<_>>()
                    .join("/");
                map.insert(rel, bytes);
            }
        }
    }

    let femi_market = Arc::new(map);
    let femi_market2 = femi_market.clone();
    tracing::info!(dir = %femi_market_dir, files = femi_market.len(), "loaded femi.market assets");
    Ok(Router::new()
        .merge(crate::assets::route::route())
        .route("/", post(api::handler::api).fallback(move |uri: Uri| {
            let dir = femi_market.clone();
            handle_fallback(uri.clone(), dir)
        }))
        .layer(DefaultBodyLimit::max((1024 * 1024) * 30))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        )
        .fallback(move |uri: Uri| {
            let dir = femi_market2.clone();
            handle_fallback(uri, dir)
        })
        .layer(axum::middleware::from_fn(log_requests))
        .with_state(AppState { cli, pool }))
}

fn handle_fallback(uri: Uri, dir: Arc<HashMap<String, Vec<u8>>>) -> impl Future<Output=Response> {
    async move {
        let path = uri.path().trim_start_matches('/');
        let primary = match path {
            "" | "legal" | "privacy-policy" => "index.html",
            _ => path,
        };
        let mut response = if let Some(bytes) = dir.get(primary) {
            let mime = mime_guess::from_path(primary).first_or_octet_stream();
            ([(header::CONTENT_TYPE, mime.as_ref())], bytes.clone()).into_response()
        } else {
            (StatusCode::NOT_FOUND, "404").into_response()
        };
        response.headers_mut().insert(
            "Document-Isolation-Policy",
            "isolate-and-credentialless".parse().unwrap(),
        );
        response
    }
}
