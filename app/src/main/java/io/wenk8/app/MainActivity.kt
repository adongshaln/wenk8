package io.wenk8.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashGuard.install(applicationContext)
        enableEdgeToEdge()
        val app = application as Wenk8App
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                Wenk8AppRoot(app)
            }
        }
    }
}
