package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.player.tv.component.PlayerAnimations
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
fun AutoSkipSponsorTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    skippedSeconds: Int
) {
    AnimatedVisibility(
        visible = show && skippedSeconds > 0,
        enter = PlayerAnimations.tipEnter,
        exit = PlayerAnimations.tipExit
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = PlayerColors.tipBackground
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    text = stringResource(R.string.video_player_auto_skip_sponsor_tip, skippedSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = PlayerColors.textPrimary
                )
            }
        }
    }
}