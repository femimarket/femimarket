//! One row per solve run: the date it solved, how long it took, and the
//! note - which records how the run ended (optimal, timed-out draft,
//! infeasible) and anything else worth saying about it - the frame around
//! a set of assignments.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "rota")]
pub struct Rota {
    pub id: i32,
    pub from_date: chrono::NaiveDate,
    pub to_date: chrono::NaiveDate,
    /// Wall-clock seconds the solve took.
    pub wall_seconds: Option<f64>,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
