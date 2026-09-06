use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::users::get::UserGetQuery;
use crate::users::User;

pub(crate) async fn list(pool: &Pool, query: UserGetQuery) -> Result<Vec<User>, mysql_async::Error> {
    let mut conn = pool.get_conn().await?;
    conn.exec(include_str!("list.sql"), params! { "matrix_id" => query.matrix_id }).await
}
