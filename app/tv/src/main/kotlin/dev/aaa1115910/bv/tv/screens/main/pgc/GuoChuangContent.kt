package dev.aaa1115910.bv.tv.screens.main.pgc

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.activities.pgc.PgcIndexActivity
import dev.aaa1115910.bv.tv.activities.pgc.guochuang.GuoChuangTimelineActivity
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GuoChuangContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    pgcViewModel: PgcGuoChuangViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val onOpenTimeline: () -> Unit = {
        context.startActivity(Intent(context, GuoChuangTimelineActivity::class.java))
    }
    val onOpenIndex: () -> Unit = {
        PgcIndexActivity.actionStart(context = context, pgcType = PgcType.GuoChuang)
    }

    PgcScaffold(
        lazyListState = lazyListState,
        pgcViewModel = pgcViewModel,
        pgcType = PgcType.GuoChuang,
        featureButtons = { vertical ->
            GuoChuangFeatureButtons(
                modifier = if (vertical) Modifier else Modifier.padding(vertical = 12.dp),
                vertical = vertical,
                onOpenTimeline = onOpenTimeline,
                onOpenIndex = onOpenIndex
            )
        }
    )
}

@Composable
private fun GuoChuangFeatureButtons(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    onOpenTimeline: () -> Unit,
    onOpenIndex: () -> Unit
) {
    val buttons = listOf(
        Triple(
            stringResource(R.string.anime_home_button_timeline),
            Icons.Rounded.Alarm,
            onOpenTimeline
        ),
        Triple(
            stringResource(R.string.anime_home_button_index),
            Icons.AutoMirrored.Rounded.List,
            onOpenIndex
        ),
        Triple(
            stringResource(R.string.pgc_home_button_unknown),
            Icons.Rounded.QuestionMark,
            showPlaceholderToast
        ),
        Triple(
            stringResource(R.string.pgc_home_button_unknown),
            Icons.Rounded.QuestionMark,
            showPlaceholderToast
        )
    )
    PgcFeatureButtons(
        modifier = modifier,
        buttons = buttons,
        vertical = vertical
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun GuoChuangFeatureButtonsPreview() {
    BVTheme {
        GuoChuangFeatureButtons(
            modifier = Modifier,
            onOpenTimeline = {},
            onOpenIndex = {},
        )
    }
}
