package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

@Composable
fun TripleLikeTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    message: String = "一键三连成功"
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ),
        exit = fadeOut(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + scaleOut(
            targetScale = 0.8f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
