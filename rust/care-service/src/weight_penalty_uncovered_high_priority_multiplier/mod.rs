use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "weight_penalty_uncovered_high_priority_multiplier")]
pub struct WeightPenaltyUncoveredHighPriorityMultiplier {
    pub id: i32,
    pub multiplier: f64,
    pub note: String,
    pub user_id: String,
    pub created_at: chrono::NaiveDateTime,
}
