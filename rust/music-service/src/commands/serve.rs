use std::net::SocketAddr;
use std::sync::OnceLock;

pub static WEB_DIR_LOCK: OnceLock<String> = OnceLock::new();
pub static ALIBABA_STUDIO_API_LOCK: OnceLock<String> = OnceLock::new();
pub static ALIBABA_STUDIO_BASE_LOCK: OnceLock<String> = OnceLock::new();
pub static FAL_KEY_LOCK: OnceLock<String> = OnceLock::new();
pub static COMFY_KEY_LOCK: OnceLock<String> = OnceLock::new();
pub static NGROK_AI_LOCK: OnceLock<String> = OnceLock::new();
pub static NGROK_AI_URL_LOCK: OnceLock<String> = OnceLock::new();
pub static QWEN_ASR_0_6B_DIR_LOCK: OnceLock<String> = OnceLock::new();

pub static COMFY_FLUX2_KLEIN_I2I_WORKFLOW_LOCK: OnceLock<String> = OnceLock::new();
pub static COMFY_FLUX2_DEV_I2I_WORKFLOW_LOCK: OnceLock<String> = OnceLock::new();
pub static COMFY_LTX2_3A2V_WORKFLOW_LOCK: OnceLock<String> = OnceLock::new();
pub static SSLPUB_LOCK: OnceLock<String> = OnceLock::new();
pub static SSLPRIV_LOCK: OnceLock<String> = OnceLock::new();

pub async fn serve(
    cli: crate::server::Cli,
    web_dir: String,
    alibaba_studio_api: String,
    alibaba_studio_base: String,
    fal_key: String,
    comfy_key: String,
    ngrok_ai: String,
    ngrok_ai_url: String,
    qwen_asr_0_6b_dir: String,
    comfy_flux2_klein_i2i_workflow: String,
    comfy_flux2_dev_i2i_workflow: String,
    comfy_ltx2_3a2v_workflow: String,
    sslpub: String,
    sslpriv: String,
) -> Result<(), Box<dyn std::error::Error>> {
    // Both ring and aws-lc-rs are in the dependency tree, so rustls can't
    // auto-select a crypto provider — pick one before any TLS config is built.
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("install rustls crypto provider");

    WEB_DIR_LOCK.set(web_dir).expect("web_dir already set");
    ALIBABA_STUDIO_API_LOCK
        .set(alibaba_studio_api)
        .expect("alibaba_studio_api already set");
    ALIBABA_STUDIO_BASE_LOCK
        .set(alibaba_studio_base)
        .expect("alibaba_studio_base already set");
    FAL_KEY_LOCK.set(fal_key).expect("fal_key already set");
    COMFY_KEY_LOCK.set(comfy_key).expect("comfy_key already set");
    NGROK_AI_LOCK.set(ngrok_ai).expect("ngrok_ai already set");
    NGROK_AI_URL_LOCK
        .set(ngrok_ai_url)
        .expect("ngrok_ai_url already set");
    QWEN_ASR_0_6B_DIR_LOCK
        .set(qwen_asr_0_6b_dir)
        .expect("qwen_asr_0_6b_dir already set");
    COMFY_LTX2_3A2V_WORKFLOW_LOCK
        .set(tokio::fs::read_to_string(&comfy_ltx2_3a2v_workflow).await.expect("reading comfy_ltx2_3a2v_workflow"))
        .expect("comfy_ltx2_3a2v_workflow already set");
    COMFY_FLUX2_KLEIN_I2I_WORKFLOW_LOCK
        .set(tokio::fs::read_to_string(&comfy_flux2_klein_i2i_workflow).await.expect("reading comfy_flux2_klein_i2i_workflow"))
        .expect("comfy_flux2_klein_i2i_workflow already set");
    COMFY_FLUX2_DEV_I2I_WORKFLOW_LOCK
        .set(tokio::fs::read_to_string(&comfy_flux2_dev_i2i_workflow).await.expect("reading comfy_flux2_dev_i2i_workflow"))
        .expect("comfy_flux2_dev_i2i_workflow already set");
    SSLPUB_LOCK.set(sslpub).expect("sslpub already set");
    SSLPRIV_LOCK.set(sslpriv).expect("sslpriv already set");

    let app = crate::server::start_server(cli).await?;
    let addr = SocketAddr::from(([0, 0, 0, 0], std::env::var("PORT").unwrap_or_else(|_| "9000".to_string()).parse().expect("parse PORT")));
    let tls = axum_server::tls_rustls::RustlsConfig::from_pem_file(
        SSLPUB_LOCK.get().unwrap(),
        SSLPRIV_LOCK.get().unwrap(),
    )
    .await
    .expect("load TLS cert/key");
    tracing::info!("listening (https) on {}", addr);
    axum_server::bind(addr)
        .serve(app.into_make_service())
        .await
        .expect("serve");
    Ok(())
}
