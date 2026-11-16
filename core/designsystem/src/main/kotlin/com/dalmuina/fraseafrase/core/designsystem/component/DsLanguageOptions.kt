package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Desplegable de idiomas destino disponibles (`settings-screen`, requisito "Selección del idioma
 * destino"). Muestra el idioma activo preseleccionado y notifica el código elegido por
 * [onSelect]. Implementado sobre `ExposedDropdownMenuBox`/`DropdownMenu` de Material 3, sin añadir
 * ninguna dependencia nueva.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsLanguageOptions(
    options: List<DsLanguageOption>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.code == selectedCode }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = selectedOption?.displayName.orEmpty(),
            onValueChange = {},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.semantics { selected = option.code == selectedCode },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DsLanguageBadge(languageCode = option.code)
                            Text(text = option.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.code)
                    }
                )
            }
        }
    }
}

/**
 * Opción propia del design system para [DsLanguageOptions]: código corto y nombre a mostrar. No
 * es el tipo `LanguageTag` del dominio (ADR-0010): el mapeo lo hace la feature que llama a este
 * componente (igual que [DsLanguageBadge] recibe `languageCode: String`).
 */
data class DsLanguageOption(val code: String, val displayName: String)
