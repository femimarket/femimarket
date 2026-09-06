//! Which gender of carer a shift requires: a hard wall for the solver,
//! pinned per shift like its other rules. Append-only - changing means a
//! new rule (note mandatory) and repointing the shift.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "gender_rules")]
pub struct GenderRule {
    pub id: i32,
    /// f | m - the gender the carer must be.
    pub requires_gender: String,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
