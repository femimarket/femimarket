use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, EnumIter, DeriveActiveEnum, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(rs_type = "String", db_type = "String(StringLen::None)")]
pub enum Material {
    #[sea_orm(string_value = "Column")]
    Column,
    #[sea_orm(string_value = "Text")]
    Text,
    #[sea_orm(string_value = "OutlinedTextField")]
    OutlinedTextField,
    #[sea_orm(string_value = "Button")]
    Button,
    #[sea_orm(string_value = "Surface")]
    Surface,
}

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(table_name = "components", schema_name = "ui")]
#[schema(as = Component)]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: String,
}

impl ActiveModelBehavior for ActiveModel {}
