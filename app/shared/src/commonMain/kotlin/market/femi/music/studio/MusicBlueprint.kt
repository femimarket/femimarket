package market.femi.music.studio

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import market.femi.State

@Composable
fun MusicBlueprint(
    state: State,
) {
    if (!state.socialMediaBlueprintDialog.value) return

    AlertDialog(
        onDismissRequest = { state.socialMediaBlueprintDialog.hide() },
        title = { Text("Edit About - Mind Map") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.socialMediaBlueprintDialog.blueprint.isNotBlank()) {
                    var isAboutTextExpanded by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.socialMediaBlueprintDialog.blueprint,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Generated About Summary") },
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        minLines = if (isAboutTextExpanded) 4 else 1,
                        maxLines = if (isAboutTextExpanded) 16 else 1,
                        trailingIcon = {
                            IconButton(onClick = { isAboutTextExpanded = !isAboutTextExpanded }) {
                                Icon(
                                    imageVector = if (isAboutTextExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle About"
                                )
                            }
                        }
                    )
                }

                if (state.socialMediaBlueprintDialog.blueprint.isBlank()) {
                    var isAboutTextExpanded by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.socialMediaBlueprintDialog.blueprint,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Generated About Summary") },
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        minLines = if (isAboutTextExpanded) 4 else 1,
                        maxLines = if (isAboutTextExpanded) 16 else 1,
                        trailingIcon = {
                            IconButton(onClick = {
                                isAboutTextExpanded = !isAboutTextExpanded
                            }) {
                                Icon(
                                    imageVector = if (isAboutTextExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle About"
                                )
                            }
                        }
                    )
                }

                if (state.socialMediaBlueprintDialog.blueprint.isBlank()) {
                    Button(
                        onClick = { state.socialMediaBlueprintDialog.generateBlueprint() },
                        enabled = !state.socialMediaBlueprintDialog.isWorking,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (state.socialMediaBlueprintDialog.isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate About Summary")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = {state.socialMediaBlueprintDialog.hide()}) { Text("Close") } }
    )
}