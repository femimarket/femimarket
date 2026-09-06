use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::users::User;

pub(crate) async fn create(pool: &Pool, user: User) -> Result<User, mysql_async::Error> {
    let user = User {
        id: uuid::Uuid::new_v4().to_string(),
        created_at: chrono::Utc::now().naive_utc(),
        ..user
    };
    let mut conn = pool.get_conn().await?;
    conn.exec_drop(
        include_str!("create.sql"),
        params! {
            "id" => &user.id,
            "user_type" => user.user_type.clone(),
            "first_name" => &user.first_name,
            "last_name" => &user.last_name,
            "matrix_id" => user.matrix_id,
            "parent_id" => &user.parent_id,
            "created_at" => user.created_at,
        },
    )
    .await?;
    Ok(user)
}
