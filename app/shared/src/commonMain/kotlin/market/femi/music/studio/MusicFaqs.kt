package market.femi.music.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import market.femi.State
import market.femi.models.answeredCount
import market.femi.models.has10Questions

@Composable
fun MusicFaqs(
    state: State,
) {
    if (!state.faqsDialog.value) return

    AlertDialog(
        onDismissRequest = { state.faqsDialog.cancel() },
        title = { Text("Edit About - FAQs") },
        text = {


            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    modifier = Modifier.clickable { state.faqsDialog.isExpanded.toggle() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            "About (10 Questions)",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text("${state.faqsDialog.faqs.answeredCount}/10 answered")
                    },
                    trailingContent = {
                        Icon(
                            imageVector = if (state.faqsDialog.isExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Section"
                        )
                    }
                )

                if (state.faqsDialog.isExpanded.value) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {


                        if (!state.faqsDialog.faqs.has10Questions) {
                            Button(
                                onClick = { state.faqsDialog.generateFaqs() },
                                enabled = !state.faqsDialog.isWorking,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                if (state.faqsDialog.isWorking) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate Questions")
                                }
                            }
                        }


                        LazyColumn {
                            itemsIndexed(state.faqsDialog.faqs) { index, qa ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = qa.question,
                                            onValueChange = { state.faqsDialog.editQuestion(it,index) },
                                            label = { Text("Question ${index + 1}") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 2,
                                            maxLines = 4
                                        )
                                        OutlinedTextField(
                                            value = qa.answer.orEmpty(),
                                            onValueChange = { state.faqsDialog.editAnswer(it,index) },
                                            label = { Text("Answer ${index + 1}") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 2,
                                            maxLines = 4
                                        )
                                    }
                                }
                            }
                        }


                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { state.faqsDialog.confirm() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { state.faqsDialog.cancel() }) { Text("Close") } }
    )
}