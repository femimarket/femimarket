use crate::server::{start_server, Cli};

pub async fn serve(cli: Cli, port: u16) -> Result<(), Box<dyn std::error::Error>> {
    let app = start_server(cli).await?;
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", port)).await?;
    tracing::info!("serving on {}", listener.local_addr()?);
    axum::serve(listener, app).await?;
    Ok(())
}
