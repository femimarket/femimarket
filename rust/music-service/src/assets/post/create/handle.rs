use id3::frame::PictureType;
use id3::Tag;
use mysql_async::params;
use mysql_async::prelude::*;
use tokio::io::{AsyncRead, AsyncReadExt};

use crate::assets::{Asset, AssetType};
use crate::server::AppState;

// bytes go straight to disk, never held whole in ram. rows come last: a row
// pointing at a file that never finished is the failure mode to avoid.
// the medium is sniffed from the stored bytes (extensions lie), then each
// medium runs its own arm: audio reads its id3 back — a front-cover picture
// becomes its own asset row, connected through asset_links. what a connection
// means is already said by each side's asset_type — the link itself stays dumb.
pub(crate) async fn create(
    state: &AppState,
    user_id: i32,
    name: String,
    mut reader: impl AsyncRead + Unpin,
) -> Result<Asset, Box<dyn std::error::Error + Send + Sync>> {
    let mut conn = state.pool.get_conn().await?;
    if conn
        .exec_first::<Asset, _, _>(include_str!("asset.sql"), params! { "name" => &name })
        .await?
        .is_some()
    {
        return Err(format!("asset '{name}' already exists").into());
    }

    let path = std::path::Path::new(&state.cli.fs_url).join(&name);
    let mut file = tokio::fs::File::create(&path).await?;
    tokio::io::copy(&mut reader, &mut file).await?;

    let mut head = vec![0u8; 8192];
    let read = tokio::fs::File::open(&path).await?.read(&mut head).await?;
    let kind = infer::get(&head[..read]).ok_or_else(|| format!("could not sniff the type of '{name}'"))?;

    match kind.matcher_type() {
        infer::MatcherType::Audio => {
            conn.exec_drop(
                include_str!("create.sql"),
                params! { "user_id" => user_id, "name" => &name, "asset_type" => AssetType::Song },
            )
            .await?;
            let asset = Asset {
                id: conn.last_insert_id().ok_or("asset was not inserted")? as i32,
                user_id,
                name: name.clone(),
                asset_type: AssetType::Song,
            };

            if let Ok(tag) = Tag::read_from_path(&path) {
                if let Some(picture) = tag.pictures().find(|p| p.picture_type == PictureType::CoverFront) {
                    if let Some(kind) = infer::get(&picture.data) {
                        let cover_name = format!("{name}-front_cover.{}", kind.extension());
                        if conn
                            .exec_first::<Asset, _, _>(include_str!("asset.sql"), params! { "name" => &cover_name })
                            .await?
                            .is_none()
                        {
                            tokio::fs::write(std::path::Path::new(&state.cli.fs_url).join(&cover_name), &picture.data).await?;
                            conn.exec_drop(
                                include_str!("create.sql"),
                                params! { "user_id" => user_id, "name" => &cover_name, "asset_type" => AssetType::FrontCover },
                            )
                            .await?;
                            let cover_id = conn.last_insert_id().ok_or("front cover was not inserted")? as i32;
                            conn.exec_drop(
                                include_str!("asset_link.sql"),
                                params! { "a_id" => asset.id, "b_id" => cover_id },
                            )
                            .await?;
                        }
                    }
                }
            }

            Ok(asset)
        }
        infer::MatcherType::Image => unimplemented!("image assets"),
        infer::MatcherType::Video => unimplemented!("video assets"),
        other => Err(format!("'{name}' is {other:?}, not a supported medium").into()),
    }
}
