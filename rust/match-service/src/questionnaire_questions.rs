use sea_orm::entity::prelude::*;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "questionnaire_questions")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub questionnaire_id: i32,
    #[sea_orm(belongs_to, from = "questionnaire_id", to = "id")]
    pub questionnaire: BelongsTo<super::questionnaires::Entity>,
    pub question_id: i32,
    #[sea_orm(belongs_to, from = "question_id", to = "id")]
    pub question: BelongsTo<super::questions::Entity>,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}
