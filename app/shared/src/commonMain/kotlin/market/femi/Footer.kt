@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package market.femi

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.care_nav_account
import femi.app.shared.generated.resources.care_nav_jobs
import femi.app.shared.generated.resources.care_nav_rota
import femi.app.shared.generated.resources.care_nav_servers
import femi.app.shared.generated.resources.care_nav_tasks
import femi.app.shared.generated.resources.company_footer
import femi.app.shared.generated.resources.music_nav_explore
import femi.app.shared.generated.resources.music_nav_servers
import femi.app.shared.generated.resources.music_nav_songs
import femi.app.shared.generated.resources.music_nav_studio
import femi.app.shared.generated.resources.music_nav_you
import org.jetbrains.compose.resources.stringResource

class FooterState(private val state: State): ActiveShowState() {
    private fun navigateTo(route: Route) {
        if (state.nav.backStack.lastOrNull() != route) state.nav.backStack.add(route)
    }
    fun clickJobs(){
        if (state.kv.candidateId.isEmpty()) {
            navigateTo(CareJobsShiftsRoute)
        } else {
            navigateTo(CareCandidateCheckRoute)
            working(
                state.scope,
                state.log,
                "openCareJobs",
                requireShown = false,
            ) {
                runCatching {
                    state.care.getCandidateName(state.kv.candidateId)
                }.getOrElse {
                    state.nav.openCareContact()
                    return@working
                }
            }
        }
    }

}

@Composable
fun Footer(state: State) {
    if (state.nav.isSuperApp or state.nav.isAbout) {
        Text(
            text = stringResource(Res.string.company_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .basicMarquee(iterations = Int.MAX_VALUE),
        )
    } else if (state.nav.isMusicRoute) {
        NavigationBar(windowInsets = WindowInsets(0)) {
            NavigationBarItem(
                selected = state.nav.route == MusicServersRoute,
                onClick = { state.nav.openMusicServer() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.music_nav_servers)) },
            )
            NavigationBarItem(
                selected = state.nav.route == MusicSongsRoute,
                onClick = { state.nav.openMusicSongs() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.music_nav_songs)) },
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.music_nav_explore)) },
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.music_nav_you)) },
            )
            NavigationBarItem(
                selected = state.nav.route == MusicStudioRoute,
                onClick = { state.nav.openMusicStudio() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.MovieFilter,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.music_nav_studio)) },
            )
        }
    } else if (state.nav.isCareRegisterRoute) {
    } else if (state.nav.isCareRoute) {
        NavigationBar(windowInsets = WindowInsets(0)) {
            NavigationBarItem(
                selected = state.nav.route == CareServersRoute,
                onClick = { state.nav.openCareServers() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.care_nav_servers)) },
            )
            NavigationBarItem(
                selected = state.nav.route == CareRotaRoute,
                onClick = { state.nav.openCareRota() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.care_nav_rota)) },
            )
            NavigationBarItem(
                selected = state.nav.route == CareTasksRoute,
                onClick = { state.nav.openCareTasks() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Checklist,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.care_nav_tasks)) },
            )
            NavigationBarItem(
                selected = state.nav.route == CareJobsShiftsRoute || state.nav.route == CareJobsRoute || state.nav.route == CareCandidateCheckRoute,
                onClick = { state.footer.clickJobs() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Work,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.care_nav_jobs)) },
            )
            NavigationBarItem(
                selected = state.nav.route == CareAccountRoute,
                onClick = { state.nav.openCareAccount() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.care_nav_account)) },
            )
        }
    }
}
