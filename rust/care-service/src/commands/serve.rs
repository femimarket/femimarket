use crate::server::start_server;

pub async fn serve(database_url: &str, port: u16) -> Result<(), Box<dyn std::error::Error>> {
    let app = start_server(database_url).await?;
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", port)).await?;
    tracing::info!("serving on {}", listener.local_addr()?);
    axum::serve(listener, app).await?;
    Ok(())
}
