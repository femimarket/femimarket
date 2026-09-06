//! chat-service: the Matrix bot lives here.
//!
//!     chat-service run --user-id @bot:example.org --password ... --room !abc:example.org
//!
//! This service owns everything Matrix: the session, the room, and the
//! mapping from Matrix senders to care-service user handles. The care domain
//! knows nothing about any of it.
//!
//! Skeleton: logs in, syncs, and logs every message in the configured room.
//! The brain - sender mapping, LLM, database access - is not wired yet.

use clap::{Parser, Subcommand};
use matrix_sdk::{
    Client, Room, RoomState,
    config::SyncSettings,
    ruma::{OwnedRoomId, OwnedUserId, events::room::message::SyncRoomMessageEvent},
};

#[derive(Parser)]
#[command(name = "chat-service", about = "Matrix interface to the care domain")]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Log in and listen in the configured room.
    Run {
        /// The bot's own Matrix account, e.g. @bot:example.org
        #[arg(long, env = "MATRIX_USER_ID")]
        user_id: OwnedUserId,
        #[arg(long, env = "MATRIX_PASSWORD")]
        password: String,
        /// The one room the bot serves, e.g. !abc:example.org
        #[arg(long, env = "MATRIX_ROOM_ID")]
        room_id: OwnedRoomId,
    },
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();
    match cli.command {
        Command::Run {
            user_id,
            password,
            room_id,
        } => run(user_id, password, room_id).await,
    }
}

async fn run(
    user_id: OwnedUserId,
    password: String,
    room_id: OwnedRoomId,
) -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::builder()
        .server_name(user_id.server_name())
        .build()
        .await?;

    client
        .matrix_auth()
        .login_username(&user_id, &password)
        .send()
        .await?;
    println!("logged in as {user_id}");

    let own_id = user_id.clone();
    client.add_event_handler(move |event: SyncRoomMessageEvent, room: Room| {
        let own_id = own_id.clone();
        let room_id = room_id.clone();
        async move {
            if room.state() != RoomState::Joined || room.room_id() != room_id {
                return;
            }
            let Some(original) = event.as_original() else {
                return; // redactions etc.
            };
            if original.sender == own_id {
                return; // never talk to ourselves
            }
            // The brain goes here: map sender -> care-service user handle,
            // hand the text + handle to the agent, reply with its answer.
            println!("[{}] {}: {}", room.room_id(), original.sender, original.content.body());
        }
    });

    println!("listening");
    client.sync(SyncSettings::default()).await?;
    Ok(())
}
