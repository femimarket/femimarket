use music_service::commands;
use music_service::server::{Cli, Command};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = <Cli as clap::Parser>::parse();

    let log_level = cli.verbosity.log_level_filter().to_string().to_lowercase();
    let filter = tracing_subscriber::EnvFilter::builder()
        .with_default_directive(log_level.parse()?)
        .from_env_lossy();
    let f = tracing_subscriber::fmt::layer().compact().with_span_events(
        tracing_subscriber::fmt::format::FmtSpan::NEW
            | tracing_subscriber::fmt::format::FmtSpan::CLOSE,
    );
    use tracing_subscriber::layer::SubscriberExt;
    use tracing_subscriber::util::SubscriberInitExt;
    tracing_subscriber::registry().with(filter).with(f).init();

    match cli.command.clone() {
        Command::Init => {
            use mysql_async::prelude::*;
            let pool = mysql_async::Pool::new(cli.database_url.as_str());
            let mut conn = pool.get_conn().await?;
            let src = std::path::Path::new(concat!(env!("CARGO_MANIFEST_DIR"), "/src"));
            for migrations in std::iter::once(src.join("migrations"))
                .chain(std::fs::read_dir(src)?.flatten().map(|module| module.path().join("migrations")))
                .filter(|migrations| migrations.is_dir())
            {
                let mut files: Vec<_> = std::fs::read_dir(migrations)?
                    .flatten()
                    .map(|file| file.path())
                    .filter(|file| file.extension().is_some_and(|extension| extension == "sql"))
                    .collect();
                files.sort();
                for file in files {
                    conn.query_drop(std::fs::read_to_string(file)?).await?;
                }
            }
            drop(conn);
            pool.disconnect().await?;
            println!("schema ready");
        }
        Command::Openapi => {
            use utoipa::OpenApi;
            println!("{}", music_service::server::ApiDoc::openapi().to_pretty_json()?);
        }
        Command::Serve {
            web_dir,
            alibaba_studio_api,
            alibaba_studio_base,
            fal_key,
            comfy_key,
            ngrok_ai,
            ngrok_ai_url,
            qwen_asr_0_6b_dir,
            comfy_ltx2_3a2v_workflow,
            comfy_flux2_klein_i2i_workflow,
            comfy_flux2_dev_i2i_workflow,
            sslpub,
            sslpriv,
        } => {
            commands::serve::serve(
                cli.clone(),
                web_dir,
                alibaba_studio_api,
                alibaba_studio_base,
                fal_key,
                comfy_key,
                ngrok_ai,
                ngrok_ai_url,
                qwen_asr_0_6b_dir,
                comfy_flux2_klein_i2i_workflow,
                comfy_flux2_dev_i2i_workflow,
                comfy_ltx2_3a2v_workflow,
                sslpub,
                sslpriv,
            )
            .await?;
        }
    }
    Ok(())
}
