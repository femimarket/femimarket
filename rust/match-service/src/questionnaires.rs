use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::{DatabaseConnection, EntityLoaderTrait};

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "questionnaires")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub name: String,
    #[sea_orm(has_many, via = "questionnaire_questions")]
    pub questions: HasMany<super::questions::Entity>,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}

#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct QuestionnaireList {
    id: i32,
    name: String,
    note: String,
    created_at: String,
}

#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path(id): Path<i32>,
) -> Result<Json<ModelEx>, (StatusCode, String)> {
    let mut questionnaire = Entity::load()
        .filter_by_id(id)
        .with(super::questions::Entity)
        .one(&db)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or((StatusCode::NOT_FOUND, format!("questionnaire not found: {id}")))?;
    let labels = super::labels::Entity::load()
        .filter(
            super::labels::Column::Id.is_in(
                questionnaire
                    .questions
                    .iter()
                    .flat_map(|question| [Some(question.question_label_id), question.pretext_label_id])
                    .flatten()
                    .collect::<Vec<_>>(),
            ),
        )
        .with(super::translations::Entity)
        .all(&db)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    if let HasMany::Loaded(questions) = &mut questionnaire.questions {
        for question in questions.iter_mut() {
            if let Some(label) = labels.iter().find(|label| label.id == question.question_label_id) {
                question.question_label = BelongsTo::Loaded(Box::new(label.clone()));
            }
            if let Some(label) = labels.iter().find(|label| Some(label.id) == question.pretext_label_id) {
                question.pretext_label = BelongsTo::Loaded(Some(Box::new(label.clone())));
            }
        }
    }
    Ok(Json(questionnaire))
}

#[utoipa::path(get, path = "/questionnaires", responses((status = OK, body = Vec<QuestionnaireList>)))]
#[tracing::instrument(skip_all)]
pub async fn list(
    State(db): State<DatabaseConnection>,
) -> Result<Json<Vec<QuestionnaireList>>, (StatusCode, String)> {
    Ok(Json(
        Entity::find()
            .all(&db)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
            .into_iter()
            .map(|row| QuestionnaireList {
                id: row.id,
                name: row.name,
                note: row.note,
                created_at: row.created_at.to_rfc3339(),
            })
            .collect(),
    ))
}
