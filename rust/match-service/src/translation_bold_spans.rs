use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "translation_bold_spans")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub translation_id: i32,
    #[sea_orm(belongs_to, from = "translation_id", to = "id")]
    pub translation: BelongsTo<super::translations::Entity>,
    pub start: i32,
    pub end: i32,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}
