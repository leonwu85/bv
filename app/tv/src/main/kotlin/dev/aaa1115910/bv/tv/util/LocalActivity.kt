package dev.aaa1115910.bv.tv.util

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

/** Returns the Activity explicitly supplied by [dev.aaa1115910.bv.tv.activities.TvComponentActivity]. */
@Composable
internal fun requireTvActivity(): Activity = checkNotNull(LocalActivity.current) {
    "TV content must be hosted by an Activity"
}
