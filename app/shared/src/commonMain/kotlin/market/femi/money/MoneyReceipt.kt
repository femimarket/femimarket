package market.femi.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun MoneyReceipt(state: State) {
    val money = state.moneyApp
    val transaction = money.transactions.firstOrNull()

    LaunchedEffect(transaction?.id) { money.pollUntilSettled() }

    if (transaction == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text("No transfer to show", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        return
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
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    transaction.receive.format().let {
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
                    Text(
                        "to ${transaction.recipientName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = when (transaction.status) {
                            TxStatus.DELIVERED -> MaterialTheme.colorScheme.surfaceContainerHighest
                            TxStatus.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
                            TxStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            transaction.status.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (transaction.status) {
                                TxStatus.DELIVERED -> MaterialTheme.colorScheme.onSurface
                                TxStatus.PROCESSING -> MaterialTheme.colorScheme.onTertiaryContainer
                                TxStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            },
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        transaction.timeline.forEachIndexed { index, event ->
                            Row(Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .background(
                                                if (event.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                CircleShape,
                                            )
                                            .border(
                                                1.dp,
                                                if (event.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                CircleShape,
                                            ),
                                    )
                                    if (index != transaction.timeline.lastIndex) {
                                        Box(
                                            Modifier
                                                .width(2.dp)
                                                .height(28.dp)
                                                .background(
                                                    if (event.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                ),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f).padding(bottom = 8.dp)) {
                                    Text(
                                        event.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (event.done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (event.at.isNotBlank()) {
                                        Text(
                                            event.at.take(19).replace('T', ' '),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        ReceiptRow("You sent", transaction.sendGross.format())
                        ReceiptRow("Fee", if (transaction.fee.isZero) "No fee" else transaction.fee.format())
                        ReceiptRow("They get", transaction.receive.format(), emphasis = true)
                        ReceiptRow("To", "${transaction.bankName} · ${transaction.maskedAccount}")
                        ReceiptRow("Reference", transaction.reference)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = { state.nav.openMoneyHome() }, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

@Composable
fun MoneyTransactions(state: State) {
    val money = state.moneyApp
    if (money.transactions.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No transfers yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your transfers will show up here with live tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(money.transactions, key = { it.id }) { transaction ->
            ListItem(
                headlineContent = { Text(transaction.recipientName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(transaction.createdAt.take(10), maxLines = 1) },
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
                        Text(transaction.receive.format(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
}

@Composable
fun MoneySettings(state: State) {
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focus.clearFocus() }) }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Environment", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ReceiptRow("Payments", if (state.money.isSandbox) "Sandbox (mock)" else "Live")
                ReceiptRow("Rates", if (state.money.isSeeded) "Frozen snapshot" else "Live mid-market")
                ReceiptRow("Database", state.money.dbUrl)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Currencycloud", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Paste sandbox credentials to switch the payments service from mock to real. " +
                        "Note that a production key carries full money-movement authority, so it belongs " +
                        "on a server rather than in a shipped client.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.money.ccBaseUrl,
                    onValueChange = { state.money.ccBaseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.money.ccLoginId,
                    onValueChange = { state.money.ccLoginId = it },
                    label = { Text("Login ID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.money.ccApiKey,
                    onValueChange = { state.money.ccApiKey = it },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Database", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.money.dbUrl,
                    onValueChange = { state.money.dbUrl = it },
                    label = { Text("SurrealDB URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, emphasis: Boolean = false) {
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
