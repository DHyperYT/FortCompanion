package com.dhyper.fncompanion

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.work.*
import com.dhyper.fncompanion.data.db.AppDatabase
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.worker.ShopCheckWorker
import com.dhyper.fncompanion.worker.ShopRefreshReceiver
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import com.dhyper.fncompanion.ui.screens.*
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Custom Cubic Bezier for smoother Fortnite-style movement
val ExpoEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

sealed class NavRoute(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object BrHub : NavRoute("br_hub", "BR", Icons.Default.MonetizationOn)
    object Stw : NavRoute("stw", "STW", Icons.Default.Star)
    object Account : NavRoute("account", "Account", Icons.Default.AccountCircle)

    // Sub-routes
    object Shop : NavRoute("shop", "Item Shop", Icons.Default.ShoppingBag)
    object Map : NavRoute("map", "Maps", Icons.Default.Map)
    object News : NavRoute("news", "News", Icons.Default.Newspaper)
    object Tracker : NavRoute("tracker", "Tracker", Icons.Default.Leaderboard)
    object Cosmetics : NavRoute("cosmetics", "Cosmetics", Icons.Default.Shield)
    object Locker : NavRoute("locker", "BR Locker", Icons.Default.Checkroom)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleShopWorker()
        ShopRefreshReceiver.scheduleNextAlarm(this)
        requestNotificationPermission()
        setContent {
            FortniteCompanionTheme {
                FortniteCompanionApp()
            }
        }
    }

    private fun scheduleShopWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 1)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var nextReset = calendar.timeInMillis
        if (nextReset <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            nextReset = calendar.timeInMillis
        }
        val initialDelay = nextReset - now

        val request = PeriodicWorkRequestBuilder<ShopCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ShopCheckWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortniteCompanionApp() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val authRepository = AuthRepository(db.authDao())

    // --- INTRO ANIMATION STATE ---
    var showIntro by remember { mutableStateOf(true) }
    val introAnim = remember { Animatable(0f) } 
    val alphaAnim = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    var displayedText by remember { mutableStateOf("") }
    val fullText = "FORTNITE COMPANION"

    LaunchedEffect(Unit) {
        // 1. Typing effect
        alphaAnim.snapTo(1f)
        for (i in 1..fullText.length) {
            displayedText = fullText.substring(0, i)
            delay(60) 
        }
        delay(600)
        
        // 2. Continuous transition: text move and content fade start together
        launch {
            introAnim.animateTo(1f, animationSpec = tween(1500, easing = ExpoEaseOut))
            showIntro = false // Swap splash with actual Scaffold at the exact end
        }
        launch {
            delay(300) // Content starts fading in mid-move
            contentAlpha.animateTo(1f, animationSpec = tween(1000, easing = LinearEasing))
        }
    }

    val authViewModel: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(authRepository) as T
    })
    val statsViewModel: StatsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = StatsViewModel(authRepo = authRepository) as T
    })
    val shopViewModel: ShopViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ShopViewModel(authRepo = authRepository, wishlistDao = db.wishlistDao()) as T
    })
    val mapViewModel: MapViewModel = viewModel()
    val newsViewModel: NewsViewModel = viewModel()
    val stwViewModel: StwViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = StwViewModel(authRepo = authRepository) as T
    })
    val lockerViewModel: PersonalLockerViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = PersonalLockerViewModel(authRepository = authRepository) as T
    })
    val cosmeticsViewModel: CosmeticsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = CosmeticsViewModel(authRepo = authRepository, wishlistDao = db.wishlistDao()) as T
    })

    val authSession by authViewModel.authSession.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoute.BrHub.route

    val topLevelRoutes = listOf(NavRoute.BrHub.route, NavRoute.Stw.route, NavRoute.Account.route)
    val isTopLevel = currentRoute in topLevelRoutes
    val navItems = listOf(NavRoute.BrHub, NavRoute.Stw, NavRoute.Account)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.alpha(contentAlpha.value),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("FORTNITE", fontWeight = FontWeight.Black, fontSize = 18.sp, color = SleekTextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COMPANION", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SleekCyan)
                        }
                    },
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SleekCyan)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(NavRoute.Account.route) }) {
                            if (authSession != null) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekEmerald)
                            else Icon(Icons.Default.Lock, contentDescription = null, tint = SleekTextMuted)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekSurface),
                    modifier = Modifier.drawBehind {
                        val y = size.height - 1.dp.toPx() / 2
                        drawLine(color = SleekSurfaceBorder, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = SleekSurface, modifier = Modifier.navigationBarsPadding()) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { navController.navigate(item.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = SleekCyan, selectedTextColor = SleekCyan, indicatorColor = SleekPrimary.copy(alpha = 0.25f), unselectedIconColor = SleekTextMuted, unselectedTextColor = SleekTextMuted)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                NavHost(navController = navController, startDestination = NavRoute.BrHub.route) {
                    composable(NavRoute.BrHub.route) { BrHubScreen({ navController.navigate(NavRoute.Shop.route) }, { navController.navigate(NavRoute.Map.route) }, { navController.navigate(NavRoute.News.route) }, { navController.navigate(NavRoute.Tracker.route) }) }
                    composable(NavRoute.Shop.route) { ShopScreen(shopViewModel, cosmeticsViewModel, { navController.navigate(NavRoute.Cosmetics.route) }) }
                    composable(NavRoute.Map.route) { MapScreen(mapViewModel) }
                    composable(NavRoute.News.route) { NewsScreen(newsViewModel) }
                    composable(NavRoute.Tracker.route) { StatsLookupScreen(statsViewModel) }
                    composable(NavRoute.Cosmetics.route) { CosmeticsScreen(cosmeticsViewModel) }
                    composable(NavRoute.Stw.route) { StwScreen(stwViewModel) }
                    composable(NavRoute.Account.route) { AccountAuthScreen(authViewModel, { navController.navigate(NavRoute.Locker.route) }) }
                    composable(NavRoute.Locker.route) { PersonalLockerScreen(authSession, lockerViewModel, cosmeticsViewModel, { navController.popBackStack() }) }
                }
            }
        }

        if (showIntro || introAnim.value < 1f) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SleekBackground.copy(alpha = 1f - contentAlpha.value)),
                contentAlignment = Alignment.TopStart
            ) {
                val startSize = 34.sp
                val endSize = 18.sp
                val currentFontSize = lerp(startSize.value, endSize.value, introAnim.value).sp

                val density = LocalDensity.current
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                
                val startX = with(density) { (maxWidth.toPx() / 2) }
                val startY = with(density) { (maxHeight.toPx() / 2) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .alpha(if (introAnim.value > 0.99f) 0f else alphaAnim.value)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                val adjustedStartX = startX - (placeable.width / 2f)
                                val adjustedStartY = startY - (placeable.height / 2f)
                                
                                // Standard TopAppBar height is 64dp.
                                val barHeight = with(density) { 64.dp.toPx() }
                                val targetX = with(density) { 16.dp.toPx() }
                                // Center vertically within the TopAppBar (below status bar)
                                val targetY = with(density) { 
                                    statusBarHeight.toPx() + (barHeight / 2f) - (placeable.height / 2f)
                                }

                                val x = lerp(adjustedStartX, targetX, introAnim.value)
                                val y = lerp(adjustedStartY, targetY, introAnim.value)
                                
                                placeable.placeRelative(x.toInt(), y.toInt())
                            }
                        }
                ) {
                    val splitIndex = if (displayedText.length >= 8) 8 else displayedText.length
                    val part1 = displayedText.substring(0, splitIndex)
                    val part2 = if (displayedText.length > 8) displayedText.substring(8) else ""

                    Text(
                        text = part1,
                        style = TextStyle(
                            fontFamily = FortniteFont,
                            fontWeight = FontWeight.Black,
                            fontSize = currentFontSize,
                            color = SleekTextPrimary
                        )
                    )
                    if (part2.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = part2.trim(),
                            style = TextStyle(
                                fontFamily = FortniteFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = currentFontSize,
                                color = SleekCyan
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + fraction * (stop - start)
