use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::users::User;

pub(crate) async fn me(
    pool: &Pool,
    matrix_url: &str,
    token: &str,
) -> Result<Option<User>, Box<dyn std::error::Error + Send + Sync>> {
    let whoami = reqwest::Client::new()
        .get(format!("{matrix_url}/_matrix/client/v3/account/whoami"))
        .bearer_auth(token)
        .send()
        .await?;
    if !whoami.status().is_success() {
        return Ok(None);
    }
    let matrix_user_id = whoami.json::<serde_json::Value>().await?["user_id"]
        .as_str()
        .ok_or("whoami response missing user_id")?
        .to_string();
    let mut conn = pool.get_conn().await?;
    Ok(conn
        .exec_first(include_str!("me.sql"), params! { "matrix_user_id" => matrix_user_id })
        .await?)
}
