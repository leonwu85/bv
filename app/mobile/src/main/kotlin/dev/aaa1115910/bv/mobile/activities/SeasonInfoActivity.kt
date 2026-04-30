package dev.aaa1115910.bv.mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.mobile.screen.SeasonInfoScreen
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

class SeasonInfoActivity : ComponentActivity() {
    companion object {
        fun actionStart(
            context: Context,
            epId: Int? = null,
            seasonId: Int? = null,
            proxyArea: ProxyArea = ProxyArea.MainLand
        ) {
            context.startActivity(
                Intent(context, SeasonInfoActivity::class.java).apply {
                    epId?.takeIf { it > 0 }?.let { putExtra("epid", it) }
                    seasonId?.takeIf { it > 0 }?.let { putExtra("seasonid", it) }
                    putExtra("proxy_area", proxyArea.ordinal)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVMobileTheme {
                SeasonInfoScreen()
            }
        }
    }
}
