package dev.aaa1115910.bv.tv.activities.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.component.RemoteControlPanelDemo
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs

class RemoteControllerPanelDemoActivity : TvComponentActivity() {
    companion object {
        const val EXTRA_DIRECT_OPEN = "direct_open"

        fun actionStart(
            context: Context,
            avid: Long,
            cid: Long,
            title: String,
            partTitle: String,
            played: Int,
            fromSeason: Boolean,
            subType: Int? = null,
            epid: Int? = null,
            seasonId: Int? = null,
            isVerticalVideo: Boolean = false,
            proxyArea: ProxyArea = ProxyArea.MainLand,
            playerIconIdle: String = "",
            playerIconMoving: String = "",
            play: Long = 0,
            danmaku: Int = 0,
            like: Int = 0,
            coin: Int = 0,
            favorite: Int = 0,
            upName: String = "",
            upId: Long = 0L,
            upFace: String = "",
            pubTime: String = "",
            audioOnlyMode: Boolean = false
        ) {
            context.startActivity(
                Intent(context, RemoteControllerPanelDemoActivity::class.java).apply {
                    putExtra("avid", avid)
                    putExtra("cid", cid)
                    putExtra("title", title)
                    putExtra("partTitle", partTitle)
                    putExtra("played", played)
                    putExtra("fromSeason", fromSeason)
                    putExtra("subType", subType)
                    putExtra("epid", epid)
                    putExtra("seasonId", seasonId)
                    putExtra("isVerticalVideo", isVerticalVideo)
                    putExtra("proxy_area", proxyArea.ordinal)
                    putExtra("playerIconIdle", playerIconIdle)
                    putExtra("playerIconMoving", playerIconMoving)
                    putExtra("play", play)
                    putExtra("danmaku", danmaku)
                    putExtra("like", like)
                    putExtra("coin", coin)
                    putExtra("favorite", favorite)
                    putExtra("upName", upName)
                    putExtra("upId", upId)
                    putExtra("upFace", upFace)
                    putExtra("pubTime", pubTime)
                    putExtra("audioOnlyMode", audioOnlyMode)
                }
            )
        }

        fun actionStartDirect(
            context: Context,
            avid: Long,
            cid: Long? = null,
            proxyArea: ProxyArea = ProxyArea.MainLand,
            audioOnlyMode: Boolean = false
        ) {
            context.startActivity(
                Intent(context, RemoteControllerPanelDemoActivity::class.java).apply {
                    putExtra(EXTRA_DIRECT_OPEN, true)
                    putExtra("avid", avid)
                    cid?.takeIf { it > 0L }?.let { putExtra("cid", it) }
                    putExtra("proxy_area", proxyArea.ordinal)
                    putExtra("audioOnlyMode", audioOnlyMode)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                RemoteControllerPanelDemoScreen()
            }
        }
    }
}

@Composable
fun RemoteControllerPanelDemoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val intent = (context as Activity).intent

    val continueToPlayerV3 = {
        Prefs.showedRemoteControllerPanelDemo = true
        if (intent.getBooleanExtra(RemoteControllerPanelDemoActivity.EXTRA_DIRECT_OPEN, false)) {
            dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity.actionStartDirect(
                context = context,
                avid = intent.getLongExtra("avid", 0),
                cid = intent.getLongExtra("cid", 0).takeIf { it > 0L },
                proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)],
                audioOnlyMode = intent.getBooleanExtra("audioOnlyMode", false)
            )
        } else {
            dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity.actionStart(
                context = context,
                avid = intent.getLongExtra("avid", 0),
                cid = intent.getLongExtra("cid", 0),
                title = intent.getStringExtra("title") ?: "",
                partTitle = intent.getStringExtra("partTitle") ?: "",
                played = intent.getIntExtra("played", 0),
                fromSeason = intent.getBooleanExtra("fromSeason", false),
                subType = intent.getIntExtra("subType", 0),
                epid = intent.getIntExtra("epid", 0),
                seasonId = intent.getIntExtra("seasonId", 0),
                isVerticalVideo = intent.getBooleanExtra("isVerticalVideo", false),
                proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)],
                playerIconIdle = intent.getStringExtra("playerIconIdle") ?: "",
                playerIconMoving = intent.getStringExtra("playerIconMoving") ?: "",
                audioOnlyMode = intent.getBooleanExtra("audioOnlyMode", false)
            )
        }
        context.finish()
    }

    RemoteControlPanelDemo(
        modifier = modifier.fillMaxSize(),
        onConfirm = continueToPlayerV3
    )
}
