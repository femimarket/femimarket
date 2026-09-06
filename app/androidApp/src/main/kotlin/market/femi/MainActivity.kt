package market.femi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import market.femi.services.createRealDbService
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidContext.init(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val state = State()

        setContent {
            App(state)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}