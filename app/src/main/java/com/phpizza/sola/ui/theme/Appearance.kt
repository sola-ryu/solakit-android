package com.phpizza.sola.ui.theme

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

val LocalHighContrast = staticCompositionLocalOf { false }
private val LocalSetHighContrast = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
fun AppAppearance(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("sola_appearance", Context.MODE_PRIVATE)
    }
    var highContrast by remember { mutableStateOf(preferences.getBoolean("high_contrast", false)) }
    CompositionLocalProvider(LocalSetHighContrast provides { enabled ->
        highContrast = enabled
        preferences.edit().putBoolean("high_contrast", enabled).apply()
    }) {
        SolaTheme(highContrast = highContrast, content = content)
    }
}

@Composable
fun contrastBorder(selected: Boolean = false): BorderStroke? =
    if (LocalHighContrast.current) BorderStroke(
        if (selected) 3.dp else 1.5.dp, MaterialTheme.colorScheme.outline
    ) else null

@Composable
fun HighContrastSetting() {
    val enabled = LocalHighContrast.current
    val onChange = LocalSetHighContrast.current
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = enabled, role = Role.Switch, onValueChange = onChange
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("High-contrast light mode", modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium)
        Switch(checked = enabled, onCheckedChange = null)
    }
}
