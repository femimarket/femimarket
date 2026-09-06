use clap::{Parser, Subcommand};
use sea_orm::Database;

use match_service::commands;

#[derive(Parser)]
#[command(name = "match-service", about = "Match service")]
struct Cli {
    #[arg(long, env = "DATABASE_URL")]
    database_url: String,
    #[command(flatten)]
    verbosity: clap_verbosity_flag::Verbosity,
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    Init,
    Openapi,
    Serve {
        #[arg(long)]
        port: u16,
    },
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

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

    let database_url = cli.database_url;
    match cli.command {
        Command::Init => {
            use sea_orm::ConnectionTrait;
            let url = url::Url::parse(&database_url)?;
            let db = Database::connect(format!(
                "{}://{}:{}@{}",
                url.scheme(),
                url.username(),
                url.password().ok_or_else(|| sea_orm::DbErr::Custom(format!("no password in {database_url}")))?,
                url.host_str().ok_or_else(|| sea_orm::DbErr::Custom(format!("no host in {database_url}")))?,
            ))
            .await?;
            db.execute_unprepared(&format!("CREATE DATABASE IF NOT EXISTS `{}`", url.path().trim_start_matches('/'))).await?;
            let db = Database::connect(&database_url).await?;
            db.get_schema_registry("match_service::*").sync(&db).await?;
            println!("schema ready");
        }
        Command::Openapi => {
            use utoipa::OpenApi;
            println!("{}", match_service::server::ApiDoc::openapi().to_pretty_json()?);
        }
        Command::Serve { port } => {
            commands::serve::serve(&database_url, port).await?;
        }
    }
    Ok(())
}
