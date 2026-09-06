package market.femi.money

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun MoneyAmount(state: State) {
    val money = state.moneyApp
    val send = money.send
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { focus.clearFocus() }) }
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Where to?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val destinations = CORRIDORS.filter { it.from == send.corridor.from }.distinctBy { it.destination }
                SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                    destinations.forEachIndexed { index, corridor ->
                        val country = countryOf(corridor.destination)
                        SegmentedButton(
                            selected = send.corridor.destination == corridor.destination,
                            onClick = { money.setCorridor(corridor.from, corridor.to, corridor.rail) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = destinations.size),
                            label = { Text("${country?.flag ?: ""} ${country?.name ?: corridor.destination}", maxLines = 1) },
                        )
                    }
                }

                OutlinedTextField(
                    value = send.sendText,
                    onValueChange = { money.setSendAmountText(it) },
                    label = { Text("You send") },
                    prefix = { Text(send.corridor.from.symbol, style = MaterialTheme.typography.titleLarge) },
                    suffix = { Text(send.corridor.from.code, style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    singleLine = true,
                    isError = send.quoteError != null,
                    supportingText = send.quoteError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (send.corridor.from.exponent > 0) KeyboardType.Decimal else KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                RateLine(state)

                OutlinedTextField(
                    value = send.receiveText,
                    onValueChange = { money.setReceiveAmountText(it) },
                    label = { Text("They get") },
                    prefix = { Text(send.corridor.to.symbol, style = MaterialTheme.typography.titleLarge) },
                    suffix = { Text(send.corridor.to.code, style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (send.corridor.to.exponent > 0) KeyboardType.Decimal else KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                val rails = CORRIDORS.filter { it.from == send.corridor.from && it.destination == send.corridor.destination }
                if (rails.size > 1) {
                    Text(
                        "How they receive it",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 480.dp).fillMaxWidth().animateContentSize()) {
                        rails.forEachIndexed { index, corridor ->
                            SegmentedButton(
                                selected = send.corridor.rail == corridor.rail,
                                onClick = { money.setRecipientRail(corridor.rail) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = rails.size),
                                label = { Text(corridor.rail.label, maxLines = 1) },
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Fee",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                if (send.fee.isZero) "No fee" else send.fee.format(),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Arrives",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                send.corridor.deliveryLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = {
                    focus.clearFocus()
                    money.continueFromAmount()
                },
                enabled = money.canContinueFromAmount,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "No account needed yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RateLine(state: State) {
    val send = state.moneyApp.send
    val rate = send.rate ?: return
    var scale = 1L
    repeat((send.corridor.to.exponent - send.corridor.from.exponent).coerceAtLeast(0)) { scale *= 10 }
    val converted = Money.ofMinor(rate.clientRateE6 * 100L / RATE_SCALE * scale, send.corridor.to)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "1 ${send.corridor.from.code} = ${converted.format()}",
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (send.fee.isZero) "No fees" else "Fee ${send.fee.format()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
