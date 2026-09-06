use axum::Router;
use mysql_async::Pool;

pub(crate) fn route() -> Router<Pool> {
    Router::new()
        .merge(super::list::route::route())
        .merge(super::nearby::route::route())
}
