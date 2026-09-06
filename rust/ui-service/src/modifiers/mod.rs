use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(table_name = "modifiers", schema_name = "ui")]
#[schema(as = Modifier)]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub composable_id: i32,
    #[sea_orm(belongs_to, from = "composable_id", to = "id")]
    #[schema(value_type = Option<super::composables::ModelEx>, no_recursion)]
    pub composable: BelongsTo<super::composables::Entity>,
    #[sea_orm(default_value = false)]
    pub fill_max_size: bool,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub background: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub vertical_scroll: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub horizontal_scroll: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub wrap_content_width: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub width_in_max: Option<String>,
    #[sea_orm(default_value = false)]
    pub fill_max_width: bool,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub padding: Option<String>,
    pub note: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}

impl ActiveModelBehavior for ActiveModel {}
