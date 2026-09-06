use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(table_name = "inputs", schema_name = "ui")]
#[schema(as = Input)]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub user_id: i32,
    pub composable_id: i32,
    #[sea_orm(belongs_to, from = "composable_id", to = "id")]
    #[schema(value_type = Option<super::composables::ModelEx>, no_recursion)]
    pub composable: BelongsTo<super::composables::Entity>,
    pub value: String,
    pub note: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}

impl ActiveModelBehavior for ActiveModel {}
