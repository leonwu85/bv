package dev.aaa1115910.bv.tv.activities.dynamic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.DynamicDetailScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class DynamicDetailActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context, dynamicId: String) {
            context.startActivity(
                Intent(context, DynamicDetailActivity::class.java).apply {
                    putExtra("dynamicId", dynamicId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dynamicId = intent.getStringExtra("dynamicId") ?: ""
        setContent {
            BVTheme {
                DynamicDetailScreen(dynamicId = dynamicId)
            }
        }
    }
}
