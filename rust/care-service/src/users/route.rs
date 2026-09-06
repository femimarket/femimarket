use axum::extract::FromRequestParts;
use axum::http::request::Parts;
use axum::http::StatusCode;
use axum::RequestPartsExt;
use axum::Router;
use axum_extra::headers::authorization::Bearer;
use axum_extra::headers::Authorization;
use axum_extra::TypedHeader;
use mysql_async::Pool;

use super::User;

pub(crate) fn route() -> Router<Pool> {
    Router::new()
        .merge(super::post::route::route())
        .merge(super::put::route::route())
        .merge(super::get::route::route())
}

pub struct Me {
    pub matrix_user: matrix_client::models::User,
    pub users: Vec<User>,
}

impl FromRequestParts<Pool> for Me {
    type Rejection = (StatusCode, String);

    async fn from_request_parts(parts: &mut Parts, pool: &Pool) -> Result<Self, Self::Rejection> {
        let TypedHeader(Authorization(bearer)) = parts
            .extract::<TypedHeader<Authorization<Bearer>>>()
            .await
            .map_err(|e| (StatusCode::UNAUTHORIZED, e.to_string()))?;
        let configuration = matrix_client::apis::configuration::Configuration {
            bearer_access_token: Some(bearer.token().to_string()),
            ..Default::default()
        };
        let user = matrix_client::apis::users_api::me(&configuration)
            .await
            .map_err(|e| match e {
                matrix_client::apis::Error::ResponseError(response) => (
                    StatusCode::UNAUTHORIZED,
                    format!("{}/users/me {} {}", configuration.base_path, response.status, response.content),
                ),
                e => (StatusCode::UNAUTHORIZED, format!("{}/users/me {e}", configuration.base_path)),
            })?;
        let users = super::get::me::handle::me(pool, user.id)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
        Ok(Me { matrix_user: user, users })
    }
}
