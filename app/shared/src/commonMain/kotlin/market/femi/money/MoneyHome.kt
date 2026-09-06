package market.femi.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun MoneyHome(state: State) {
    val money = state.moneyApp
    LaunchedEffect(Unit) { money.start() }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Today's rate",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            money.tickerLine.ifBlank { "Loading rate…" }.let {
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
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        if (state.money.isSeeded) "Indicative — demo data" else "Live mid-market",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    money.send.corridor.deliveryLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (money.recipients.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("Send again", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(money.recipients, key = { it.id }) { recipient ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .widthIn(max = 48.dp)
                                            .clickable { money.startRepeatSend(recipient.id) },
                                    ) {
                                        Box(
                                            Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                recipient.initials.ifBlank { "?" },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            recipient.displayName.split(" ").first(),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (money.transactions.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Recent", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { state.nav.openMoneyTransactions() }) { Text("See all") }
                        }
                    }
                    items(money.transactions.take(4), key = { it.id }) { transaction ->
                        ListItem(
                            headlineContent = { Text(transaction.recipientName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(transaction.bankName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        transaction.recipientName.take(2).uppercase().ifBlank { "?" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                    )
                                }
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(transaction.sendGross.format(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(
                                        transaction.status.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (transaction.status) {
                                            TxStatus.DELIVERED -> MaterialTheme.colorScheme.primary
                                            TxStatus.PROCESSING -> MaterialTheme.colorScheme.onSurfaceVariant
                                            TxStatus.FAILED -> MaterialTheme.colorScheme.error
                                        },
                                    )
                                }
                            },
                        )
                    }
                }

                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            SEED_RATE_DISCLAIMER,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = { money.startSend() }, modifier = Modifier.fillMaxWidth()) {
                Text("Send money")
            }
        }
    }
}
