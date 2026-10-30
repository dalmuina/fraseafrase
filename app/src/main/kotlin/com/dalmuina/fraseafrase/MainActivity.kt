package com.dalmuina.fraseafrase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.ui.FraseAFraseApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                FraseAFraseApp()
            }
        }
    }
}
