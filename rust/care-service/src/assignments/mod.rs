//! A rota's content: shifts assigned to workers, one row per carer per
//! shift - a double-up is two rows. Every row belongs to the solve run
//! (rota) that produced it, so past rotas and alternative options survive
//! whole - the same shift may appear across many rotas. start_time is the
//! solver's chosen moment within the shift's time-rule window; end_time is
//! when the carer walks out - the duration from start unless an
//! early_leave_rules row let them go sooner.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "assignments")]
pub struct Assignment {
    pub rota_id: i32,
    pub shift_id: i32,
    pub staff_id: i32,
    pub assignment_type_id: String,
    pub start_time: chrono::NaiveTime,
    pub end_time: chrono::NaiveTime,
    pub note: String,
    /// Who asserted this fact, and when.
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
