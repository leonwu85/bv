package dev.aaa1115910.bv.tv.util

import android.content.Intent
import android.net.Uri
import android.content.Context
import dev.aaa1115910.biliapi.entity.BiliContentLink
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.ContentLinkResolver
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.bv.tv.activities.video.*
import dev.aaa1115910.bv.tv.activities.dynamic.DynamicDetailActivity
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Returns true for a recognized link, including a link whose content is unavailable. */
suspend fun openBiliContent(context: Context, text: String): Boolean {
    val link = ContentLinkResolver.resolve(text) ?: return false
    try {
        when (link) {
            is BiliContentLink.Video -> {
                val info = withContext(Dispatchers.IO) {
                    BiliHttpApi.getVideoInfo(bv = AvBvConverter.av2bv(link.aid)).getResponseData()
                }
                val page = if (link.cid != null) info.pages.firstOrNull { it.cid == link.cid }
                    else info.pages.getOrNull(link.page - 1)
                requireNotNull(page) { "链接指定的分 P 不存在" }
                VideoPlayerV3Activity.actionStart(context, avid = link.aid, cid = page.cid,
                    title = info.title, partTitle = page.part, fromSeason = false,
                    played = 0, initialSeekPositionMs = link.positionMs)
            }
            is BiliContentLink.Season -> SeasonInfoActivity.actionStart(context, epId = link.episodeId, seasonId = link.seasonId)
            is BiliContentLink.Live -> VideoPlayerV3Activity.actionStartLive(context, roomId = link.roomId, title = "")
            is BiliContentLink.LiveActivity -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
            is BiliContentLink.User -> UpInfoActivity.actionStart(context, link.mid, "", "")
            is BiliContentLink.Dynamic -> DynamicDetailActivity.actionStart(context, link.id)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        (e.localizedMessage ?: "无法打开链接").toast(context)
    }
    return true
}
