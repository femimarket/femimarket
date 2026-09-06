use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::users::User;

pub(crate) async fn id(pool: &Pool, user: User) -> Result<User, mysql_async::Error> {
    let mut conn = pool.get_conn().await?;
    conn.exec_drop(
        include_str!("id.sql"),
        params! {
            "user_type" => user.user_type.clone(),
            "first_name" => &user.first_name,
            "last_name" => &user.last_name,
            "id" => &user.id,
        },
    )
    .await?;
    Ok(user)
}
