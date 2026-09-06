@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun MoneyPhone(state: State) {
    val money = state.moneyApp
    val identity = money.identity
    val focus = LocalFocusManager.current

    LaunchedEffect(identity.otp) {
        if (identity.otp.length == 6 && money.otpSent) {
            focus.clearFocus()
            money.verifyOtp()
        }
    }

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
                Text("What's your number?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    state.money.reasonFor(KycTier.PHONE_VERIFIED),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                var countryExpanded by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = countryExpanded,
                        onExpandedChange = { countryExpanded = it },
                        modifier = Modifier.weight(0.38f),
                    ) {
                        OutlinedTextField(
                            value = "${identity.phoneCountry.flag} ${identity.phoneCountry.dialCode}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Country") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = countryExpanded,
                            onDismissRequest = { countryExpanded = false },
                        ) {
                            SEND_COUNTRIES.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text("${country.flag}  ${country.name} (${country.dialCode})") },
                                    onClick = {
                                        identity.phoneCountry = country
                                        countryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = identity.phoneRaw,
                        onValueChange = { money.setPhone(it) },
                        label = { Text("Mobile number") },
                        singleLine = true,
                        isError = identity.error != null && !money.otpSent,
                        supportingText = identity.error.takeIf { !money.otpSent }?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.weight(0.62f),
                    )
                }

                Column(Modifier.fillMaxWidth().animateContentSize()) {
                    if (money.otpSent) {
                        OutlinedTextField(
                            value = identity.otp,
                            onValueChange = { money.setOtp(it) },
                            label = { Text("6-digit code") },
                            singleLine = true,
                            isError = identity.error != null,
                            supportingText = {
                                Text(
                                    identity.error ?: if (money.isDemoData) {
                                        "Demo code: ${money.debugOtp} — no SMS is sent in sandbox"
                                    } else {
                                        "Sent to +${money.phoneE164}"
                                    },
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (money.otpSent) {
                Button(
                    onClick = {
                        focus.clearFocus()
                        money.verifyOtp()
                    },
                    enabled = identity.otp.length == 6 && !money.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Verify")
                }
                TextButton(onClick = { money.requestOtp() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Send a new code")
                }
            } else {
                Button(
                    onClick = {
                        focus.clearFocus()
                        money.requestOtp()
                    },
                    enabled = money.phoneLooksValid && !money.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Send code")
                }
            }
        }
    }
}

@Composable
fun MoneyIdentity(state: State) {
    val money = state.moneyApp
    val identity = money.identity
    val focus = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val needsDoc = money.blockingKycTier() == KycTier.ID_VERIFIED

    LaunchedEffect(money.kycVerificationUrl) {
        money.kycVerificationUrl?.let {
            uriHandler.openUri(it)
            money.kycVerificationUrl = null
        }
    }

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
                Text("One quick check", style = MaterialTheme.typography.headlineSmall)
                Text(
                    state.money.reasonFor(KycTier.NAME_DOB),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = identity.firstName,
                    onValueChange = { money.setFirstName(it) },
                    label = { Text("First name") },
                    singleLine = true,
                    isError = identity.error != null && identity.firstName.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = identity.lastName,
                    onValueChange = { money.setLastName(it) },
                    label = { Text("Last name") },
                    singleLine = true,
                    isError = identity.error != null && identity.lastName.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                identity.dob.let { dob ->
                    if (dob.length == 10) dob.substring(8, 10) + dob.substring(5, 7) + dob.substring(0, 4) else ""
                }.let { digits ->
                    OutlinedTextField(
                        value = digits,
                        onValueChange = { typed ->
                            typed.filter { it.isDigit() }.take(8).let {
                                money.setDob(
                                    if (it.length == 8) {
                                        "${it.substring(4)}-${it.substring(2, 4)}-${it.substring(0, 2)}"
                                    } else {
                                        ""
                                    },
                                )
                            }
                        },
                        label = { Text("Date of birth") },
                        singleLine = true,
                        isError = identity.error != null,
                        supportingText = {
                            Text(
                                identity.error ?: if (digits.length == 8) {
                                    "${digits.substring(0, 2)} / ${digits.substring(2, 4)} / ${digits.substring(4)}"
                                } else {
                                    "Day, month, year — e.g. 17041991"
                                },
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (needsDoc) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Photo ID needed", style = MaterialTheme.typography.titleMedium)
                            Text(
                                state.money.reasonFor(KycTier.ID_VERIFIED),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { money.startIdVerification() },
                                enabled = !money.busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Verify my ID")
                            }
                        }
                    }
                }

                Text(
                    "We ask for this only because you're sending money — never to browse rates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = {
                    focus.clearFocus()
                    money.submitIdentity()
                },
                enabled = money.canSubmitIdentity && !money.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        }
    }
}
