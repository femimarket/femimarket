package market.femi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.ui.unit.dp
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.matrix_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun MusicLogin(state: State) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        val code = state.login.userCode
        if (code == null) {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = "You need to login",
                    style = MaterialTheme.typography.headlineMedium,

                )
                Text(
                    text = "To access music video services you need to login via matrix. With matrix you can access services provided by EARN \$FEMI LTD.",
                    style = MaterialTheme.typography.bodyLarge,

                )
                Image(
                    painter = painterResource(Res.drawable.matrix_logo),
                    contentDescription = null,
                    modifier = Modifier.height(48.dp),
                )
                Text(
                    text = "Why Matrix?",
                    style = MaterialTheme.typography.headlineSmall,

                )
                Text(
                    text = "Matrix is a decentralised encrypted chat service.",
                    style = MaterialTheme.typography.bodyLarge,

                )
                Text(
                    text = "The added benefit here is because matrix is chat service, you can also contact EARN \$FEMI LTD.",
                    style = MaterialTheme.typography.bodyLarge,

                )
                Text(
                    text = "To login, you'll be provided with a code and taken to matrix servers use existent or create new account with code. Once complete, you'll be logged in.",
                    style = MaterialTheme.typography.bodyLarge,

                )
                Button(
                    onClick = { state.login.start() },
                    enabled = !state.login.isWorking,
                ) {
                    Text("Get a code")
                }
                Text(
                    text = "Join the community. Contact @femi:femi.market for invite.",
                    style = MaterialTheme.typography.bodyLarge,

                )
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = "Enter this code on the sign in page",
                    style = MaterialTheme.typography.headlineMedium,

                )
                Card {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.displayMedium,

                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
                val uriHandler = LocalUriHandler.current
                OutlinedButton(onClick = { uriHandler.openUri(state.login.verificationUri) }) {
                    Text("Open the sign in page")
                }
                HorizontalDivider()
                CircularProgressIndicator()
                Text(
                    text = "Waiting for you to finish on the page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                )
            }
        }
    }
}
