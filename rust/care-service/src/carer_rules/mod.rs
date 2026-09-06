//! The carers a shift needs: one live row per required carer. Counting a
//! shift's live rows tells you how many carers it takes; each row may
//! demand a gender, null = anyone. Cancellation is the one sanctioned
//! update - changing a shift's carers means cancelling rows (note
//! mandatory) and appending new ones.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "carer_rules")]
pub struct CarerRule {
    pub id: i32,
    pub shift_id: i32,
    /// f | m - the gender this carer must be. Null = anyone.
    pub requires_gender: Option<String>,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
    /// Why it was cancelled and when; null = live.
    pub cancelled_note: Option<String>,
    pub cancelled_at: Option<chrono::NaiveDateTime>,
}
