package com.pabl3st.rutapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.RutasAppTheme
import com.pabl3st.rutapp.core.ui.theme.ThemeMode
import com.pabl3st.rutapp.core.ui.theme.ThemeViewModel
import com.pabl3st.rutapp.navigation.RutasNavGraph
import dagger.hilt.android.AndroidEntryPoint
import com.pabl3st.rutapp.fcm.RutasMessagingService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeVm: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Extraer deep link del Intent (viene de notificación FCM)
        val deepLinkType     = intent?.getStringExtra(RutasMessagingService.EXTRA_DEEP_LINK_TYPE)
        val deepLinkRouteUid = intent?.getStringExtra(RutasMessagingService.EXTRA_DEEP_LINK_ROUTE_UID)
        val initialRouteUid  = deepLinkRouteUid?.takeIf {
            deepLinkType == "route_assigned" || deepLinkType == "route_reassigned"
        }

        setContent {
            val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> systemDark
            }

            RutasAppTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RutasNavGraph(
                        onExitApp        = { finish() },
                        initialRouteUid  = initialRouteUid,
                    )
                }
            }
        }
    }
}
