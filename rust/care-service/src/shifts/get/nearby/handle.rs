use axum::http::StatusCode;
use futures::stream::{StreamExt, TryStreamExt};
use mysql_async::prelude::*;
use mysql_async::{params, Pool};
use std::collections::HashSet;
use travel_service::travel_times::TravelTime;

const CONCURRENT_REQUESTS: usize = 10;

#[derive(Clone, Debug, PartialEq, Eq, FromRow)]
#[mysql(crate_name = "mysql_async")]
pub struct ShiftNearby {
    pub id: i32,
    pub client_id: i32,
    pub postcode_id: String,
}

pub(crate) async fn nearby(
    pool: &Pool,
    travel_url: &str,
    postcode: &str,
    min: i32,
    max: i32,
) -> Result<String, (StatusCode, String)> {
    let mut conn = pool
        .get_conn()
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let shifts: Vec<ShiftNearby> = conn
        .exec(include_str!("nearby.sql"), params! { "on_date" => chrono::Utc::now().date_naive() })
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let destinations = shifts
        .iter()
        .map(|s| s.postcode_id.clone())
        .collect::<HashSet<String>>()
        .into_iter()
        .collect::<Vec<String>>();
    let travel_times = futures::stream::iter(destinations)
        .map(|destination| {
            let url = format!(
                "{}/travel_times/{}/{}",
                travel_url.trim_end_matches('/'),
                postcode.split_whitespace().collect::<String>(),
                destination.split_whitespace().collect::<String>()
            );
            async move {
                let response = reqwest::get(&url)
                    .await
                    .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;
                if response.status() == reqwest::StatusCode::NOT_FOUND {
                    return Err((
                        StatusCode::NOT_FOUND,
                        response.text().await.map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?,
                    ));
                }
                if !response.status().is_success() {
                    return Err((
                        StatusCode::BAD_GATEWAY,
                        response.text().await.map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?,
                    ));
                }
                response
                    .json::<Vec<TravelTime>>()
                    .await
                    .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))
            }
        })
        .buffer_unordered(CONCURRENT_REQUESTS)
        .try_collect::<Vec<Vec<TravelTime>>>()
        .await?;
    let compact = postcode.split_whitespace().collect::<String>().to_uppercase();
    let district = compact[..compact.len().saturating_sub(3)].to_string();
    let shifts_within_min = shifts
        .iter()
        .filter(|shift| travel_times.iter().flatten().any(|t| {
            t.travel_mins <= min && t.to_postcode_id == shift.postcode_id
        }))
        .count();
    let shifts_within_max = shifts
        .iter()
        .filter(|shift| travel_times.iter().flatten().any(|t| {
            t.travel_mins <= max && t.to_postcode_id == shift.postcode_id
        }))
        .count();
    Ok(if shifts_within_min > 0 {
        format!("{shifts_within_min} shifts within {min} minutes of {district}")
    } else if shifts_within_max > 0 {
        format!("{shifts_within_max} shifts near {district}")
    } else {
        format!("We are not in {district} yet")
    })
}
