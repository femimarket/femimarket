use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "weight_solver_workers")]
pub struct WeightSolverWorkers {
    pub id: i32,
    pub workers: i32,
    pub note: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
