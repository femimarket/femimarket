package market.femi.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.*
import market.femi.State
import market.femi.TEST_CODEC_URL
import market.femi.TEST_DB_URL
import market.femi.TEST_FS_URL
import market.femi.TEST_META_URL
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MusicServers(
    state: State,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Database & Storage Setup",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.music_setup_intro),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Image(
                    painter = painterResource(Res.drawable.music_settings_setup),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.music_setup_fs_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.music_setup_fs),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val uriHandler = LocalUriHandler.current
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    AssistChip(
                        onClick = { uriHandler.openUri("https://github.com/sigoden/dufs") },
                        label = { Text(stringResource(Res.string.music_setup_fs_link)) },
                    )
                }
                OutlinedTextField(
                    value = state.kv.fsUrl,
                    onValueChange = { state.kv.fsUrl = it },
                    label = { Text(stringResource(Res.string.setup_dialog_fs_url_label)) },
                    supportingText = { Text(stringResource(Res.string.setup_dialog_fs_url_support)) },
                    placeholder = { Text(TEST_FS_URL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.music_setup_db_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.music_setup_db),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val uriHandler = LocalUriHandler.current
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    AssistChip(
                        onClick = { uriHandler.openUri("https://couchdb.apache.org/") },
                        label = { Text(stringResource(Res.string.music_setup_db_link)) },
                    )
                }
                OutlinedTextField(
                    value = state.kv.dbUrl,
                    onValueChange = { state.kv.dbUrl = it },
                    label = { Text(stringResource(Res.string.setup_dialog_db_url_label)) },
                    supportingText = { Text(stringResource(Res.string.setup_dialog_db_url_support)) },
                    placeholder = { Text(TEST_DB_URL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.kv.dbUsername,
                    onValueChange = { state.kv.dbUsername = it },
                    label = { Text(stringResource(Res.string.setup_dialog_db_username_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.kv.dbPass,
                    onValueChange = { state.kv.dbPass = it },
                    label = { Text(stringResource(Res.string.setup_dialog_db_pass_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.music_setup_codec_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.music_setup_codec),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedTextField(
                    value = state.kv.codecUrl,
                    onValueChange = { state.kv.codecUrl = it },
                    label = { Text(stringResource(Res.string.setup_dialog_codec_url_label)) },
                    supportingText = { Text(stringResource(Res.string.setup_dialog_codec_url_support)) },
                    placeholder = { Text(TEST_CODEC_URL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.music_setup_meta_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.music_setup_meta),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedTextField(
                    value = state.kv.metaUrl,
                    onValueChange = { state.kv.metaUrl = it },
                    label = { Text(stringResource(Res.string.setup_dialog_meta_url_label)) },
                    supportingText = { Text(stringResource(Res.string.setup_dialog_meta_url_support)) },
                    placeholder = { Text(TEST_META_URL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
