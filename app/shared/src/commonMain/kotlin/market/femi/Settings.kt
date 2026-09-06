package market.femi

import androidx.compose.runtime.Composable
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.legal
import femi.app.shared.generated.resources.settings_legal
import femi.app.shared.generated.resources.login
import femi.app.shared.generated.resources.login_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Settings(state: State) {
    CarouselMenu(
        items = listOf(
            CarouselMenuItem(
                title = stringResource(Res.string.settings_legal),
                imagePainter = painterResource(Res.drawable.legal),
                onClick = { state.nav.openLegal() },
            ),
            CarouselMenuItem(
                title = stringResource(Res.string.login_title),
                imagePainter = painterResource(Res.drawable.login),
                onClick = { state.nav.openLogin() },
            ),
        ),
    )
}
