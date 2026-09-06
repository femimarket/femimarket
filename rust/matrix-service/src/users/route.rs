use axum::Router;

use crate::server::AppState;

pub fn route() -> Router<AppState> {
    Router::new().merge(super::get::route::route())
}
