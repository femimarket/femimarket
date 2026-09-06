use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::DatabaseConnection;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "translations")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub label_id: i32,
    #[sea_orm(belongs_to, from = "label_id", to = "id")]
    pub label: BelongsTo<super::labels::Entity>,
    pub lang_id: String,
    #[sea_orm(belongs_to, from = "lang_id", to = "id")]
    pub lang: BelongsTo<super::langs::Entity>,
    pub text: String,
    #[sea_orm(has_many)]
    pub translation_bold_spans: HasMany<super::translation_bold_spans::Entity>,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}

#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct TranslationGet {
    id: i32,
    label_id: i32,
    lang_id: String,
    text: String,
    note: String,
    created_at: String,
}

#[utoipa::path(get, path = "/translations", responses((status = OK, body = Vec<TranslationGet>)))]
#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
) -> Result<Json<Vec<TranslationGet>>, (StatusCode, String)> {
    Ok(Json(
        Entity::find()
            .all(&db)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
            .into_iter()
            .map(|row| TranslationGet {
                id: row.id,
                label_id: row.label_id,
                lang_id: row.lang_id,
                text: row.text,
                note: row.note,
                created_at: row.created_at.to_rfc3339(),
            })
            .collect(),
    ))
}
