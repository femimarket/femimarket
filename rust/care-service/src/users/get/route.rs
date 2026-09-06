use axum::Router;
use mysql_async::Pool;

pub(crate) fn route() -> Router<Pool> {
    Router::new()
        .merge(super::list::route::route())
        .merge(super::me::route::route())
        .merge(super::service_users::route::route())
        .merge(super::id::route::route())
}
