package com.rssf.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rssf.reader.ui.ReaderApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = darkColors(primary = Color(0xFF03A9F4), background = Color(0xFF101010), surface = Color(0xFF191919))) {
                ReaderApp(applicationContext)
            }
        }
    }
}
