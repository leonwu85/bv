package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
fun ControllerTopBar(
    modifier: Modifier = Modifier,
    clock: Triple<Int, Int, Int>,
    title: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = PlayerColors.textPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 4f
                    )
                )
            )
            Spacer(modifier = Modifier.width(24.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Clock(
            hour = clock.first,
            minute = clock.second,
            second = clock.third
        )
    }
}

@Composable
internal fun Clock(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
    second: Int,
) {
    Text(
        modifier = modifier,
        color = PlayerColors.textPrimary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black,
                blurRadius = 4f
            )
        ),
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 32.sp)) {
                append("$hour".padStart(2, '0'))
                append(":")
                append("$minute".padStart(2, '0'))
            }
            withStyle(SpanStyle(fontSize = 18.sp)) {
                append(":")
                append("$second".padStart(2, '0'))
            }
        }
    )
}

@Preview
@Composable
private fun ClockPreview() {
    MaterialTheme {
        Clock(hour = 12, minute = 30, second = 30)
    }
}
