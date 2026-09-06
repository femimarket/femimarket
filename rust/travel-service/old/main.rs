use clap::{Parser, Subcommand};

mod streets;
mod sync;
mod tables;
mod timetable;
mod transit;

#[derive(Parser)]
#[command(name = "travel-service", about = "Travel time engine")]
struct Cli {
    #[arg(long, env = "DATABASE_URL", global = true)]
    database_url: Option<String>,
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    Sync {
        #[arg(long)]
        from_date: String,
        #[arg(long)]
        to_date: String,
        #[arg(long, env = "OSM")]
        osm: String,
        #[arg(long = "gtfs", required = true)]
        gtfs: Vec<String>,
    },
    ProbeStreet {
        #[arg(long)]
        from: String,
        #[arg(long)]
        to: String,
        #[arg(long)]
        mode: String,
        #[arg(long, env = "OSM")]
        osm: String,
    },
    Probe {
        #[arg(long)]
        from: String,
        #[arg(long)]
        to: String,
        #[arg(long)]
        date: String,
        #[arg(long)]
        at: String,
        #[arg(long, env = "OSM")]
        osm: String,
        #[arg(long = "gtfs", required = true)]
        gtfs: Vec<String>,
    },
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();
    let database_url = cli
        .database_url
        .ok_or("--database-url (or DATABASE_URL) is required")?;
    match cli.command {
        Command::Sync {
            from_date,
            to_date,
            osm,
            gtfs,
        } => {
            let report = sync::sync(&database_url, &from_date, &to_date, &osm, &gtfs).await?;
            println!("{report}");
        }
        Command::ProbeStreet { from, to, mode, osm } => {
            sync::probe_street(&database_url, &from, &to, &mode, &osm).await?;
        }
        Command::Probe {
            from,
            to,
            date,
            at,
            osm,
            gtfs,
        } => {
            sync::probe(&database_url, &from, &to, &date, &at, &osm, &gtfs).await?;
        }
    }
    Ok(())
}
