use sea_orm::entity::prelude::*;
use sea_orm::{DatabaseConnection, DbErr, EntityLoaderTrait};

use super::{Column, Entity, ModelEx};

pub async fn get(db: &DatabaseConnection, id: i32) -> Result<Option<ModelEx>, DbErr> {
    let Some(mut composable) = Entity::load()
        .filter_by_id(id)
        .with(crate::modifiers::Entity)
        .with(crate::inputs::Entity)
        .with((localisation_service::labels::Entity, localisation_service::translations::Entity))
        .one(db)
        .await?
    else {
        return Ok(None);
    };
    if let BelongsTo::Loaded(Some(label)) = &mut composable.label {
        if let HasMany::Loaded(translations) = &mut label.translations {
            let spans = localisation_service::translation_bold_spans::Entity::load()
                .filter(
                    localisation_service::translation_bold_spans::Column::TranslationId
                        .is_in(translations.iter().map(|translation| translation.id).collect::<Vec<_>>()),
                )
                .all(db)
                .await?;
            for translation in translations.iter_mut() {
                translation.translation_bold_spans = HasMany::Loaded(
                    spans.iter().filter(|span| span.translation_id == translation.id).cloned().collect(),
                );
            }
        }
    }
    let mut childrens = Vec::new();
    for child in Entity::load().filter(Column::ParentId.eq(composable.id)).all(db).await? {
        if let Some(child) = Box::pin(get(db, child.id)).await? {
            childrens.push(child);
        }
    }
    composable.childrens = HasMany::Loaded(childrens);
    Ok(Some(composable))
}
