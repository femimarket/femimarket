use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::{ActiveModelTrait, ActiveValue::Set, DatabaseConnection};

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "applicants")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: String,
    pub session_id: String,
    #[sea_orm(belongs_to, from = "session_id", to = "id")]
    pub session: BelongsTo<super::sessions::Entity>,
    pub first_name: String,
    pub last_name: String,
    pub linkedin: String,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}

#[derive(serde::Deserialize, utoipa::ToSchema)]
pub struct ApplicantCreate {
    first_name: String,
    last_name: String,
    linkedin: String,
}

#[utoipa::path(
    post,
    path = "/sessions/{session_id}/applicant",
    params(("session_id" = String, Path)),
    request_body = ApplicantCreate,
    responses((status = NO_CONTENT), (status = UNPROCESSABLE_ENTITY, body = String)),
)]
#[tracing::instrument(skip_all)]
pub async fn create(
    State(db): State<DatabaseConnection>,
    Path(session_id): Path<String>,
    Json(applicant): Json<ApplicantCreate>,
) -> Result<StatusCode, (StatusCode, String)> {
    if applicant.first_name.trim().is_empty() {
        return Err((StatusCode::UNPROCESSABLE_ENTITY, "first_name is empty".to_string()));
    }
    if applicant.last_name.trim().is_empty() {
        return Err((StatusCode::UNPROCESSABLE_ENTITY, "last_name is empty".to_string()));
    }
    if applicant.linkedin.trim().is_empty() {
        return Err((StatusCode::UNPROCESSABLE_ENTITY, "linkedin is empty".to_string()));
    }
    ActiveModel {
        id: Set(uuid::Uuid::new_v4().to_string()),
        session_id: Set(session_id),
        first_name: Set(applicant.first_name.trim().to_string()),
        last_name: Set(applicant.last_name.trim().to_string()),
        linkedin: Set(applicant.linkedin.trim().to_string()),
        note: Set(String::new()),
        created_at: Set(chrono::Utc::now().into()),
    }
    .insert(&db)
    .await
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    Ok(StatusCode::NO_CONTENT)
}
