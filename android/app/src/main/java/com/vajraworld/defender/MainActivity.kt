package com.vajraworld.defender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vajraworld.defender.ui.navigation.VajraNavGraph
import com.vajraworld.defender.ui.theme.VajraWorldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VajraApplication
        setContent {
            VajraWorldTheme {
                VajraNavGraph(repository = app.repository)
            }
        }
    }
}
