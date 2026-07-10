package dev.aaa1115910.bv.tv.activities.settings

import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.settings.MediaCodecScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class MediaCodecActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                MediaCodecScreen()
            }
        }
    }
}
