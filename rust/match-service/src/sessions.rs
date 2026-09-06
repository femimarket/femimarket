use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::{ActiveModelTrait, ActiveValue::Set, DatabaseConnection};

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "sessions")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: String,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}

#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct SessionGet {
    id: String,
    note: String,
    created_at: String,
}

#[utoipa::path(post, path = "/sessions", responses((status = OK, body = String)))]
#[tracing::instrument(skip_all)]
pub async fn create(
    State(db): State<DatabaseConnection>,
) -> Result<String, (StatusCode, String)> {
    let id = uuid::Uuid::new_v4().to_string();
    ActiveModel {
        id: Set(id.clone()),
        note: Set(String::new()),
        created_at: Set(chrono::Utc::now().into()),
    }
    .insert(&db)
    .await
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    Ok(id)
}

#[utoipa::path(
    get,
    path = "/sessions/{id}",
    params(("id" = String, Path)),
    responses((status = OK, body = SessionGet), (status = NOT_FOUND, body = String)),
)]
#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path(id): Path<String>,
) -> Result<Json<SessionGet>, (StatusCode, String)> {
    let row = Entity::find_by_id(&id)
        .one(&db)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or((StatusCode::NOT_FOUND, format!("session not found: {id}")))?;
    Ok(Json(SessionGet {
        id: row.id,
        note: row.note,
        created_at: row.created_at.to_rfc3339(),
    }))
}
