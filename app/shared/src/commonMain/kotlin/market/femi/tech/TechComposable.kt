package market.femi.tech

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import market.femi.ServerComposable
import market.femi.State

@Composable
fun TechComposable(state: State, composableId: Int) {
    LaunchedEffect(composableId) {
        state.techApp.getComposable(composableId)
    }
    state.techApp.composable?.let { ServerComposable(state, it) }
}
