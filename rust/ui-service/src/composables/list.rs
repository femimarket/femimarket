use sea_orm::{ColumnTrait, DatabaseConnection, DbErr, EntityTrait, QueryFilter};

use super::{Column, Entity, Model};

pub async fn list(db: &DatabaseConnection) -> Result<Vec<Model>, DbErr> {
    Entity::find()
        .filter(Column::Name.is_not_null())
        .filter(Column::Name.ne(""))
        .all(db)
        .await
}
