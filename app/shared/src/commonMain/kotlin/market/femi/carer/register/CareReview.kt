package market.femi.carer.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import market.femi.State

private val answers = listOf(
    "Name" to "Folasade Adeyemi",
    "Email" to "folasade@example.com",
    "Mobile" to "07700 900123",
    "Address" to "TW15 1RB, Ashford",
    "Sole trader" to "Yes, UTR 1234567890",
    "Right to work" to "British or Irish citizen",
    "Role" to "Care work at £13 an hour",
    "DBS" to "123456789012, on the update service",
    "Bank" to "Barclays, 20-00-00, 12345678",
    "Documents" to "Passport added, CV added",
    "Skills" to "Drives, happy to carpool, hoist trained",
    "Availability" to "Mornings and evenings, Mon to Fri",
    "Matrix" to "@aramide:femi.market",
)

@Composable
fun CareReview(state: State) {
    CareStep(
        say = "Here is everything you have told me.",
        sub = "Tap anything to fix it.",
        stepNumber = 15,
        stepsTotal = 16,
        action = "Looks right",
        onNext = { state.nav.openCarePassword() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(panel)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            answers.forEach { entry ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dim,
                        modifier = Modifier.width(130.dp),
                    )
                    Text(
                        text = entry.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink,
                    )
                }
            }
        }
    }
}
