use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(table_name = "labels", schema_name = "localisation")]
#[schema(as = Label)]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub name: String,
    #[sea_orm(has_many, from = "id", to = "label_id")]
    #[schema(value_type = Option<Vec<super::translations::ModelEx>>)]
    pub translations: HasMany<super::translations::Entity>,
    pub note: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}

impl ActiveModelBehavior for ActiveModel {}
