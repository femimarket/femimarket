use axum::Router;

use crate::server::AppState;

pub(crate) fn route() -> Router<AppState> {
    Router::new().merge(super::me::route::route())
}
