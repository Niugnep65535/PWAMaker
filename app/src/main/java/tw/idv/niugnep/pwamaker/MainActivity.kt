package tw.idv.niugnep.pwamaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import tw.idv.niugnep.pwamaker.model.PwaConfig
import tw.idv.niugnep.pwamaker.model.UaMode
import tw.idv.niugnep.pwamaker.ui.GeneratorScreen
import tw.idv.niugnep.pwamaker.ui.ViewerScreen
import tw.idv.niugnep.pwamaker.ui.theme.PWAMakerTheme
import tw.idv.niugnep.pwamaker.utils.ShortcutUtils

sealed class Screen {
    data object Generator : Screen()
    data class Viewer(val config: PwaConfig) : Screen()
}

class MainActivity : ComponentActivity() {

    private var currentScreenState = mutableStateOf<Screen>(Screen.Generator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched from a shortcut
        handleIntent(intent)

        setContent {
            PWAMakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screen by remember { currentScreenState }

                    when (val s = screen) {
                        is Screen.Generator -> {
                            GeneratorScreen(
                                onOpenViewer = { pwaConfig ->
                                    ShortcutUtils.launchPwaInNewWindow(this@MainActivity, pwaConfig)
                                }
                            )
                        }

                        is Screen.Viewer -> {
                            ViewerScreen(
                                config = s.config,
                                onClose = {
                                    moveTaskToBack(true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val launchViewer = intent.getBooleanExtra(ShortcutUtils.EXTRA_LAUNCH_VIEWER, false)
        if (launchViewer) {
            val name = intent.getStringExtra(ShortcutUtils.EXTRA_PWA_NAME) ?: "PWA App"
            val url = intent.getStringExtra(ShortcutUtils.EXTRA_PWA_URL) ?: "https://google.com"
            val iconUri = intent.getStringExtra(ShortcutUtils.EXTRA_PWA_ICON_URI)
            val uaModeName = intent.getStringExtra(ShortcutUtils.EXTRA_PWA_UA_MODE) ?: UaMode.BASIC_MOBILE.name
            val customUa = intent.getStringExtra(ShortcutUtils.EXTRA_PWA_CUSTOM_UA) ?: ""

            val uaMode = try {
                UaMode.valueOf(uaModeName)
            } catch (e: Exception) {
                UaMode.BASIC_MOBILE
            }

            val pwaConfig = PwaConfig(
                name = name,
                url = url,
                iconUri = iconUri,
                uaMode = uaMode,
                customUa = customUa
            )

            currentScreenState.value = Screen.Viewer(pwaConfig)
        }
    }
}
