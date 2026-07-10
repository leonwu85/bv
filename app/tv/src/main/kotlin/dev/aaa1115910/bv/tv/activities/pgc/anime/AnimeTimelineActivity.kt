package dev.aaa1115910.bv.tv.activities.pgc.anime

import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.main.pgc.anime.AnimeTimelineScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class AnimeTimelineActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                AnimeTimelineScreen()
            }
        }
    }
}
