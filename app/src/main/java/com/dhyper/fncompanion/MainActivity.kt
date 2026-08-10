package com.dhyper.fncompanion

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import com.dhyper.fncompanion.data.db.SettingsEntity
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.worker.ShopCheckWorker
import com.dhyper.fncompanion.worker.ShopRefreshReceiver
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import com.dhyper.fncompanion.ui.screens.*
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.*
import kotlinx.coroutines.launch

sealed class NavRoute(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object BrHub : NavRoute("br_hub", "BR", Icons.Default.MonetizationOn)
    object Stw : NavRoute("stw", "STW", Icons.Default.Star)
    object MyAccount : NavRoute("my_account", "My Account", Icons.Default.AccountCircle)
    object Settings : NavRoute("settings", "Settings", Icons.Default.Settings)

    // Sub-routes
    object Shop : NavRoute("shop", "Item Shop", Icons.Default.ShoppingBag)
    object Map : NavRoute("map", "Maps", Icons.Default.Map)
    object News : NavRoute("news", "News", Icons.Default.Newspaper)
    object Tracker : NavRoute("tracker", "Tracker", Icons.Default.Leaderboard)
    object Cosmetics : NavRoute("cosmetics", "All Cosmetics", Icons.Default.Shield)
    object Locker : NavRoute("locker", "BR Locker", Icons.Default.Checkroom)
    object Aes : NavRoute("aes", "AES Keys", Icons.Default.VpnKey)
    object Career : NavRoute("career", "Career", Icons.Default.History)
    object AddAccount : NavRoute("add_account", "Add Account", Icons.Default.PersonAdd)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        enableEdgeToEdge()
        scheduleShopWorker()
        ShopRefreshReceiver.scheduleNextAlarm(this)
        requestNotificationPermission()
        setContent {
            val currentSettings by db.settingsDao().getSettings().collectAsState(initial = null)
            FortniteCompanionTheme(
                accentColor = currentSettings?.accentColor ?: "Cyan"
            ) {
                FortniteCompanionApp(currentSettings)
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
fun FortniteCompanionApp(settings: SettingsEntity?) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val authRepository = AuthRepository(db.authDao())

    val authViewModel: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(authRepository) as T
    })
    val statsViewModel: StatsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = StatsViewModel(authRepo = authRepository, settingsDao = db.settingsDao()) as T
    })
    val shopViewModel: ShopViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ShopViewModel(authRepo = authRepository, wishlistDao = db.wishlistDao(), settingsDao = db.settingsDao()) as T
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
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = CosmeticsViewModel(authRepo = authRepository, wishlistDao = db.wishlistDao(), settingsDao = db.settingsDao()) as T
    })
    val settingsViewModel: SettingsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(authRepository = authRepository, authDao = db.authDao(), settingsDao = db.settingsDao()) as T
    })

    val brExtendedViewModel: BrExtendedViewModel = viewModel()

    val authSession by authViewModel.authSession.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoute.BrHub.route

    val isTopLevel = currentRoute in listOf(NavRoute.BrHub.route, NavRoute.Stw.route, NavRoute.MyAccount.route, NavRoute.Settings.route)
    val navItems = listOf(NavRoute.BrHub, NavRoute.Stw, NavRoute.MyAccount, NavRoute.Settings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FORTNITE", fontWeight = FontWeight.Black, fontSize = 18.sp, color = SleekTextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COMPANION", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(NavRoute.MyAccount.route) }) {
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
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = SleekTextMuted,
                            unselectedTextColor = SleekTextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = NavRoute.BrHub.route) {
                composable(NavRoute.BrHub.route) { 
                    BrHubScreen(
                        onNavigateToShop = { navController.navigate(NavRoute.Shop.route) }, 
                        onNavigateToMap = { navController.navigate(NavRoute.Map.route) }, 
                        onNavigateToNews = { navController.navigate(NavRoute.News.route) }, 
                        onNavigateToTracker = { navController.navigate(NavRoute.Tracker.route) },
                        onNavigateToAes = { navController.navigate(NavRoute.Aes.route) },
                        onNavigateToCosmetics = { navController.navigate(NavRoute.Cosmetics.route) }
                    ) 
                }
                composable(NavRoute.Shop.route) { ShopScreen(shopViewModel, cosmeticsViewModel) }
                composable(NavRoute.Map.route) { MapScreen(mapViewModel) }
                composable(NavRoute.News.route) { NewsScreen(newsViewModel) }
                composable(NavRoute.Tracker.route) { StatsLookupScreen(statsViewModel) }
                composable(NavRoute.Cosmetics.route) { CosmeticsScreen(cosmeticsViewModel, shopViewModel) }
                composable(NavRoute.Stw.route) { StwScreen(stwViewModel) }
                composable(NavRoute.MyAccount.route) { 
                    AccountAuthScreen(
                        authViewModel, 
                        settingsViewModel,
                        onNavigateToLocker = { navController.navigate(NavRoute.Locker.route) },
                        onNavigateToCareer = { navController.navigate(NavRoute.Career.route) }
                    ) 
                }
                composable(NavRoute.Locker.route) { PersonalLockerScreen(authSession, lockerViewModel, cosmeticsViewModel, { navController.popBackStack() }) }
                composable(NavRoute.Career.route) { CareerScreen(authViewModel, statsViewModel) }
                composable(NavRoute.Aes.route) { AesScreen(brExtendedViewModel) }
                composable(NavRoute.Settings.route) { 
                    SettingsScreen(
                        authViewModel, 
                        statsViewModel, 
                        settingsViewModel,
                        onAddAccount = { navController.navigate(NavRoute.AddAccount.route) }
                    ) 
                }
                composable(NavRoute.AddAccount.route) {
                    AccountAuthScreen(
                        authViewModel,
                        settingsViewModel,
                        onNavigateToLocker = { navController.popBackStack() },
                        onNavigateToCareer = { navController.popBackStack() },
                        forceLogin = true,
                        onLoginSuccess = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
