use clap::{Parser, Subcommand};

use travel_service::commands;

#[derive(Parser)]
#[command(name = "travel-service", about = "Travel time engine")]
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
    LoadPostcodes {
        #[arg(long)]
        onspd: String,
    },
    Serve {
        #[arg(long)]
        port: u16,
    },
    ExportTravelTimes {
        #[arg(long)]
        from_date: String,
        #[arg(long)]
        to_date: String,
    },
    ComputeTravelTimes {
        #[arg(long)]
        from_date: String,
        #[arg(long)]
        to_date: String,
        #[arg(long, env = "MOTIS_URL")]
        motis_url: String,
        #[arg(long, value_delimiter = ',', required = true)]
        from_postcodes: Vec<String>,
        #[arg(long, value_delimiter = ',', required = true)]
        to_postcodes: Vec<String>,
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
        Command::Serve { port } => {
            commands::serve::serve(&database_url, port).await?;
        }
        Command::LoadPostcodes { onspd } => {
            commands::load_postcodes::load_postcodes(&database_url, &onspd).await?;
        }
        Command::ExportTravelTimes { from_date, to_date } => {
            commands::export_travel_times::export_travel_times(&database_url, &from_date, &to_date)
                .await?;
        }
        Command::ComputeTravelTimes {
            from_date,
            to_date,
            motis_url,
            from_postcodes,
            to_postcodes,
        } => {
            let trim = |list: Vec<String>| -> Vec<String> {
                list.iter().map(|p| p.trim().to_string()).collect()
            };
            let report = commands::compute_travel_times::compute_travel_times(
                &database_url,
                &from_date,
                &to_date,
                &motis_url,
                &trim(from_postcodes),
                &trim(to_postcodes),
            )
            .await?;
            println!("{report}");
        }
    }
    Ok(())
}
