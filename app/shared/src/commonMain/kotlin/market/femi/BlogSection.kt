package market.femi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun BlogTitle(
    title: @Composable () -> Unit,
) {
    title()
}

@Composable
fun BlogTitle(
    title: String,
) {
    BlogTitle(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
            )
        },
    )
}

@Composable
fun BlogSection(
    title: @Composable () -> Unit,
    desc: @Composable () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title()
        desc()
        content()
    }
}

@Composable
fun BlogSection(
    title: String,
    desc: String,
    content: @Composable () -> Unit = {},
) {
    BlogSection(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        desc = {
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        content = content,
    )
}
