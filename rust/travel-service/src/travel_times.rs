//! Pre-computed travel minutes between postcodes - the answer sheet the
//! solver consults for every leg of every carer's day, replacing
//! travel_matrix_lookup.csv. Machine-computed from road maps and
//! timetables, not a user assertion, so it carries no note or user frame.
//! departure_time is set only for timetabled modes (public transport):
//! one row per 15-minute departure slot across the day. Car and bike
//! times are constant and leave it null. A missing pair means that leg
//! cannot exist.

use crate::postcodes;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use chrono::Utc;
use sea_orm::entity::prelude::*;
use sea_orm::DatabaseConnection;

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "travel_times")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub from_postcode_id: String,
    #[sea_orm(belongs_to, from = "from_postcode_id", to = "id", relation_enum = "FromPostcode")]
    pub from_postcode: BelongsTo<super::postcodes::Entity>,
    pub to_postcode_id: String,
    #[sea_orm(belongs_to, from = "to_postcode_id", to = "id", relation_enum = "ToPostcode")]
    pub to_postcode: BelongsTo<super::postcodes::Entity>,
    pub transport_mode_id: String,
    /// Whole minutes - the solver plans on an integer-minute grid.
    pub travel_mins: i32,
    pub departure_time: Option<DateTimeUtc>,
}

impl ActiveModelBehavior for ActiveModel {}

const WALK_SPEED_METERS_PER_SECOND: f64 = 1.0;
const BIKE_SPEED_METERS_PER_SECOND: f64 = 12.0 / 3.6;


#[derive(serde::Serialize, serde::Deserialize)]
pub struct TravelTime {
    pub from_postcode_id: String,
    pub to_postcode_id: String,
    pub transport_mode_id: String,
    pub travel_mins: i32,
    pub departure_time: Option<chrono::DateTime<chrono::Utc>>,
}

#[tracing::instrument(skip_all)]
pub async fn get(
    State(db): State<DatabaseConnection>,
    Path((from_postcode, to_postcode)): Path<(String, String)>,
) -> Result<Json<Vec<TravelTime>>, (StatusCode, String)> {
    let motis_url = std::env::var("MOTIS_URL")
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let from_postcode = from_postcode.to_uppercase();
    let from_postcode = format!("{} {}", &from_postcode[..from_postcode.len().saturating_sub(3)], &from_postcode[from_postcode.len().saturating_sub(3)..]);
    let to_postcode = to_postcode.to_uppercase();
    let to_postcode = format!("{} {}", &to_postcode[..to_postcode.len().saturating_sub(3)], &to_postcode[to_postcode.len().saturating_sub(3)..]);
    let from = postcodes::Entity::find_by_id(from_postcode.clone())
        .one(&db)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or((StatusCode::NOT_FOUND, format!("postcode not found: {from_postcode}")))?;
    let to = postcodes::Entity::find_by_id(to_postcode.clone())
        .one(&db)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or((StatusCode::NOT_FOUND, format!("postcode not found: {to_postcode}")))?;
    let mut configuration = motis_service::apis::configuration::Configuration::new();
    configuration.base_path = motis_url.trim_end_matches('/').to_string();
    let from_coord = format!("{},{}", from.latitude, from.longitude);
    let to_coord = format!("{},{}", to.latitude, to.longitude);
    let response = motis_service::apis::routing_api::plan(
        &configuration,
        &from_coord,
        &to_coord,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some(WALK_SPEED_METERS_PER_SECOND),
        Some(BIKE_SPEED_METERS_PER_SECOND),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some(false),
        None,
        None,
        Some(vec![
            motis_service::models::Mode::Car,
            motis_service::models::Mode::Bike,
            motis_service::models::Mode::Walk,
        ]),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some(2 * 60 * 60),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
    )
    .await
    .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;
    let times = response
        .itineraries
        .iter()
        .chain(response.direct.iter())
        .map(|i| {
            let transport_mode_id = match i
                .legs
                .iter()
                .map(|leg| leg.mode)
                .find(|mode| *mode != motis_service::models::Mode::Walk)
            {
                None => "walk",
                Some(motis_service::models::Mode::Car) => "car",
                Some(motis_service::models::Mode::Bike) => "bicycle",
                Some(_) => "transit",
            };
            TravelTime {
                from_postcode_id: from_postcode.clone(),
                to_postcode_id: to_postcode.clone(),
                transport_mode_id: transport_mode_id.to_string(),
                travel_mins: ((i.duration as i64 + 30) / 60) as i32,
                departure_time: match transport_mode_id {
                    "transit" => Some(i.start_time.with_timezone(&Utc)),
                    _ => None,
                },
            }
        })
        .collect::<Vec<TravelTime>>();
    Ok(Json(times))
}
