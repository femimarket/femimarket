//! Carers: permanent identity facts only. Anything with a shelf life
//! (absence, mentoring, double-ups-only) lives in its own dated table.

use mysql_async::prelude::FromRow;

#[derive(Clone, Debug, PartialEq, Eq, FromRow, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", table_name = "staffs")]
pub struct Staff {
    pub id: i32,
    pub name: String,
    /// Null = not yet told - Roundsys doesn't know who drives, a human
    /// fills this in. The solver expects a value, so a solve including a
    /// null-transport carer fails loudly until the fact is entered.
    pub transport_mode_id: Option<String>,
    pub gender: Option<String>,
    pub postcode_id: String,
    /// Roundsys pk, while that system is still being fed from / pushed to.
    pub roundsys_pk: Option<String>,
}

impl Staff {
    pub fn transport_mode_id(&self) -> &str {
        self.transport_mode_id.as_deref().expect("transport_mode_id is null")
    }

    pub fn break_mins(&self, break_rules: &[super::break_rules::BreakRule]) -> i64 {
        break_rules.iter()
            .filter(|r| r.staff_id == self.id)
            .max_by_key(|r| r.id)
            .expect("staff has no active break rule")
            .break_mins as i64
    }

    pub fn break_required_after_mins(&self, break_rules: &[super::break_rules::BreakRule]) -> i64 {
        break_rules.iter()
            .filter(|r| r.staff_id == self.id)
            .max_by_key(|r| r.id)
            .expect("staff has no active break rule")
            .break_required_after_mins as i64
    }

    pub fn passengers(&self, passenger_rules: &[super::passenger_rules::PassengerRule]) -> i64 {
        passenger_rules.iter()
            .filter(|r| r.staff_id == self.id)
            .map(|r| r.passengers as i64)
            .max()
            .unwrap_or(0)
    }
}
