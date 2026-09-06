use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "prices")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub offer_id: i32,
    #[sea_orm(belongs_to, from = "offer_id", to = "id")]
    pub offer: BelongsTo<super::offers::Entity>,
    pub currency_id: String,
    #[sea_orm(belongs_to, from = "currency_id", to = "id")]
    pub currency: BelongsTo<super::currencies::Entity>,
    pub amount: i64,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}
