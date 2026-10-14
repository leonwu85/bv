package dev.aaa1115910.bv.player.tv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme

/** Fit labels such as 1.25x into scaled TV buttons without wrapping or truncating the suffix. */
@Composable
internal fun AutoSizeButtonText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    val style = MaterialTheme.typography.titleMedium
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = style.copy(
                color = color,
                fontWeight = fontWeight ?: style.fontWeight,
                textAlign = TextAlign.Center,
                lineHeight = 1.em,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 1.sp,
                maxFontSize = style.fontSize,
                stepSize = 0.25.sp,
            ),
        )
    }
}
