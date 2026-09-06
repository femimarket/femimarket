//! What care work consists of - top-level cares claimed whole, and the
//! granular facts they break into.

use mysql_async::prelude::FromValue;

#[derive(Clone, Debug, PartialEq, Eq, FromValue, serde::Serialize, serde::Deserialize, utoipa::ToSchema)]
#[mysql(crate_name = "mysql_async", is_string)]
pub enum Activity {
    // Top-Level Cares (Claimed Whole)
    PersonalCare,
    Hoisting,
    StomaCare,
    PegFeeding,
    Catheter,
    Abi,
    Tbi,

    // Granular Facts / Sub-tasks
    BedWash,
    MealPrep,
    StomaBagChange,
}
