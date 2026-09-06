use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use sea_orm::entity::prelude::*;
use sea_orm::{DatabaseConnection, EntityLoaderTrait};
use crate::{labels, langs, translation_bold_spans};

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(table_name = "translations", schema_name = "localisation")]
#[schema(as = Translation)]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub label_id: i32,
    #[sea_orm(belongs_to, from = "label_id", to = "id")]
    #[schema(value_type = Option<super::labels::ModelEx>, no_recursion)]
    pub label: BelongsTo<super::labels::Entity>,
    pub lang_id: String,
    #[sea_orm(belongs_to, from = "lang_id", to = "id", on_update = "Cascade")]
    #[schema(value_type = Option<super::langs::ModelEx>)]
    pub lang: BelongsTo<super::langs::Entity>,
    pub text: String,
    #[sea_orm(has_many)]
    #[schema(value_type = Option<Vec<super::translation_bold_spans::ModelEx>>)]
    pub translation_bold_spans: HasMany<super::translation_bold_spans::Entity>,
    pub note: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}

impl ActiveModelBehavior for ActiveModel {}

#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path(id): Path<i32>,
) -> Result<Json<ModelEx>, (StatusCode, String)> {
    Ok(Json(
        Entity::load()
            .filter_by_id(id)
            .with(labels::Entity)
            .with(langs::Entity)
            .with(translation_bold_spans::Entity)
            .one(&db)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
            .ok_or((StatusCode::NOT_FOUND, format!("translation not found: {id}")))?,
    ))
}
