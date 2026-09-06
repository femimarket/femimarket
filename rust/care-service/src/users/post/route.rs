use axum::Router;
use mysql_async::Pool;

pub(crate) fn route() -> Router<Pool> {
    Router::new().merge(super::create::route::route())
}
