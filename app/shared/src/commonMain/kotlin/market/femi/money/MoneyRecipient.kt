@file:OptIn(ExperimentalMaterial3Api::class)

package market.femi.money

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun MoneyRecipient(state: State) {
    val money = state.moneyApp
    val form = money.recipientForm
    val focus = LocalFocusManager.current
    val isWallet = money.send.corridor.rail == Rail.MOBILE_MONEY
    var pickingBank by remember { mutableStateOf(false) }

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
                if (money.recipients.isNotEmpty()) {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    money.recipients.take(3).forEach { recipient ->
                        ListItem(
                            headlineContent = { Text(recipient.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text("${recipient.bankName} · ${recipient.maskedAccount}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            leadingContent = {
                                Box(
                                    Modifier
                                        .size(40.dp)
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
                            },
                            modifier = Modifier.clickable { money.useRecipient(recipient.id) },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Text(
                        "New recipient",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ListItem(
                    headlineContent = {
                        Text(
                            form.bank?.name ?: (if (isWallet) "Choose a provider" else "Choose a bank"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = { Text(form.bank?.let { "Tap to change" } ?: "Required") },
                    trailingContent = {
                        Text(
                            if (form.bank == null) "Choose" else "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable { pickingBank = true },
                )

                Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    form.bank?.let { bank ->
                        OutlinedTextField(
                            value = form.accountNumber,
                            onValueChange = { money.setAccountNumber(it) },
                            label = { Text(if (isWallet) "Mobile number" else "Account number") },
                            supportingText = { Text("${form.accountNumber.length} / ${bank.accountLen} digits") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        when (val resolve = form.resolveState) {
                            is ResolveState.Resolving -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Checking name…", style = MaterialTheme.typography.bodyMedium)
                            }

                            is ResolveState.Matched -> Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Account name",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        resolve.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "Confirmed by ${bank.shortName}. Check this is the right person.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            is ResolveState.NotFound -> Text(
                                resolve.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )

                            ResolveState.Idle -> Unit
                        }

                        if (!state.money.supports(bank)) {
                            OutlinedTextField(
                                value = form.typedName,
                                onValueChange = { form.typedName = it },
                                label = { Text("Recipient's full name") },
                                supportingText = { Text("${bank.shortName} can't confirm names before payout") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
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
                    money.confirmRecipient()
                },
                enabled = money.canConfirmRecipient && !money.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        }
    }

    if (pickingBank) {
        BankSheet(state, isWallet) { pickingBank = false }
    }
}

@Composable
private fun BankSheet(state: State, isWallet: Boolean, onDismiss: () -> Unit) {
    val money = state.moneyApp
    val form = money.recipientForm
    var query by remember { mutableStateOf("") }
    val banks = money.banksForForm()
    val matches = banks
        .filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.shortName.contains(query, ignoreCase = true)
        }
        .sortedByDescending { it.popular }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                if (isWallet) "Choose a provider" else "Choose a bank",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search banks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(matches, key = { it.code }) { bank ->
                    ListItem(
                        headlineContent = { Text(bank.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = if (bank.popular) {
                            { Text("Popular", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        } else {
                            null
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (form.bank?.code == bank.code) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        ),
                        modifier = Modifier.clickable {
                            money.selectBank(bank.code)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}
