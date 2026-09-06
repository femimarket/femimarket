pub mod get_clients;
pub mod get_staffs;
pub mod publish_postcodes;
pub mod publish_rota;
pub mod serve;
pub mod solve;

use mysql_async::prelude::*;
use mysql_async::{params, Conn};

pub async fn resolve_postcode(
    conn: &mut Conn,
    raw: &str,
    owner: &str,
) -> Result<String, Box<dyn std::error::Error>> {
    let key: String = raw
        .chars()
        .filter(|c| !c.is_whitespace())
        .collect::<String>()
        .to_uppercase();
    if key.len() < 5 || !key.is_ascii() {
        return Err(format!("{owner}: postcode {raw:?} is not a postcode").into());
    }
    let pcds = format!("{} {}", &key[..key.len() - 3], &key[key.len() - 3..]);
    let found: Option<String> = conn
        .exec_first(include_str!("postcode.sql"), params! { "id" => &pcds })
        .await?;
    if found.is_none() {
        return Err(format!(
            "{owner}: postcode {raw:?} not found in postcodes - load ONSPD or fix the source"
        )
        .into());
    }
    Ok(pcds)
}
