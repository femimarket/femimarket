use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "contents")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub offer_id: i32,
    #[sea_orm(belongs_to, from = "offer_id", to = "id")]
    pub offer: BelongsTo<super::offers::Entity>,
    #[sea_orm(default_value = false)]
    pub title: bool,
    #[sea_orm(default_value = false)]
    pub list: bool,
    pub label_id: i32,
    #[sea_orm(belongs_to, from = "label_id", to = "id")]
    pub label: BelongsTo<super::labels::Entity>,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}
