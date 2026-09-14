package dev.aaa1115910.bv.mobile.util

import android.content.Intent
import android.net.Uri
import android.content.Context
import dev.aaa1115910.biliapi.entity.BiliContentLink
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.ContentLinkResolver
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.bv.mobile.activities.*
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
                VideoPlayerActivity.actionStart(context, aid = link.aid, cid = page.cid,
                    title = info.title, partTitle = page.part, resumeHistory = false,
                    initialSeekPositionMs = link.positionMs)
            }
            is BiliContentLink.Season -> SeasonInfoActivity.actionStart(context, epId = link.episodeId, seasonId = link.seasonId)
            is BiliContentLink.Live -> VideoPlayerActivity.actionStartLive(context, roomId = link.roomId, title = "")
            is BiliContentLink.LiveActivity -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
            is BiliContentLink.User -> UserSpaceActivity.actionStart(context, link.mid, "")
            is BiliContentLink.Dynamic -> DynamicDetailActivity.actionStart(context, link.id)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        (e.localizedMessage ?: "无法打开链接").toast(context)
    }
    return true
}
