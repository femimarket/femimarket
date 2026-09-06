use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::users::User;

pub(crate) async fn me(pool: &Pool, matrix_id: i32) -> Result<Vec<User>, mysql_async::Error> {
    let mut conn = pool.get_conn().await?;
    conn.exec(include_str!("me.sql"), params! { "matrix_id" => matrix_id }).await
}
