pub mod post;
pub mod route;

use mysql_async::prelude::{FromRow, FromValue};

#[derive(Clone, Debug, PartialEq, Eq, FromValue, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", is_string)]
pub enum AssetType {
    Song,
    Instrumental,
    FrontCover,
    Protagonist,
    Scene,
    Video,
}

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "assets")]
pub struct Asset {
    pub id: i32,
    pub user_id: i32,
    // the filename in the file store — how every other service addresses the asset
    pub name: String,
    pub asset_type: AssetType,
}

// #[sea_orm::model]
// #[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize, utoipa::ToSchema)]
// #[sea_orm(table_name = "audios", schema_name = "music")]
// #[schema(as = Audio)]
// pub struct Model {
//     #[sea_orm(primary_key)]
//     pub id: i32,
//     pub user_id: i32,
//     #[sea_orm(belongs_to, from = "user_id", to = "id")]
//     #[schema(value_type = Option<user_service::users::ModelEx>)]
//     pub user: BelongsTo<user_service::users::Entity>,
//     // the filename in the file store — how every other service addresses the song
//     pub name: String,
//     // a label, not an entity: nothing is ever authored against a project
//     pub project: String,
//     pub image: String,
//     pub genre: Option<String>,
//     pub lyrics: Option<String>,
//     pub edited_lyrics: Option<String>,
//     pub protagonist: Option<String>,
//     pub social_media_blueprint: Option<String>,
//     // the final cut, once one exists
//     pub video: Option<String>,
//     pub uid: Option<String>,
//     pub liked: Option<bool>,
//     pub backed_up: bool,
//     pub error: Option<String>,
// }
