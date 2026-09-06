use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::users::User;

pub(crate) async fn id(pool: &Pool, id: &str) -> Result<Option<User>, mysql_async::Error> {
    let mut conn = pool.get_conn().await?;
    conn.exec_first(include_str!("id.sql"), params! { "id" => id }).await
}
