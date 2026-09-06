package market.femi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import market.femi.ui.models.Material
import market.femi.ui.models.ComponentValue

@Composable
fun ServerComposable(state: State, ui: JsonElement) {
    val composable = Json.decodeFromJsonElement(market.femi.ui.models.Composable.serializer(), ui)
    var modifier: Modifier = Modifier
    composable.modifiers.orEmpty().sortedBy { it.id }.forEach { row ->
        if (row.fillMaxSize) modifier = modifier.fillMaxSize()
        row.background?.let {
            modifier = when (it) {
                ComponentValue.MaterialThemePeriodColorSchemePeriodSurface -> modifier.background(MaterialTheme.colorScheme.surface)
                else -> error("unregistered background: $it")
            }
        }
        row.verticalScroll?.let {
            modifier = when (it) {
                ComponentValue.rememberScrollStateLeft_ParenthesisRight_Parenthesis -> modifier.verticalScroll(rememberScrollState())
                else -> error("unregistered vertical_scroll: $it")
            }
        }
        row.horizontalScroll?.let {
            modifier = when (it) {
                ComponentValue.rememberScrollStateLeft_ParenthesisRight_Parenthesis -> modifier.horizontalScroll(rememberScrollState())
                else -> error("unregistered horizontal_scroll: $it")
            }
        }
        row.wrapContentWidth?.let {
            modifier = when (it) {
                ComponentValue.AlignmentPeriodCenterHorizontally -> modifier.wrapContentWidth(Alignment.CenterHorizontally)
                else -> error("unregistered wrap_content_width: $it")
            }
        }
        row.widthInMax?.let {
            modifier = when (it) {
                ComponentValue.max_Equal_WindowSizeClassPeriodWIDTH_DP_EXPANDED_LOWER_BOUNDPeriodDp ->
                    modifier.widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp)
                else -> error("unregistered width_in_max: $it")
            }
        }
        if (row.fillMaxWidth) modifier = modifier.fillMaxWidth()
        row.padding?.let {
            modifier = when (it) {
                ComponentValue._24PeriodDp -> modifier.padding(24.dp)
                ComponentValue._14PeriodDp -> modifier.padding(14.dp)
                ComponentValue.top_Equal_12PeriodDp -> modifier.padding(top = 12.dp)
                else -> error("unregistered padding: $it")
            }
        }
    }
    when (composable.componentId) {
        Material.Column -> Column(
            modifier = modifier,
            verticalArrangement = when (composable.columnVerticalArrangement) {
                ComponentValue.ArrangementPeriodSpacedByLeft_Parenthesis12PeriodDpRight_Parenthesis -> Arrangement.spacedBy(12.dp)
                null -> Arrangement.Top
                else -> error("unregistered vertical_arrangement: ${composable.columnVerticalArrangement}")
            },
        ) {
            ui.jsonObject["childrens"]?.jsonArray.orEmpty().sortedBy { it.jsonObject["sort_id"]!!.jsonPrimitive.content }.forEach { ServerComposable(state, it) }
        }
        Material.Text -> Text(
            text = composable.label?.translations.orEmpty().let { translations ->
                (translations.firstOrNull { it.langId == Locale.current.toLanguageTag() }
                    ?: translations.firstOrNull { it.langId == Locale.current.toLanguageTag().split("-").first() }
                    ?: translations.firstOrNull { it.langId == "en" })?.text ?: ""
            },
            style = when (composable.textStyle) {
                ComponentValue.MaterialThemePeriodTypographyPeriodHeadlineMedium -> MaterialTheme.typography.headlineMedium
                ComponentValue.MaterialThemePeriodTypographyPeriodBodyLarge -> MaterialTheme.typography.bodyLarge
                ComponentValue.MaterialThemePeriodTypographyPeriodBodySmall -> MaterialTheme.typography.bodySmall
                ComponentValue.MaterialThemePeriodTypographyPeriodTitleMedium -> MaterialTheme.typography.titleMedium
                ComponentValue.MaterialThemePeriodTypographyPeriodHeadlineSmall -> MaterialTheme.typography.headlineSmall
                null -> LocalTextStyle.current
                else -> error("unregistered text_style: ${composable.textStyle}")
            },
            color = when (composable.textColor) {
                ComponentValue.MaterialThemePeriodColorSchemePeriodOnSurfaceVariant -> MaterialTheme.colorScheme.onSurfaceVariant
                null -> Color.Unspecified
                else -> error("unregistered text_color: ${composable.textColor}")
            },
            fontFamily = when (composable.textFontFamily) {
                ComponentValue.FontFamilyPeriodMonospace -> FontFamily.Monospace
                null -> null
                else -> error("unregistered text_font_family: ${composable.textFontFamily}")
            },
            modifier = modifier,
        )
        Material.OutlinedTextField -> remember(composable.id) { mutableStateOf(composable.outlinedTextFieldValue ?: "") }.let { value ->
            OutlinedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                label = {
                    Text(
                        composable.label?.translations.orEmpty().let { translations ->
                            (translations.firstOrNull { it.langId == Locale.current.toLanguageTag() }
                                ?: translations.firstOrNull { it.langId == Locale.current.toLanguageTag().split("-").first() }
                                ?: translations.firstOrNull { it.langId == "en" })?.text ?: ""
                        },
                    )
                },
                singleLine = composable.outlinedTextFieldSingleLine,
                minLines = composable.outlinedTextFieldMinLines ?: 1,
                modifier = modifier,
            )
        }
        Material.Surface -> Surface(
            modifier = modifier,
            shape = when (composable.surfaceShape) {
                ComponentValue.RoundedCornerShapeLeft_Parenthesis10PeriodDpRight_Parenthesis -> RoundedCornerShape(10.dp)
                null -> RoundedCornerShape(0.dp)
                else -> error("unregistered surface_shape: ${composable.surfaceShape}")
            },
            color = when (composable.surfaceColor) {
                ComponentValue.MaterialThemePeriodColorSchemePeriodSurfaceContainerHighest -> MaterialTheme.colorScheme.surfaceContainerHighest
                null -> MaterialTheme.colorScheme.surface
                else -> error("unregistered surface_color: ${composable.surfaceColor}")
            },
        ) {
            Column { ui.jsonObject["childrens"]?.jsonArray.orEmpty().sortedBy { it.jsonObject["sort_id"]!!.jsonPrimitive.content }.forEach { ServerComposable(state, it) } }
        }
        Material.Button -> Button(
            onClick = { composable.buttonActionNav?.let { state.nav.openTechComposable(it) } },
            modifier = modifier,
        ) {
            Text(
                composable.label?.translations.orEmpty().let { translations ->
                    (translations.firstOrNull { it.langId == Locale.current.toLanguageTag() }
                        ?: translations.firstOrNull { it.langId == Locale.current.toLanguageTag().split("-").first() }
                        ?: translations.firstOrNull { it.langId == "en" })?.text ?: ""
                },
            )
        }
    }
}
