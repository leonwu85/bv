package dev.aaa1115910.bv.tv.activities.pgc.guochuang

import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.screens.main.pgc.anime.AnimeTimelineScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class GuoChuangTimelineActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                AnimeTimelineScreen(
                    filter = TimelineFilter.GuoChuang,
                    titleResId = R.string.title_activity_guochuang_timeline
                )
            }
        }
    }
}
