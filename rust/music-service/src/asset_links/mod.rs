use mysql_async::prelude::FromRow;

// a link states one fact: these two assets are connected. no direction, no
// role — what a connection MEANS is already said by each side's asset_type.
// one row per pair; traversal checks both columns.
#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "asset_links")]
pub struct AssetLink {
    pub id: i32,
    pub a_id: i32,
    pub b_id: i32,
}
