pub mod get;
pub mod list;
pub mod route;

use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(table_name = "composables", schema_name = "ui")]
#[schema(as = Composable)]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub user_id: i32,
    #[schema(value_type = super::components::Material)]
    pub component_id: String,
    #[sea_orm(belongs_to, from = "component_id", to = "id")]
    #[schema(value_type = Option<super::components::ModelEx>)]
    pub component: BelongsTo<super::components::Entity>,
    pub parent_id: Option<i32>,
    #[sea_orm(self_ref, relation_enum = "Parent", from = "ParentId", to = "Id")]
    #[schema(value_type = Option<ModelEx>, no_recursion)]
    pub parent: BelongsTo<Option<Entity>>,
    pub name: Option<String>,
    pub sort_id: String,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub column_vertical_arrangement: Option<String>,
    pub label_id: Option<i32>,
    #[sea_orm(belongs_to, from = "label_id", to = "id")]
    #[schema(value_type = Option<localisation_service::labels::ModelEx>)]
    pub label: BelongsTo<Option<localisation_service::labels::Entity>>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub text_style: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub text_color: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub text_font_family: Option<String>,
    pub outlined_text_field_value: Option<String>,
    #[sea_orm(default_value = false)]
    pub outlined_text_field_single_line: bool,
    pub outlined_text_field_min_lines: Option<i32>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub button_action: Option<String>,
    pub button_action_nav: Option<i32>,
    #[sea_orm(self_ref, relation_enum = "ButtonActionNav", from = "ButtonActionNav", to = "Id")]
    #[schema(value_type = Option<ModelEx>, no_recursion)]
    pub button_action_nav_composable: BelongsTo<Option<Entity>>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub surface_shape: Option<String>,
    #[schema(value_type = Option<super::component_values::ComponentValue>)]
    pub surface_color: Option<String>,
    #[sea_orm(has_many, from = "id", to = "parent_id", relation_enum = "Childrens")]
    #[schema(value_type = Option<Vec<ModelEx>>, no_recursion)]
    pub childrens: HasMany<crate::composables::Entity>,
    #[sea_orm(has_many, from = "id", to = "composable_id")]
    #[schema(value_type = Option<Vec<super::modifiers::ModelEx>>)]
    pub modifiers: HasMany<super::modifiers::Entity>,
    #[sea_orm(has_many, from = "id", to = "composable_id")]
    #[schema(value_type = Option<Vec<super::inputs::ModelEx>>)]
    pub inputs: HasMany<super::inputs::Entity>,
    pub note: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}

impl ActiveModelBehavior for ActiveModel {}
