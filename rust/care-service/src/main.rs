use care_service::commands::get_clients::get_clients;
use care_service::commands::get_staffs::get_staffs;
use care_service::sync_travel_times;
use clap::{Parser, Subcommand};

#[derive(Parser)]
#[command(name = "care-service", about = "Rota database schema service")]
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
    GetStaffs {
        #[arg(long, env = "ROUNDSYS_REGION")]
        region: String,
        #[arg(long, env = "ROUNDSYS_VERSION")]
        rockstar_version: String,
        #[arg(long, env = "ROUNDSYS_COOKIE")]
        cookie: String,
    },
    GetClients {
        #[arg(long, env = "ROUNDSYS_REGION")]
        region: String,
        #[arg(long, env = "ROUNDSYS_VERSION")]
        rockstar_version: String,
        #[arg(long, env = "ROUNDSYS_COOKIE")]
        cookie: String,
    },
    SyncTravelTimes {
        #[arg(long)]
        from_date: String,
        #[arg(long)]
        to_date: String,
        #[arg(long)]
        server: String,
    },
    Solve {
        #[arg(long)]
        rota_id: i32,
        #[arg(long, default_value_t = 300)]
        max_seconds: i64,
        #[arg(long)]
        travel_times: String,
    },
    PublishPostcodes,
    PublishRota {
        #[arg(long)]
        rota_id: i32,
        #[arg(long, env = "ROUNDSYS_REGION")]
        region: String,
        #[arg(long, env = "ROUNDSYS_VERSION")]
        rockstar_version: String,
        #[arg(long, env = "ROUNDSYS_COOKIE")]
        cookie: String,
        #[arg(long)]
        go: bool,
    },
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
            use mysql_async::prelude::*;
            let pool = mysql_async::Pool::new(database_url.as_str());
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
            println!("{}", care_service::server::ApiDoc::openapi().to_pretty_json()?);
        }
        Command::GetStaffs {
            region,
            rockstar_version,
            cookie,
        } => {
            get_staffs(&database_url, &region, &rockstar_version, &cookie).await?;
        }
        Command::GetClients {
            region,
            rockstar_version,
            cookie,
        } => {
            get_clients(&database_url, &region, &rockstar_version, &cookie).await?;
        }
        Command::SyncTravelTimes { from_date, to_date, server } => {
            sync_travel_times(&server, &from_date, &to_date).await?;
        }
        Command::Solve { rota_id, max_seconds, travel_times } => {
            care_service::commands::solve::solve(&database_url, rota_id, max_seconds, &travel_times)
                .await?;
        }
        Command::PublishPostcodes => {
            care_service::commands::publish_postcodes::publish_postcodes(&database_url)
                .await?;
        }
        Command::PublishRota { rota_id, region, rockstar_version, cookie, go } => {
            care_service::commands::publish_rota::publish_rota(
                &database_url, rota_id, &region, &rockstar_version, &cookie, go).await?;
        }
        Command::Serve { port } => {
            care_service::commands::serve::serve(&database_url, port).await?;
        }
    }
    Ok(())
}
