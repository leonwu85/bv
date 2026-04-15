package dev.aaa1115910.bv.tv.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.MaterialTheme

@Composable
fun screenBackgroundGradient(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    return Brush.verticalGradient(
        colors = listOf(
            surfaceVariant,
            surface,
            surface
        )
    )
}