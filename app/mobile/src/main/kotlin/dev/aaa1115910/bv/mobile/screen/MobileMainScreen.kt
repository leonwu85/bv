package dev.aaa1115910.bv.mobile.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.repositories.UserRepository as BiliUserRepository
import dev.aaa1115910.bv.mobile.activities.SettingsActivity
import dev.aaa1115910.bv.mobile.component.ImagePreviewerActions
import dev.aaa1115910.bv.mobile.screen.home.DynamicScreen
import dev.aaa1115910.bv.mobile.screen.home.HomeScreen
import dev.aaa1115910.bv.mobile.screen.home.MineScreen
import dev.aaa1115910.bv.mobile.screen.home.SearchScreen
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserSwitchViewModel
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MobileMainScreen(
    modifier: Modifier = Modifier,
    popularViewModel: PopularViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel(),
    userSwitchViewModel: UserSwitchViewModel = koinViewModel(),
    biliUserRepository: BiliUserRepository = koinInject()
) {
    val logger = KotlinLogging.logger("MobileMainScreen")
    val state = rememberMobileMainScreenState(
        popularViewModel = popularViewModel,
        userViewModel = userViewModel,
        userSwitchViewModel = userSwitchViewModel
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

    val pictures = remember { mutableStateListOf<Picture>() }
    var savingPreviewImage by remember { mutableStateOf(false) }
    var dynamicUnreadCount by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = { pictures.size },
        getKey = { pictures[it].key }
    )

    fun refreshDynamicUnreadCount() {
        if (!userViewModel.isLogin) {
            dynamicUnreadCount = 0
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                biliUserRepository.getDynamicUnreadCount()
            }.onSuccess { count ->
                withContext(Dispatchers.Main) {
                    dynamicUnreadCount = count
                }
            }.onFailure {
                logger.warn(it) { "Load dynamic unread count failed" }
            }
        }
    }

    LaunchedEffect(userViewModel.isLogin) {
        refreshDynamicUnreadCount()
    }

    LaunchedEffect(state.currentNavItem) {
        if (state.currentNavItem == MobileMainScreenNav.Dynamic) {
            dynamicUnreadCount = 0
        }
    }

    DisposableEffect(lifecycleOwner, userViewModel.isLogin) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshDynamicUnreadCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit =
        { newPictures, afterSetPictures ->
            pictures.swapList(newPictures)
            logger.fInfo { "update image previewer pictures list: $newPictures" }
            afterSetPictures()
        }

    val verticalNavOrder = listOf(
        MobileMainScreenNav.Home,
        MobileMainScreenNav.Search,
        MobileMainScreenNav.Dynamic,
        MobileMainScreenNav.Mine,
        MobileMainScreenNav.Setting
    ).map { it.name }
    val horizontalNavOrder = listOf(
        MobileMainScreenNav.Home,
        MobileMainScreenNav.Dynamic,
        MobileMainScreenNav.Setting
    ).map { it.name }

    val compareNavIndex: (String?, String?) -> Boolean = { a, b ->
        if (navSuiteType == NavigationSuiteType.NavigationBar) {
            horizontalNavOrder.indexOf(a) < horizontalNavOrder.indexOf(b)
        } else {
            verticalNavOrder.indexOf(a) < verticalNavOrder.indexOf(b)
        }
    }

    val navEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            val coefficient = 10
            if (navSuiteType == NavigationSuiteType.NavigationBar) {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeIn() + slideInHorizontally { -it / coefficient }
                } else {
                    fadeIn() + slideInHorizontally { it / coefficient }
                }
            } else {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeIn() + slideInVertically { -it / coefficient }
                } else {
                    fadeIn() + slideInVertically { it / coefficient }
                }
            }
        }

    val navExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        {
            val coefficient = 10
            if (navSuiteType == NavigationSuiteType.NavigationBar) {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeOut() + slideOutHorizontally { it / coefficient }
                } else {
                    fadeOut() + slideOutHorizontally { -it / coefficient }
                }
            } else {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeOut() + slideOutVertically { it / coefficient }
                } else {
                    fadeOut() + slideOutVertically { -it / coefficient }
                }
            }
        }

    BackHandler(previewerState.canClose || previewerState.animating) {
        if (previewerState.canClose) scope.launch {
            previewerState.closeTransform()
        }
    }

    val navHostContent: @Composable () -> Unit = {
        NavHost(
            navController = state.navController,
            startDestination = MobileMainScreenNav.Home.name,
            enterTransition = navEnterTransition,
            exitTransition = navExitTransition
        ) {
            composable(MobileMainScreenNav.Home.name) {
                HomeScreen(
                    rcmdGridState = state.rcmdGridState,
                    popularGridState = state.popularGridState,
                    windowSize = state.windowSizeClass.widthSizeClass,
                    onOpenSearch = { state.navigate(MobileMainScreenNav.Search) },
                    onOpenMine = { state.navigate(MobileMainScreenNav.Mine) }
                )
            }

            composable(MobileMainScreenNav.Dynamic.name) {
                BackHandler(previewerState.canClose || previewerState.animating) {
                    if (previewerState.canClose) scope.launch {
                        previewerState.closeTransform()
                    }
                }

                DynamicScreen(
                    dynamicGridState = state.dynamicGridState,
                    previewerState = previewerState,
                    onShowPreviewer = onShowPreviewer,
                    // dynamicViewModel = dynamicViewModel
                )
            }

            composable(MobileMainScreenNav.Search.name) {
                SearchScreen()
            }
            composable(MobileMainScreenNav.Mine.name) {
                MineScreen(
                    windowSize = state.windowSizeClass.widthSizeClass,
                    userViewModel = userViewModel,
                    userSwitchViewModel = userSwitchViewModel,
                    onBack = {
                        if (!state.navController.popBackStack()) {
                            state.navigate(MobileMainScreenNav.Home)
                        }
                    }
                )
            }
        }
    }

    Box(
        modifier = modifier,
    ) {
        if (state.currentNavItem == MobileMainScreenNav.Mine) {
            navHostContent()
        } else {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    NavigationSuit(
                        mobileMainScreenState = state,
                        navigationSuiteType = navSuiteType,
                        avatar = userViewModel.face,
                        dynamicUnreadCount = dynamicUnreadCount,
                        onNavigate = { navItem ->
                            if (navItem == MobileMainScreenNav.Dynamic) {
                                dynamicUnreadCount = 0
                            }
                            state.navigate(navItem)
                        },
                        onOpenMine = { state.navigate(MobileMainScreenNav.Mine) }
                    )
                }
            ) {
                navHostContent()
            }
        }
    }

    ImagePreviewer(
        modifier = Modifier
            .fillMaxSize(),
        state = previewerState,
        imageLoader = { index ->
            val imageRequest = ImageRequest.Builder(LocalContext.current)
                .data(pictures[index].url)
                .size(Size.ORIGINAL)
                .build()
            rememberAsyncImagePainter(imageRequest)
        },
        previewerLayer = {
            foreground = { page ->
                ImagePreviewerActions(
                    saving = savingPreviewImage,
                    onClose = {
                        if (previewerState.canClose) {
                            scope.launch {
                                previewerState.closeTransform()
                            }
                        }
                    },
                    onSave = {
                        val picture = pictures.getOrNull(page)
                        if (picture == null) {
                            "图片不存在".toast(context)
                            return@ImagePreviewerActions
                        }
                        if (savingPreviewImage) return@ImagePreviewerActions
                        scope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                savingPreviewImage = true
                            }
                            runCatching {
                                saveImageToGallery(context, picture.url)
                            }.onSuccess {
                                withContext(Dispatchers.Main) {
                                    "图片已保存到相册".toast(context)
                                }
                            }.onFailure {
                                logger.warn(it) { "Save dynamic preview image failed" }
                                withContext(Dispatchers.Main) {
                                    "保存失败：${it.localizedMessage ?: "未知错误"}".toast(context)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                savingPreviewImage = false
                            }
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun NavigationSuit(
    modifier: Modifier = Modifier,
    mobileMainScreenState: MobileMainScreenState,
    navigationSuiteType: NavigationSuiteType,
    avatar: String,
    dynamicUnreadCount: Int,
    onNavigate: (MobileMainScreenNav) -> Unit,
    onOpenMine: () -> Unit,
) {
    when (navigationSuiteType) {
        NavigationSuiteType.NavigationBar -> {
            NavigationSuite(
                modifier = modifier
            ) {
                listOf(
                    MobileMainScreenNav.Home,
                    MobileMainScreenNav.Dynamic,
                    MobileMainScreenNav.Setting,
                ).forEach { navItem ->
                    item(
                        icon = {
                            NavigationIcon(
                                navItem = navItem,
                                dynamicUnreadCount = dynamicUnreadCount
                            )
                        },
                        label = { Text(navItem.displayName) },
                        selected = mobileMainScreenState.currentNavItem == navItem,
                        onClick = { onNavigate(navItem) }
                    )
                }
            }
        }

        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationRailItem(
                    icon = {
                        if (avatar.isBlank()) {
                            Icon(Icons.Rounded.Person, contentDescription = "User Avatar")
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            ) {
                                AsyncImage(
                                    modifier = Modifier
                                        .size(36.dp),
                                    model = avatar,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    },
                    selected = false,
                    onClick = onOpenMine
                )
                NavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = MobileMainScreenNav.Search.icon,
                            contentDescription = MobileMainScreenNav.Search.displayName
                        )
                    },
                    selected = mobileMainScreenState.currentNavItem == MobileMainScreenNav.Search,
                    onClick = { onNavigate(MobileMainScreenNav.Search) }
                )
                Spacer(Modifier.weight(1f))
                listOf(
                    MobileMainScreenNav.Home,
                    MobileMainScreenNav.Dynamic,
                    MobileMainScreenNav.Setting,
                ).forEach { navItem ->
                    NavigationRailItem(
                        icon = {
                            NavigationIcon(
                                navItem = navItem,
                                dynamicUnreadCount = dynamicUnreadCount
                            )
                        },
                        label = { Text(navItem.displayName) },
                        selected = mobileMainScreenState.currentNavItem == navItem,
                        onClick = { onNavigate(navItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationIcon(
    navItem: MobileMainScreenNav,
    dynamicUnreadCount: Int
) {
    if (navItem == MobileMainScreenNav.Dynamic && dynamicUnreadCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = if (dynamicUnreadCount > 99) "99+" else dynamicUnreadCount.toString())
                }
            }
        ) {
            Icon(navItem.icon, contentDescription = navItem.displayName)
        }
    } else {
        Icon(navItem.icon, contentDescription = navItem.displayName)
    }
}

data class MobileMainScreenState(
    val context: Context,
    val scope: CoroutineScope,
    val windowSizeClass: WindowSizeClass,
    val rcmdGridState: LazyGridState,
    val popularGridState: LazyGridState,
    val dynamicGridState: LazyStaggeredGridState,
    val navController: NavHostController,
    val currentBackStackEntry: NavBackStackEntry?,
    val currentNavItem: MobileMainScreenNav,
    private val homeViewModel: PopularViewModel,
    private val userViewModel: UserViewModel,
    private val userSwitchViewModel: UserSwitchViewModel,
) {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    var activeSearch by mutableStateOf(false)

    fun navigate(navItem: MobileMainScreenNav) {
        logger.fInfo { "Navigate to ${navItem.name}" }

        val navigateToRoute: () -> Unit = {
            val route = navItem.name
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                    saveState = true
                }
                restoreState = true
            }
        }

        val notCurrentNavItem = currentNavItem != navItem

        when (navItem) {
            MobileMainScreenNav.Home -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                } else {
                    scope.launch { rcmdGridState.animateScrollToItem(0) }
                    scope.launch { popularGridState.animateScrollToItem(0) }
                }
            }

            MobileMainScreenNav.Search -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                }
            }

            MobileMainScreenNav.Setting -> {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }

            MobileMainScreenNav.Dynamic -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                } else {
                    scope.launch { dynamicGridState.animateScrollToItem(0) }
                }
            }

            MobileMainScreenNav.Mine -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                }
            }
        }

        @SuppressLint("RestrictedApi")
        val breadcrumb = navController
            .currentBackStack
            .value
            .map { it.destination }
            .filterNot { it is NavGraph }
            .joinToString(" > ") { it.route ?: "null" }
        logger.fInfo { "Navigation Stack: > $breadcrumb" }
    }

}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberMobileMainScreenState(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    windowSizeClass: WindowSizeClass = calculateWindowSizeClass(context as Activity),
    rcmdGridState: LazyGridState = rememberLazyGridState(),
    popularGridState: LazyGridState = rememberLazyGridState(),
    dynamicGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    navController: NavHostController = rememberNavController(),
    popularViewModel: PopularViewModel,//= koinNavViewModel(),
    userViewModel: UserViewModel,//= koinNavViewModel(),
    userSwitchViewModel: UserSwitchViewModel //= koinNavViewModel()
): MobileMainScreenState {
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavItem by remember {
        derivedStateOf {
            MobileMainScreenNav.fromName(currentBackStackEntry?.destination?.route ?: "")
        }
    }

    LaunchedEffect(Unit) {
        if (popularViewModel.popularVideoList.isNotEmpty()) {
            scope.launch(Dispatchers.IO) { popularViewModel.loadMore() }
        }
    }

    LaunchedEffect(Unit) {
        userViewModel.updateUserInfo()
    }

    DisposableEffect(lifecycleOwner) {
        var leaveFromThisPage = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                leaveFromThisPage = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (leaveFromThisPage) {
                    scope.launch(Dispatchers.IO) {
                        userSwitchViewModel.updateUserDbList()
                    }
                }
                leaveFromThisPage = false
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return remember(
        context,
        scope,
        windowSizeClass,
        rcmdGridState,
        popularGridState,
        dynamicGridState,
        navController,
        currentNavItem
    ) {
        MobileMainScreenState(
            context,
            scope,
            windowSizeClass,
            rcmdGridState,
            popularGridState,
            dynamicGridState,
            navController,
            currentBackStackEntry,
            currentNavItem,
            popularViewModel,
            userViewModel,
            userSwitchViewModel
        )
    }
}

enum class MobileMainScreenNav(val displayName: String, val icon: ImageVector) {
    Home("首页", Icons.Rounded.Home),
    Search("搜索", Icons.Rounded.Search),
    Dynamic("动态", Icons.Rounded.FiberNew),
    Mine("我的", Icons.Rounded.Person),
    Setting("设置", Icons.Rounded.Settings), ;

    companion object {
        fun fromName(name: String) = entries.firstOrNull { it.name == name } ?: Home
    }
}
