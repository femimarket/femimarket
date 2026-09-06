@file:OptIn(ExperimentalLayoutApi::class)

package market.femi.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import market.femi.State

@Composable
fun MoneyPay(state: State) {
    val money = state.moneyApp
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.selectableGroup()) {
                    PayMethod.entries.forEach { method ->
                        val selected = money.send.payMethod == method
                        ListItem(
                            headlineContent = { Text(method.label, maxLines = 1) },
                            supportingContent = {
                                Text(method.blurb, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            leadingContent = { RadioButton(selected = selected, onClick = null) },
                            modifier = Modifier.selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { money.selectPayMethod(method) },
                            ),
                        )
                    }
                }

                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "What's it for?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Nigeria and Kenya require a reason on inbound transfers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PAYMENT_PURPOSES.forEach { purpose ->
                            FilterChip(
                                selected = money.send.purposeId == purpose.id,
                                onClick = { money.setPurpose(purpose.id) },
                                label = { Text(purpose.label, maxLines = 1) },
                            )
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = { money.continueFromPay() },
                enabled = !money.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Review transfer")
            }
        }
    }
}

@Composable
fun MoneyReview(state: State) {
    val money = state.moneyApp
    val send = money.send
    val recipient = send.recipient

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(send.lockExpiresAt) {
        while (money.lockSecondsLeft > 0) {
            delay(1.seconds)
            tick++
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("You send", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        send.sendGross.format().let {
                            Text(
                                it,
                                style = when {
                                    it.length <= 9 -> MaterialTheme.typography.displaySmall
                                    it.length <= 12 -> MaterialTheme.typography.headlineMedium
                                    it.length <= 15 -> MaterialTheme.typography.headlineSmall
                                    else -> MaterialTheme.typography.titleLarge
                                },
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("They get", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        send.receive.format().let {
                            Text(
                                it,
                                style = when {
                                    it.length <= 9 -> MaterialTheme.typography.displaySmall
                                    it.length <= 12 -> MaterialTheme.typography.headlineMedium
                                    it.length <= 15 -> MaterialTheme.typography.headlineSmall
                                    else -> MaterialTheme.typography.titleLarge
                                },
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    money.lockSecondsLeft.also { tick }.let { secondsLeft ->
                        Text(
                            if (secondsLeft <= 0) {
                                "Rate expired"
                            } else {
                                "Rate locked  ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                secondsLeft <= 0 -> MaterialTheme.colorScheme.error
                                secondsLeft in 1..59 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (secondsLeft <= 0) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { money.refreshLock() }) { Text("Refresh") }
                        }
                    }
                }

                recipient?.let {
                    ListItem(
                        headlineContent = { Text(it.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${it.bankName} · ${it.maskedAccount}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    it.initials.ifBlank { "?" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                )
                            }
                        },
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        ReviewRow("Rate", money.tickerLine.removePrefix("1 ").let { "1 $it" })
                        ReviewRow("Fee", if (send.fee.isZero) "No fee" else send.fee.format())
                        ReviewRow("Paying with", send.payMethod.label)
                        ReviewRow("Reason", purposeOf(send.purposeId).label)
                        ReviewRow("Arrives", send.etaLabel.ifBlank { send.corridor.deliveryLabel }, emphasis = true)
                    }
                }

                send.fundingAccount?.let { account ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Pay to", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            ReviewRow(account.accountNumberLabel, account.accountNumber)
                            ReviewRow(account.routingCodeLabel, account.routingCode)
                            ReviewRow("Account name", account.accountHolderName)
                            ReviewRow("Reference", account.reference, emphasis = true)
                            Text(
                                "The reference is how we match your payment. Please include it exactly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = { money.confirmSend() },
                enabled = send.recipient != null && !money.lockExpired && !send.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Send ${send.sendGross.format()}")
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, emphasis: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = if (emphasis) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
