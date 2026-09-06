use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::{ActiveModelTrait, ActiveValue::Set, DatabaseConnection};

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "session_answers")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub session_id: String,
    #[sea_orm(belongs_to, from = "session_id", to = "id")]
    pub session: BelongsTo<super::sessions::Entity>,
    pub question_id: i32,
    #[sea_orm(belongs_to, from = "question_id", to = "id")]
    pub question: BelongsTo<super::questions::Entity>,
    pub answer: String,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}

#[derive(serde::Deserialize, utoipa::ToSchema)]
pub struct SessionAnswerCreate {
    question_id: i32,
    answer: String,
}

#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct SessionAnswerGet {
    id: i32,
    session_id: String,
    question_id: i32,
    answer: String,
    note: String,
    created_at: String,
}

#[utoipa::path(
    get,
    path = "/sessions/{session_id}/answers",
    params(("session_id" = String, Path)),
    responses((status = OK, body = Vec<SessionAnswerGet>)),
)]
#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path(session_id): Path<String>,
) -> Result<Json<Vec<SessionAnswerGet>>, (StatusCode, String)> {
    Ok(Json(
        Entity::find()
            .filter(Column::SessionId.eq(session_id))
            .all(&db)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
            .into_iter()
            .map(|row| SessionAnswerGet {
                id: row.id,
                session_id: row.session_id,
                question_id: row.question_id,
                answer: row.answer,
                note: row.note,
                created_at: row.created_at.to_rfc3339(),
            })
            .collect(),
    ))
}

#[utoipa::path(
    post,
    path = "/sessions/{session_id}/answers",
    params(("session_id" = String, Path)),
    request_body = Vec<SessionAnswerCreate>,
    responses((status = NO_CONTENT), (status = UNPROCESSABLE_ENTITY, body = String)),
)]
#[tracing::instrument(skip_all)]
pub async fn create(
    State(db): State<DatabaseConnection>,
    Path(session_id): Path<String>,
    Json(answers): Json<Vec<SessionAnswerCreate>>,
) -> Result<StatusCode, (StatusCode, String)> {
    for answer in &answers {
        if answer.answer.trim().is_empty() {
            return Err((
                StatusCode::UNPROCESSABLE_ENTITY,
                format!("answer for question {} is empty", answer.question_id),
            ));
        }
    }
    for answer in answers {
        ActiveModel {
            session_id: Set(session_id.clone()),
            question_id: Set(answer.question_id),
            answer: Set(answer.answer.trim().to_string()),
            note: Set(String::new()),
            created_at: Set(chrono::Utc::now().into()),
            ..Default::default()
        }
        .insert(&db)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    }
    Ok(StatusCode::NO_CONTENT)
}
