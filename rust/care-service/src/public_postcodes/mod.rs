//! The public face of a client postcode: a real, existing postcode one
//! letter different, shown by jobs.femi.market instead of the client's
//! own. One row per real postcode, written once - a mask never changes,
//! so listings stay coherent across weeks.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "public_postcodes")]
pub struct PublicPostcode {
    pub postcode_id: String,
    pub public_postcode_id: String,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
