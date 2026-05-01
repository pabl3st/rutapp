package com.pabl3st.rutapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pabl3st.rutapp.core.ui.theme.RutasAppTheme
import com.pabl3st.rutapp.navigation.RutasNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutasAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RutasNavGraph(
                        // finish() cierra la Activity limpiamente
                        // BackHandler en Compose ya gestiona el gesto de sistema
                        onExitApp = { finish() }
                    )
                }
            }
        }
    }
}
