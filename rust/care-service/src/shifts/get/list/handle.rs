use mysql_async::prelude::*;
use mysql_async::{params, Pool};

use crate::carer_rules::CarerRule;
use crate::public_postcodes::PublicPostcode;
use crate::shifts::ShiftTimeRuleClient;

#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct PublicShift {
    id: i32,
    on_date: chrono::NaiveDate,
    earliest_start: String,
    latest_start: String,
    duration_mins: i64,
    carers_required: i64,
    requires_genders: Vec<Option<String>>,
    public_postcode_id: String,
}

pub(crate) async fn list(pool: &Pool) -> Result<Vec<PublicShift>, Box<dyn std::error::Error + Send + Sync>> {
    let mut conn = pool.get_conn().await?;
    let shifts: Vec<ShiftTimeRuleClient> = conn
        .exec(include_str!("list.sql"), params! { "on_date" => chrono::Utc::now().date_naive() })
        .await?;
    let carer_rules: Vec<CarerRule> = conn.query(include_str!("carer_rules.sql")).await?;
    let public_postcodes: Vec<PublicPostcode> = conn.query(include_str!("public_postcodes.sql")).await?;
    shifts.iter()
        .map(|s| {
            let public_postcode_id = public_postcodes.iter()
                .find(|m| m.postcode_id == s.postcode_id())
                .map(|m| m.public_postcode_id.clone())
                .ok_or_else(|| format!("no public postcode for shift {}", s.id))?;
            Ok(PublicShift {
                id: s.id,
                on_date: s.on_date,
                earliest_start: s.earliest_start.to_string(),
                latest_start: s.latest_start.to_string(),
                duration_mins: s.duration_mins(),
                carers_required: s.carers_required(&carer_rules),
                requires_genders: carer_rules.iter()
                    .filter(|r| r.shift_id == s.id)
                    .map(|r| r.requires_gender.clone())
                    .collect(),
                public_postcode_id,
            })
        })
        .collect()
}
