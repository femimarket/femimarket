use mysql_async::Pool;


#[tokio::test]
async fn list() {
    let pool = Pool::new(std::env::var("DATABASE_URL").unwrap().as_str());
    super::handle::list(&pool).await.unwrap();
}