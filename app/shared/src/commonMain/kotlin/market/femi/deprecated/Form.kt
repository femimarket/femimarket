package market.femi.deprecated

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    minLinesExpanded: Int = 4,
    maxLinesExpanded: Int = 8,

    header: String? = null,
    singleLine: Boolean = false,

    // 👉 Action Button Props
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    actionEnabled: Boolean = true,
    // 👉 Upgraded to a suspend function so the component can await its completion!
    onActionClick: (suspend () -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 👉 Internal Loading State
    var isWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                text = header,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (label != null) { { Text(label) } } else null,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else if (isExpanded) minLinesExpanded else 1,
            maxLines = if (singleLine) 1 else if (isExpanded) maxLinesExpanded else 1,
            trailingIcon = if (!singleLine) {
                {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            } else null
        )

        // Renders the button below if an action is supplied
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                // 👉 The component safely manages the execution state
                onClick = {
                    scope.launch {
                        isWorking = true
                        try {
                            onActionClick()
                        } finally {
                            isWorking = false
                        }
                    }
                },
                enabled = actionEnabled && !isWorking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                // 👉 Automatically renders a spinner when working
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    if (actionIcon != null) {
                        Icon(actionIcon, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(actionText)
                }
            }
        }
    }
}