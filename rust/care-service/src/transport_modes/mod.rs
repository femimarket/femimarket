//! How a carer travels - a reference table, not a stringly-typed enum, so
//! the semantics live as data ("public includes walking; anyone not driving
//! is taxi-eligible") and a typo'd mode is a foreign key violation.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "transport_modes")]
pub struct TransportMode {
    /// The mode itself: "car", "public", "bike".
    pub id: String,
    pub note: String,
}
