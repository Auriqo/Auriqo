package com.auriqo.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auriqo.music.constants.LyricsAnimationStyle
import com.auriqo.music.constants.LyricsSkin

@Composable
fun LyricsAppearanceDialog(
    skin: LyricsSkin,
    animation: LyricsAnimationStyle,
    meshBackground: Boolean,
    glow: Boolean,
    onSkinChange: (LyricsSkin) -> Unit,
    onAnimationChange: (LyricsAnimationStyle) -> Unit,
    onMeshChange: (Boolean) -> Unit,
    onGlowChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var marketplace by remember { mutableStateOf(false) }
    if (marketplace) {
        LyricsMarketplaceDialog(
            selected = skin,
            onSelect = { onSkinChange(it); marketplace = false },
            onDismiss = { marketplace = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apariencia de letras") },
        text = {
            Column {
                Text("Cambios rápidos, sin salir del reproductor")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { marketplace = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Marketplace de temas BetterLyrics")
                }
                Spacer(Modifier.height(8.dp))
                Text("Tema activo: ${skin.name.replace('_', ' ')}")
                Text("Animación: ${animation.name.replace('_', ' ')}")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text("Fondo mesh")
                    Switch(checked = meshBackground, onCheckedChange = onMeshChange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text("Glow")
                    Switch(checked = glow, onCheckedChange = onGlowChange)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}

@Composable
private fun LyricsMarketplaceDialog(
    selected: LyricsSkin,
    onSelect: (LyricsSkin) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marketplace BetterLyrics") },
        text = {
            Column {
                Text("Temas nativos compatibles con Compose. Se instalan al instante y quedan guardados en Auriqo.")
                Spacer(Modifier.height(12.dp))
                LyricsSkin.values().forEach { theme ->
                    TextButton(onClick = { onSelect(theme) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (theme == selected) "✓ ${theme.name.replace('_', ' ')}" else theme.name.replace('_', ' '))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("La extensión original también admite CSS arbitrario; esos estilos no se pueden aplicar directamente a Compose todavía.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
