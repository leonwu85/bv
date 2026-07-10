package dev.aaa1115910.bv.tv.activities.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.search.SearchResultScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class SearchResultActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context, keyword: String, enableProxy: Boolean) {
            context.startActivity(
                Intent(context, SearchResultActivity::class.java).apply {
                    putExtra("keyword", keyword)
                    putExtra("enableProxy", enableProxy)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                SearchResultScreen()
            }
        }
    }
}
