package market.femi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StoryboardTimelineHeader(
    state: State,
//    audioLines: List<AudioLine>,
//    genCount: GenCount,
//    isWorking: Boolean,
//    onGenerateClick: () -> Unit
) {
    if (state.data.audio?.lines.orEmpty().isEmpty()) return

    val pendingCostStr = (state.data.totalCost.totalPendingCost).toFemi()
    val pendingTasks = state.data.totalCost.missingExpands + state.data.totalCost.missingScenes + state.data.totalCost.missingImages
    val progressFloat = state.data.totalCost.completion / 100f
    val totalBudgetStr = "Spent: ${(state.data.totalCost.totalCost + state.data.totalCost.totalPendingCost).toFemi()}"
    val finalPendingStr = "Pending: ${(state.data.totalCost.totalPendingCost).toFemi()}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "STORYBOARD TIMELINE", // Force All-Caps
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Fade it out
            letterSpacing = 2.sp, // 👈 THE MAGIC SAUCE: heavily tracked text
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp) // Push it completely off the giant number
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally // 👈 This centers the big circle button
        ) {

            // 👉 2. Top Layer: The BIG, TRUE CIRCLE Button
//            Button(
//                onClick = onGenerateClick,
//                enabled = !isWorking && pendingTasks > 0,
//                modifier = Modifier
//                    .size(140.dp), // 👈 Forces a perfect 1x1 geometric circle
//                shape = CircleShape,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary, // Standard bright primary
//                    contentColor = MaterialTheme.colorScheme.onPrimary,
//                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
//                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
//                ),
//                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp) // Gives it a nice tactile pop
//            ) {
//                // Content inside the circle stacked vertically
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    if (isWorking) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(24.dp),
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            strokeWidth = 2.dp
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text("Generating...")
//                    } else if (pendingTasks == 0) {
//                        Text("✓\nDone", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
//                    } else {
//                        Text("GENERATE", fontWeight = FontWeight.Bold)
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(pendingCostStr, style = MaterialTheme.typography.titleMedium) // Shows the cost inside the circle
//                    }
//                }
//            }

            Spacer(modifier = Modifier.height(32.dp))

            // 👉 3. Middle Layer: Standard Progress Indicator
            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary, // 👈 Normal bright primary color
                trackColor = MaterialTheme.colorScheme.surfaceVariant, // 👈 Normal faded track background
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 👉 4. Bottom Layer: The Data Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = finalPendingStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = totalBudgetStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}