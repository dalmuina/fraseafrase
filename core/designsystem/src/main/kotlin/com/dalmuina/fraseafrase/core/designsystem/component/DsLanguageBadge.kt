package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.R
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Insignia circular del idioma destino activo (language-indicator). Recibe un [String] con el
 * código de idioma en vez del tipo `LanguageTag` del dominio, ya que `:core:designsystem` no
 * depende de ningún módulo del proyecto (ADR-0010, igual que [DsLevelBadge] recibe un [Int] en vez
 * del tipo `Level`). El mapeo desde `LanguageTag` lo hace la feature que llama a este componente.
 * Un código no reconocido no pinta nada.
 */
@Composable
fun DsLanguageBadge(languageCode: String, modifier: Modifier = Modifier) {
    val spec = languageBadgeSpec(languageCode) ?: return
    Icon(
        painter = painterResource(id = spec.drawableRes),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier
            .size(LanguageBadgeSize)
            .clip(CircleShape)
            .semantics { contentDescription = spec.contentDescription }
    )
}

private val LanguageBadgeSize = 40.dp

internal data class LanguageBadgeSpec(val drawableRes: Int, val contentDescription: String)

internal fun languageBadgeSpec(languageCode: String): LanguageBadgeSpec? = when (languageCode) {
    "pt-PT" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_pt_pt, contentDescription = "pt-PT")
    "es-ES" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_es_es, contentDescription = "es-ES")
    "it-IT" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_it_it, contentDescription = "it-IT")
    "en-GB" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_en_gb, contentDescription = "en-GB")
    "de-DE" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_de_de, contentDescription = "de-DE")
    "fr-FR" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_fr_fr, contentDescription = "fr-FR")
    "ru-RU" -> LanguageBadgeSpec(drawableRes = R.drawable.ic_flag_ru_ru, contentDescription = "ru-RU")
    else -> null
}

private val AllLanguageCodes = listOf("pt-PT", "es-ES", "it-IT", "en-GB", "de-DE", "fr-FR", "ru-RU")

@PreviewLightDark
@Composable
private fun DsLanguageBadgeAllLanguagesPreview() {
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            AllLanguageCodes.forEach { code -> DsLanguageBadge(languageCode = code) }
        }
    }
}
