package com.dalmuina.fraseafrase.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.R
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Raíz de la UI. Marcador provisional del esqueleto: la navegación y las pantallas
 * llegarán con los primeros cambios de OpenSpec.
 */
@Composable
fun FraseAFraseApp(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FraseAFraseAppPreview() {
    AppTheme {
        FraseAFraseApp()
    }
}
