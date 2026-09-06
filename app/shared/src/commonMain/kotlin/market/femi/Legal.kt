package market.femi

// The all-in-one legal page (privacy + terms + legal notice), copied from the web
// app's LegalPages.kt. Content lives in composeResources/values/legal.xml as one
// string-array; "# " items render as headings, the rest as body blocks.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.legal_content
import femi.app.shared.generated.resources.legal_title
import femi.app.shared.generated.resources.policy_back
import femi.app.shared.generated.resources.policy_updated
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Legal(state: State) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF09090D)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
//            Text(
//                "←  ${stringResource(Res.string.policy_back)}",
//                modifier = Modifier.clickable { state.nav.goBack() },
//                color = Color.White.copy(alpha = 0.7f),
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium,
//            )
//            Spacer(Modifier.height(26.dp))
            Text(
                stringResource(Res.string.legal_title),
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.policy_updated),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            stringArrayResource(Res.array.legal_content).forEach { item ->
                if (item.startsWith("# ")) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        item.removePrefix("# "),
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                } else {
                    Text(
                        item,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Spacer(Modifier.height(14.dp))
            Text(
                "EARN \$FEMI LTD — registered in Scotland, company no. SC604284.\n" +
                    "Registered office: 63 Dunnock House, Dunnock Road, Dunfermline, KY11 8QE.",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
