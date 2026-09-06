use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::{DatabaseConnection, EntityLoaderTrait, QueryOrder};

use crate::questionnaire_questions;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "questions")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub pretext_label_id: Option<i32>,
    #[sea_orm(belongs_to, from = "pretext_label_id", to = "id", relation_enum = "PretextLabel")]
    pub pretext_label: BelongsTo<Option<super::labels::Entity>>,
    pub question_label_id: i32,
    #[sea_orm(belongs_to, from = "question_label_id", to = "id")]
    pub question_label: BelongsTo<super::labels::Entity>,
    pub code_snippet: Option<String>,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}

#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct QuestionGet {
    id: i32,
    pretext_label_id: Option<i32>,
    question_label_id: i32,
    code_snippet: Option<String>,
    note: String,
    created_at: String,
}

#[utoipa::path(
    get,
    path = "/questionnaires/{questionnaire_id}/questions",
    params(("questionnaire_id" = i32, Path)),
    responses((status = OK, body = Vec<QuestionGet>)),
)]
#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path(questionnaire_id): Path<i32>,
) -> Result<Json<Vec<QuestionGet>>, (StatusCode, String)> {
    Ok(Json(
        questionnaire_questions::Entity::find()
            .filter(questionnaire_questions::Column::QuestionnaireId.eq(questionnaire_id))
            .order_by_asc(questionnaire_questions::Column::Id)
            .find_also_related(Entity)
            .all(&db)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
            .into_iter()
            .filter_map(|(_, question)| question)
            .map(|question| QuestionGet {
                id: question.id,
                pretext_label_id: question.pretext_label_id,
                question_label_id: question.question_label_id,
                code_snippet: question.code_snippet,
                note: question.note,
                created_at: question.created_at.to_rfc3339(),
            })
            .collect(),
    ))
}
