package dev.aaa1115910.bv.tv.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.tv.util.LocalTvUiPerformanceProfile
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.isKeyDown
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    isLargePadding: Boolean,
    initialSelectedItem: TopNavItem? = null,
    focusSelectedToken: Int = 0,
    onFocusedChanged: (TopNavItem) -> Unit = {},
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {},
    onLeftKeyEvent: () -> Unit = {},
    onPendingDownKeyEvent: (() -> Boolean)? = null,
    onDownKeyEvent: (() -> Boolean)? = null
) {
    if (items.isEmpty()) return

    val focusRequester = remember { FocusRequester() }
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val performanceProfile = LocalTvUiPerformanceProfile.current
    val enablePageAnimation =
        enableMainUiAnimation && performanceProfile.allowFullPageAnimation
    // 仅做轻微防抖；焦点解锁与内容就绪绑定为短延迟，避免原先 200+400ms 叠卡顿
    val selectionDispatchDelay = if (enablePageAnimation) 80L else 0L
    val focusUnlockDelay = if (enablePageAnimation) 100L else 0L

    var highlightedNav by remember(initialSelectedItem, items) {
        mutableStateOf(initialSelectedItem ?: items.first())
    }

    val highlightedTabIndex = items.indexOf(highlightedNav).takeIf { it >= 0 } ?: 0

    var canMoveFocusDown by remember { mutableStateOf(true) }
    var hasNavFocus by remember { mutableStateOf(false) }

    LaunchedEffect(items, initialSelectedItem) {
        val nextSelectedItem = initialSelectedItem?.takeIf { it in items } ?: items.first()
        highlightedNav = nextSelectedItem
        canMoveFocusDown = true
    }

    LaunchedEffect(highlightedNav, initialSelectedItem, items, hasNavFocus) {
        if (highlightedNav !in items) return@LaunchedEffect
        if (!hasNavFocus) {
            canMoveFocusDown = true
            return@LaunchedEffect
        }
        if (highlightedNav == initialSelectedItem) {
            canMoveFocusDown = true
            return@LaunchedEffect
        }
        delay(selectionDispatchDelay)
        if (highlightedNav != initialSelectedItem) {
            onSelectedChanged(highlightedNav)
        }

        delay(focusUnlockDelay)
        canMoveFocusDown = true
    }

    LaunchedEffect(focusSelectedToken, highlightedNav, items) {
        if (focusSelectedToken <= 0) return@LaunchedEffect
        if (highlightedNav !in items) return@LaunchedEffect
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 9.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TabRow(
            modifier = Modifier
                .focusProperties { onEnter = { focusRequester.requestFocus() } }
                .focusRestorer(focusRequester)
                .onFocusChanged {
                    hasNavFocus = it.hasFocus
                }
                .onPreviewKeyEvent {
                    if (it.isKeyDown()) {
                        if (it.key == Key.DirectionLeft && highlightedTabIndex == 0) {
                            onLeftKeyEvent()
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.DirectionDown) {
                            if (!canMoveFocusDown) {
                                if (onPendingDownKeyEvent?.invoke() == true) {
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            if (onDownKeyEvent?.invoke() == true) {
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
            selectedTabIndex = highlightedTabIndex,
            separator = { Spacer(modifier = Modifier.width(12.dp)) },
        ) {
            items.forEachIndexed { index, tab ->
                NavItemTab(
                    modifier = Modifier
                        .ifElse(index == highlightedTabIndex, Modifier.focusRequester(focusRequester)),
                    topNavItem = tab,
                    selected = index == highlightedTabIndex,
                    onFocus = {
                        // 只在切换到不同 tab 时阻止向下移动，等待页面切换完成
                        val isSameTab = tab == highlightedNav
                        highlightedNav = tab
                        onFocusedChanged(tab)
                        if (!isSameTab) {
                            canMoveFocusDown = false
                        }
                    },
                    onClick = { onClick(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabRowScope.NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val context = LocalContext.current

    Tab(
        modifier = modifier,
        selected = selected,
        onFocus = onFocus,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            text = topNavItem.getDisplayName(context),
            color = LocalContentColor.current,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

interface TopNavItem {
    fun getDisplayName(context: Context = BVApp.context): String
}

enum class HomeTopNavItem(private val displayName: String) : TopNavItem {
    Recommend("推荐"),
    Popular("热门"),
    Dynamics("动态"),
    History("历史"),
    Favorite("收藏"),
    FollowingSeason("追番"),
    ToView("稍后看");

    override fun getDisplayName(context: Context): String {
        return displayName
    }
}

enum class UgcTopNavItem(private val ugcType: UgcTypeV2) : TopNavItem {
    Douga(UgcTypeV2.Douga),
    Game(UgcTypeV2.Game),
    Kichiku(UgcTypeV2.Kichiku),
    Music(UgcTypeV2.Music),
    Dance(UgcTypeV2.Dance),
    Cinephile(UgcTypeV2.Cinephile),
    Ent(UgcTypeV2.Ent),
    Knowledge(UgcTypeV2.Knowledge),
    Tech(UgcTypeV2.Tech),
    Information(UgcTypeV2.Information),
    Food(UgcTypeV2.Food),
    ShortPlay(UgcTypeV2.Shortplay),
    Car(UgcTypeV2.Car),
    Fashion(UgcTypeV2.Fashion),
    Sports(UgcTypeV2.Sports),
    Animal(UgcTypeV2.Animal),
    Vlog(UgcTypeV2.Vlog),
    Painting(UgcTypeV2.Painting),
    Ai(UgcTypeV2.Ai),
    Home(UgcTypeV2.Home),
    Outdoors(UgcTypeV2.Outdoors),
    Gym(UgcTypeV2.Gym),
    Handmake(UgcTypeV2.Handmake),
    Travel(UgcTypeV2.Travel),
    Rural(UgcTypeV2.Rural),
    Parenting(UgcTypeV2.Parenting),
    Health(UgcTypeV2.Health),
    Emotion(UgcTypeV2.Emotion),
    LifeJoy(UgcTypeV2.LifeJoy),
    LifeExperience(UgcTypeV2.LifeExperience),
    Mysticism(UgcTypeV2.Mysticism);

    override fun getDisplayName(context: Context): String {
        return ugcType.getDisplayName(context)
    }
}

enum class PgcTopNavItem(private val pgcType: PgcType) : TopNavItem {
    Anime(PgcType.Anime),
    GuoChuang(PgcType.GuoChuang),
    Movie(PgcType.Movie),
    Documentary(PgcType.Documentary),
    Tv(PgcType.Tv),
    Variety(PgcType.Variety);

    override fun getDisplayName(context: Context): String {
        return pgcType.getDisplayName(context)
    }
}
