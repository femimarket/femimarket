//! What a minute costs when a call is pushed outside its normal start
//! window into the stretch the client has permissioned. Charged per minute
//! beyond earliest_start or latest_start, so the solver reaches there only
//! when it buys something worth more.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "weight_penalty_per_minute_permissioned_stretch")]
pub struct WeightPenaltyPerMinutePermissionedStretch {
    pub id: i32,
    pub penalty: i32,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
