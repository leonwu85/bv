package dev.aaa1115910.bv.tv.activities.search

import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import dev.aaa1115910.bv.tv.screens.search.SearchInputScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class SearchInputActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val defaultFocusRequester = remember { FocusRequester() }
            BVTheme {
                SearchInputScreen(defaultFocusRequester = defaultFocusRequester)
            }
        }
    }
}
