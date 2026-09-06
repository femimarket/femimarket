use crate::travel_times;
use chrono::NaiveDate;
use sea_orm::{ColumnTrait, Condition, Database, EntityTrait, QueryFilter};



pub async fn export_travel_times(
    database_url: &str,
    from_date: &str,
    to_date: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    let from_date = NaiveDate::parse_from_str(from_date, "%Y-%m-%d")?;
    let to_date = NaiveDate::parse_from_str(to_date, "%Y-%m-%d")?;
    let span_from = chrono::NaiveDateTime::new(from_date, chrono::NaiveTime::MIN).and_utc();
    let span_to = chrono::NaiveDateTime::new(
        to_date,
        chrono::NaiveTime::from_hms_opt(23, 59, 59).expect("time"),
    )
    .and_utc();
    let db = Database::connect(database_url).await?;
    let rows: Vec<travel_times::TravelTime> = travel_times::Entity::find()
        .filter(
            Condition::any()
                .add(travel_times::Column::DepartureTime.is_null())
                .add(travel_times::Column::DepartureTime.between(span_from, span_to)),
        )
        .all(&db)
        .await?
        .into_iter()
        .map(|r| travel_times::TravelTime {
            from_postcode_id: r.from_postcode_id,
            to_postcode_id: r.to_postcode_id,
            transport_mode_id: r.transport_mode_id,
            travel_mins: r.travel_mins,
            departure_time: r.departure_time,
        })
        .collect();
    println!("{}", serde_json::to_string(&rows)?);
    Ok(())
}
