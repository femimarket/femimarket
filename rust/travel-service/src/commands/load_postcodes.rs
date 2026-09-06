use crate::postcodes;
use sea_orm::sea_query::OnConflict;
use sea_orm::{ActiveValue::Set, Database, EntityTrait};

pub async fn load_postcodes(
    database_url: &str,
    onspd_zip: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    let db = Database::connect(database_url).await?;
    let file = std::fs::File::open(onspd_zip)?;
    let mut archive = zip::ZipArchive::new(file)?;
    let mut data_index: Option<usize> = None;
    let mut best_size = 0u64;
    for i in 0..archive.len() {
        let entry = archive.by_index(i)?;
        let name = entry.name().to_string();
        if name.contains("Data/")
            && name.ends_with(".csv")
            && !name.contains("multi_csv")
            && entry.size() > best_size
        {
            best_size = entry.size();
            data_index = Some(i);
        }
    }
    let index = data_index.ok_or("No Data/*.csv found inside the ONSPD zip")?;
    let entry = archive.by_index(index)?;
    println!("Reading {} ({} MB)...", entry.name(), entry.size() / 1_048_576);
    let mut reader = csv::Reader::from_reader(entry);
    let headers = reader.headers()?.clone();
    let column = |name: &str| {
        headers
            .iter()
            .position(|h| h == name)
            .ok_or_else(|| format!("ONSPD csv is missing column {name}"))
    };
    let pcds_idx = column("pcds")?;
    let lat_idx = column("lat")?;
    let long_idx = column("long")?;

    let mut batch: Vec<postcodes::ActiveModel> = Vec::with_capacity(5000);
    let mut loaded = 0u64;
    let mut skipped = 0u64;
    for record in reader.records() {
        let record = record?;
        let pcds = record.get(pcds_idx).unwrap_or("").trim().to_string();
        let lat: f64 = match record.get(lat_idx).unwrap_or("").trim().parse() {
            Ok(value) => value,
            Err(_) => {
                skipped += 1;
                continue;
            }
        };
        let long: f64 = match record.get(long_idx).unwrap_or("").trim().parse() {
            Ok(value) => value,
            Err(_) => {
                skipped += 1;
                continue;
            }
        };
        if pcds.is_empty() || lat > 90.0 {
            skipped += 1;
            continue;
        }
        batch.push(postcodes::ActiveModel {
            id: Set(pcds),
            latitude: Set(lat),
            longitude: Set(long),
        });
        if batch.len() == 5000 {
            loaded += batch.len() as u64;
            postcodes::Entity::insert_many(std::mem::take(&mut batch))
                .on_conflict(
                    OnConflict::column(postcodes::Column::Id)
                        .update_columns([postcodes::Column::Latitude, postcodes::Column::Longitude])
                        .to_owned(),
                )
                .exec(&db)
                .await?;
            if loaded % 500_000 == 0 {
                println!("   {loaded} postcodes...");
            }
        }
    }
    loaded += batch.len() as u64;
    if !batch.is_empty() {
        postcodes::Entity::insert_many(batch)
            .on_conflict(
                OnConflict::column(postcodes::Column::Id)
                    .update_columns([postcodes::Column::Latitude, postcodes::Column::Longitude])
                    .to_owned(),
            )
            .exec(&db)
            .await?;
    }
    println!("--> {loaded} postcodes loaded ({skipped} skipped: no position)");
    Ok(())
}
