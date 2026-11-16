package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.core.designsystem.component.DsLanguageBadge

@Composable
internal fun SettingsHeader(
    activeLanguageCode: String,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        DsLanguageBadge(
            languageCode = activeLanguageCode,
            modifier = Modifier
                .clickable(onClick = onNavigateToSettings)
                .testTag("settings_language_badge"),
        )
    }
}
