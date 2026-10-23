package dev.aaa1115910.bv.tv.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.component.OpusBrowser
import dev.aaa1115910.bv.tv.util.openBiliContent
import kotlinx.coroutines.launch

class OpusSpaceActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context, mid: Long) { context.startActivity(Intent(context, OpusSpaceActivity::class.java).putExtra("mid", mid)) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BVTheme { OpusBrowser(intent.getLongExtra("mid", 0), tv = true, onBack = ::finish,
            onOpen = { link -> lifecycleScope.launch { openBiliContent(this@OpusSpaceActivity, link) } }) } }
    }
}
