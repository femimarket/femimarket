use mysql_async::prelude::*;
use mysql_async::Pool;

use crate::users::User;

pub(crate) async fn service_users(pool: &Pool) -> Result<Vec<User>, mysql_async::Error> {
    let mut conn = pool.get_conn().await?;
    conn.query(include_str!("service_users.sql")).await
}
