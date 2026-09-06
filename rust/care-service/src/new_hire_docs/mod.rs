//! Files uploaded during onboarding - passport, certificates, DBS paper.
//! The binary lives on disk; the row points at it. Uploads happen after
//! the account exists, so user_id is the uploader.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "new_hire_docs")]
pub struct NewHireDoc {
    pub id: i32,
    pub candidate_id: String,
    pub kind: String,
    pub file: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
