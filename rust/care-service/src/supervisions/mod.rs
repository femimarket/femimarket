//! Dated, append-only: while a row spans the solve date, this carer may only
//! take shifts where one of their supervisors (supervisors table) is
//! alongside. Induction, return-to-work, competency review - the reason goes
//! in the note; the constraint is the same.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "supervisions")]
pub struct Supervision {
    pub id: i32,
    /// The supervised carer.
    pub staff_id: i32,
    pub from_date: chrono::NaiveDate,
    pub to_date: chrono::NaiveDate,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
