//! `meta-service` — axum server reading/writing femi's Audio metadata tags, one
//! GET + POST endpoint pair per field.
//!
//! `meta-service <DIR> [--port 9006]` — DIR is the served media directory (the dufs
//! dir). Each `Audio.kt` field is `POST /audio/<field>` (body `{ "file", "value" }`)
//! and `GET /audio/<field>/{file}` with its own structs (`src/audio/<field>.rs`), so
//! utoipa translates them 1:1 into OpenAPI for the Kotlin Multiplatform client
//! generation in `app/shared/build.gradle.kts` (openapi-generator, inputSpec = this
//! server's spec URL). There is no model-level read — readAudio is the Kotlin side
//! composing the field GETs.
//!
//! Swagger UI at `/swagger-ui`, spec at `/api-docs/openapi.json`.
//!
//! For audio, id3 is the source of truth: every post writes the field's TXXX custom
//! frame (ours), and fields with an interoperable home also write the proper frame
//! other apps read — TCON, TALB, USLT, SYLT (port of `id3-write-sylt`), APIC (port
//! of `id3-write-protagonist`). Gets read the TXXX first, then that proper place.

use axum::routing::{get, post};
use clap::Parser;
use meta_service::{audio, DIR};
use tower_http::cors::{Any, CorsLayer};
use utoipa::OpenApi;
use utoipa_swagger_ui::SwaggerUi;

#[derive(Parser)]
#[command(name = "meta-service", about = "Write femi metadata tags over HTTP, one endpoint per field")]
struct Cli {
    /// Directory holding the media files (the dufs dir)
    dir: std::path::PathBuf,
    #[arg(long, default_value_t = 9006)]
    port: u16,
}

#[derive(OpenApi)]
#[openapi(paths(
    meta_service::audio::id::get, meta_service::audio::id::post,
    meta_service::audio::backed_up::get, meta_service::audio::backed_up::post,
    meta_service::audio::name::get, meta_service::audio::name::post,
    meta_service::audio::error::get, meta_service::audio::error::post,
    meta_service::audio::genre::get, meta_service::audio::genre::post,
    meta_service::audio::image::get, meta_service::audio::image::post,
    meta_service::audio::like::get, meta_service::audio::like::post,
    meta_service::audio::lyrics::get, meta_service::audio::lyrics::post,
    meta_service::audio::edited_lyrics::get, meta_service::audio::edited_lyrics::post,
    meta_service::audio::eleven_labs_forced_alignment::get, meta_service::audio::eleven_labs_forced_alignment::post,
    meta_service::audio::protagonist::get, meta_service::audio::protagonist::post,
    meta_service::audio::project::get, meta_service::audio::project::post,
    meta_service::audio::uid::get, meta_service::audio::uid::post,
    meta_service::audio::audio_lines::get, meta_service::audio::audio_lines::post,
    meta_service::audio::word_alignments::get, meta_service::audio::word_alignments::post,
    meta_service::audio::faqs::get, meta_service::audio::faqs::post,
    meta_service::audio::social_media_blueprint::get, meta_service::audio::social_media_blueprint::post,
    meta_service::audio::video::get, meta_service::audio::video::post,
    meta_service::audio::lyric_tokens::get, meta_service::audio::lyric_tokens::post,
))]
struct ApiDoc;

#[tokio::main]
async fn main() {
    let cli = Cli::parse();
    DIR.set(cli.dir).expect("dir set once");

    let app = axum::Router::new()
        .route("/audio/id", post(audio::id::post))
        .route("/audio/id/{file}", get(audio::id::get))
        .route("/audio/backedUp", post(audio::backed_up::post))
        .route("/audio/backedUp/{file}", get(audio::backed_up::get))
        .route("/audio/name", post(audio::name::post))
        .route("/audio/name/{file}", get(audio::name::get))
        .route("/audio/error", post(audio::error::post))
        .route("/audio/error/{file}", get(audio::error::get))
        .route("/audio/genre", post(audio::genre::post))
        .route("/audio/genre/{file}", get(audio::genre::get))
        .route("/audio/image", post(audio::image::post))
        .route("/audio/image/{file}", get(audio::image::get))
        .route("/audio/like", post(audio::like::post))
        .route("/audio/like/{file}", get(audio::like::get))
        .route("/audio/lyrics", post(audio::lyrics::post))
        .route("/audio/lyrics/{file}", get(audio::lyrics::get))
        .route("/audio/editedLyrics", post(audio::edited_lyrics::post))
        .route("/audio/editedLyrics/{file}", get(audio::edited_lyrics::get))
        .route("/audio/elevenLabsForcedAlignment", post(audio::eleven_labs_forced_alignment::post))
        .route("/audio/elevenLabsForcedAlignment/{file}", get(audio::eleven_labs_forced_alignment::get))
        .route("/audio/protagonist", post(audio::protagonist::post))
        .route("/audio/protagonist/{file}", get(audio::protagonist::get))
        .route("/audio/project", post(audio::project::post))
        .route("/audio/project/{file}", get(audio::project::get))
        .route("/audio/uid", post(audio::uid::post))
        .route("/audio/uid/{file}", get(audio::uid::get))
        .route("/audio/audioLines", post(audio::audio_lines::post))
        .route("/audio/audioLines/{file}", get(audio::audio_lines::get))
        .route("/audio/wordAlignments", post(audio::word_alignments::post))
        .route("/audio/wordAlignments/{file}", get(audio::word_alignments::get))
        .route("/audio/faqs", post(audio::faqs::post))
        .route("/audio/faqs/{file}", get(audio::faqs::get))
        .route("/audio/socialMediaBlueprint", post(audio::social_media_blueprint::post))
        .route("/audio/socialMediaBlueprint/{file}", get(audio::social_media_blueprint::get))
        .route("/audio/video", post(audio::video::post))
        .route("/audio/video/{file}", get(audio::video::get))
        .route("/audio/lyricTokens", post(audio::lyric_tokens::post))
        .route("/audio/lyricTokens/{file}", get(audio::lyric_tokens::get))
        .merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", ApiDoc::openapi()))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        )
        // one request at a time: writes rewrite files in place, so concurrent same-file
        // requests would lose updates (or corrupt mid-rewrite) — serialize them all.
        // MUST be the Global variant: Router::layer wraps every route separately, and the
        // plain ConcurrencyLimitLayer mints a fresh semaphore per wrap — 38 routes got 38
        // independent limits, so different-field posts to the same file still raced (the
        // torn-tag bug). GlobalConcurrencyLimitLayer shares ONE semaphore across all routes.
        .layer(tower::limit::GlobalConcurrencyLimitLayer::new(1));

    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cli.port)).await.expect("bind");
    println!(
        "meta-service serving {} on http://0.0.0.0:{} — swagger at /swagger-ui",
        DIR.get().unwrap().display(),
        cli.port
    );
    axum::serve(listener, app).await.expect("serve");
}
