use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, EnumIter, DeriveActiveEnum, serde::Serialize, utoipa::ToSchema)]
#[sea_orm(rs_type = "String", db_type = "String(StringLen::None)")]
pub enum ComponentValue {
    #[sea_orm(string_value = "MaterialTheme.colorScheme.surface")]
    #[serde(rename = "MaterialTheme.colorScheme.surface")]
    MaterialThemeColorSchemeSurface,
    #[sea_orm(string_value = "rememberScrollState()")]
    #[serde(rename = "rememberScrollState()")]
    RememberScrollState,
    #[sea_orm(string_value = "Alignment.CenterHorizontally")]
    #[serde(rename = "Alignment.CenterHorizontally")]
    AlignmentCenterHorizontally,
    #[sea_orm(string_value = "max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp")]
    #[serde(rename = "max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp")]
    MaxWindowSizeClassWidthDpExpandedLowerBoundDp,
    #[sea_orm(string_value = "24.dp")]
    #[serde(rename = "24.dp")]
    Dp24,
    #[sea_orm(string_value = "Arrangement.spacedBy(12.dp)")]
    #[serde(rename = "Arrangement.spacedBy(12.dp)")]
    ArrangementSpacedBy12Dp,
    #[sea_orm(string_value = "MaterialTheme.typography.headlineMedium")]
    #[serde(rename = "MaterialTheme.typography.headlineMedium")]
    MaterialThemeTypographyHeadlineMedium,
    #[sea_orm(string_value = "MaterialTheme.typography.bodyLarge")]
    #[serde(rename = "MaterialTheme.typography.bodyLarge")]
    MaterialThemeTypographyBodyLarge,
    #[sea_orm(string_value = "MaterialTheme.colorScheme.onSurfaceVariant")]
    #[serde(rename = "MaterialTheme.colorScheme.onSurfaceVariant")]
    MaterialThemeColorSchemeOnSurfaceVariant,
    #[sea_orm(string_value = "MaterialTheme.typography.titleMedium")]
    #[serde(rename = "MaterialTheme.typography.titleMedium")]
    MaterialThemeTypographyTitleMedium,
    #[sea_orm(string_value = "top = 12.dp")]
    #[serde(rename = "top = 12.dp")]
    TopDp12,
    #[sea_orm(string_value = "RoundedCornerShape(10.dp)")]
    #[serde(rename = "RoundedCornerShape(10.dp)")]
    RoundedCornerShape10Dp,
    #[sea_orm(string_value = "MaterialTheme.colorScheme.surfaceContainerHighest")]
    #[serde(rename = "MaterialTheme.colorScheme.surfaceContainerHighest")]
    MaterialThemeColorSchemeSurfaceContainerHighest,
    #[sea_orm(string_value = "MaterialTheme.typography.bodySmall")]
    #[serde(rename = "MaterialTheme.typography.bodySmall")]
    MaterialThemeTypographyBodySmall,
    #[sea_orm(string_value = "FontFamily.Monospace")]
    #[serde(rename = "FontFamily.Monospace")]
    FontFamilyMonospace,
    #[sea_orm(string_value = "14.dp")]
    #[serde(rename = "14.dp")]
    Dp14,
    #[sea_orm(string_value = "MaterialTheme.typography.headlineSmall")]
    #[serde(rename = "MaterialTheme.typography.headlineSmall")]
    MaterialThemeTypographyHeadlineSmall,
}

#[sea_orm::model]
#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel, serde::Serialize)]
#[sea_orm(table_name = "component_values", schema_name = "ui")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub component_id: String,
    #[sea_orm(belongs_to, from = "component_id", to = "id")]
    pub component: BelongsTo<super::components::Entity>,
    pub value: ComponentValue,
    pub note: String,
    pub created_at: DateTimeUtc,
}

impl ActiveModelBehavior for ActiveModel {}
