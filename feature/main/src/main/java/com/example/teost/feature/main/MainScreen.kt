package com.example.teost.feature.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.compose.navigation
import com.example.teost.core.ui.R
import com.example.teost.presentation.navigation.Screen
import com.example.teost.presentation.screens.history.HistoryScreen
import com.example.teost.presentation.screens.profile.ProfileScreen
import com.example.teost.presentation.screens.profile.CreditsScreen
import com.example.teost.feature.search.SearchScreen
import com.example.teost.presentation.screens.test.TestScreen
// Import test composables directly
import com.example.teost.presentation.screens.test.TestTypeScreen
import com.example.teost.presentation.screens.test.TestConfigureScreen
import com.example.teost.presentation.screens.test.CartScreen
import com.example.teost.presentation.screens.test.ConfirmationScreen
import com.example.teost.presentation.screens.test.TestFlowViewModel
import com.example.teost.core.ui.theme.*

data class BottomNavItem(
    val route: String,
    val startRoute: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    currentUserId: String = ""
) {
    val navController = rememberNavController()
    var lastRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var testTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Bottom.Search.route,
            startRoute = Screen.Bottom.Search.route,
            title = stringResource(R.string.nav_search),
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search
        ),
        BottomNavItem(
            route = Screen.Bottom.Test.route,
            startRoute = Screen.TestFlow.CategorySelect.route,
            title = stringResource(R.string.nav_test),
            selectedIcon = Icons.Filled.Build,
            unselectedIcon = Icons.Outlined.Build
        ),
        BottomNavItem(
            route = Screen.Bottom.History.route,
            startRoute = Screen.HistoryFlow.Main.route,
            title = stringResource(R.string.nav_history),
            selectedIcon = Icons.Filled.DateRange,
            unselectedIcon = Icons.Outlined.DateRange
        ),
        BottomNavItem(
            route = Screen.Bottom.Profile.route,
            startRoute = Screen.ProfileFlow.Main.route,
            title = stringResource(R.string.nav_profile),
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    val isInTestGraph = currentDestination?.hierarchy?.any { it.route == Screen.Bottom.Test.route } == true

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            val transition = updateTransition(targetState = currentDestination?.route, label = "bottom_nav")
            val bgColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    containerColor = bgColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (selected) {
                                    val popped = navController.popBackStack(item.startRoute, inclusive = false)
                                    if (!popped) {
                                        navController.navigate(item.startRoute) { launchSingleTop = true }
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                AnimatedContent(
                                    targetState = selected,
                                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                    label = "nav_icon"
                                ) { isSelected ->
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        },
        topBar = {},
        floatingActionButton = {
            val isInCart = currentDestination?.route == Screen.TestFlow.Cart.route
            val isInConfirmation = currentDestination?.route == Screen.TestFlow.Confirmation.route
            val isInSuccess = currentDestination?.route == Screen.TestFlow.Success.route
            if (isInTestGraph && !isInCart && !isInConfirmation && !isInSuccess) {
                val cartCount by com.example.teost.presentation.screens.test.TestCartStore.items.collectAsState()
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.TestFlow.Cart.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    BadgedBox(badge = {
                        if (cartCount.isNotEmpty()) {
                            Badge { Text(cartCount.size.toString()) }
                        }
                    }) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(id = R.string.cart))
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        // Save last route on change
        LaunchedEffect(currentDestination?.route) {
            lastRoute = currentDestination?.route
        }
        // Restore last route after recreate (e.g., locale change)
        LaunchedEffect(Unit) {
            val route = lastRoute
            if (!route.isNullOrBlank() && route != Screen.Bottom.Search.route) {
                // Try to restore into the same destination
                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        NavHost(
            navController = navController,
            startDestination = Screen.Bottom.Search.route,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(220)
                ) + fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(180))
            }
        ) {
            composable(Screen.Bottom.Search.route) {
                SearchScreen(
                    onNavigateToTests = { target: String? ->
                        if (!target.isNullOrBlank()) {
                            testTarget = target
                            navController.navigate(Screen.Bottom.Test.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNavigateToTestsWithTargets = { target: String?, targets: List<String> ->
                        // Persist available targets for Test flow
                        navController.currentBackStackEntry?.savedStateHandle?.set("available_targets", targets.distinct())
                        if (!target.isNullOrBlank()) {
                            testTarget = target
                        }
                        navController.navigate(Screen.Bottom.Test.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                // Wire userId into SearchViewModel once per visit
                val searchVm: com.example.teost.feature.search.SearchViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                LaunchedEffect(currentUserId) {
                    if (currentUserId.isNotBlank()) searchVm.setUserId(currentUserId)
                }
            }

            navigation(startDestination = Screen.TestFlow.CategorySelect.route, route = Screen.Bottom.Test.route) {
                composable(Screen.TestFlow.CategorySelect.route) {
                    // Hydrate cart once when entering Test graph
                    val cartPersistVm: com.example.teost.presentation.screens.test.CartPersistenceViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    LaunchedEffect(Unit) { cartPersistVm.hydrate() }
                    // Pull available targets from previous (Search) and seed into TestFlowViewModel once
                    val flowVm: com.example.teost.presentation.screens.test.TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val savedTargets = navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>("available_targets")
                    LaunchedEffect(savedTargets) {
                        if (!savedTargets.isNullOrEmpty()) flowVm.setAvailableTargets(savedTargets)
                    }
                    com.example.teost.presentation.screens.test.TestScreen(
                        onNavigateToCategory = { category ->
                            val base = Screen.TestFlow.TypeSelect.route.replace("{category}", category)
                            val route = if (!testTarget.isNullOrBlank()) base.replace("{target}", testTarget!!) else base.replace("?target={target}", "")
                            navController.navigate(route)
                        }
                    )
                }

                composable(
                    route = Screen.TestFlow.TypeSelect.route,
                    arguments = listOf(
                        navArgument("category") { type = NavType.StringType },
                        navArgument("target") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category").orEmpty()
                    val target = backStackEntry.arguments?.getString("target")
                    val flowVm: com.example.teost.presentation.screens.test.TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    
                    LaunchedEffect(Unit) {
                        flowVm.pushToNavigationStack("TypeSelect")
                        flowVm.setCategory(category)
                        flowVm.setTarget(target)
                    }
                    
                    TestTypeScreen(
                        category = category,
                        target = target,
                        onNavigateToConfigure = { type ->
                            flowVm.setType(type)
                            flowVm.pushToNavigationStack("Configure")
                            val route = Screen.TestFlow.Configure.route
                                .replace("{category}", category)
                                .replace("{type}", type)
                                .let { r -> if (target.isNullOrBlank()) r.replace("?target={target}", "") else r.replace("{target}", target) }
                            navController.navigate(route)
                        },
                        onNavigateBack = { 
                            val lastScreen = flowVm.popFromNavigationStack()
                            navController.popBackStack(Screen.TestFlow.CategorySelect.route, false)
                        }
                    )
                }

                composable(
                    route = Screen.TestFlow.Configure.route,
                    arguments = listOf(
                        navArgument("category") { type = NavType.StringType },
                        navArgument("type") { type = NavType.StringType },
                        navArgument("target") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category").orEmpty()
                    val testType = backStackEntry.arguments?.getString("type").orEmpty()
                    val target = backStackEntry.arguments?.getString("target")
                    val flowVm: com.example.teost.presentation.screens.test.TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    
                    LaunchedEffect(Unit) {
                        flowVm.pushToNavigationStack("Configure")
                        flowVm.setCategory(category)
                        flowVm.setType(testType)
                        flowVm.setTarget(target)
                    }
                    
                    TestConfigureScreen(
                        category = category,
                        testType = testType,
                        target = target,
                        onAddToCart = { 
                            flowVm.pushToNavigationStack("Cart")
                            navController.navigate(Screen.TestFlow.Cart.route)
                        },
                        onNavigateBack = { 
                            // Always go back to previous screen in navigation stack
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.TestFlow.Cart.route) {
                    val flowVm: com.example.teost.presentation.screens.test.TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    LaunchedEffect(Unit) {
                        flowVm.pushToNavigationStack("Cart")
                    }
                    
                    CartScreen(
                        onNavigateToConfirmation = { 
                            flowVm.pushToNavigationStack("Confirmation")
                            navController.navigate(Screen.TestFlow.Confirmation.route) 
                        },
                        onNavigateBack = { 
                            // Always go back to previous screen in navigation stack
                            navController.popBackStack()
                        },
                        onNavigateToConfiguration = { category, type, target ->
                            flowVm.pushToNavigationStack("Configure")
                            // Navigate to configuration page with parameters
                            val route = Screen.TestFlow.Configure.route
                                .replace("{category}", category)
                                .replace("{type}", type)
                                .replace("{target}", target)
                            navController.navigate(route)
                        }
                    )
                }
                composable(Screen.TestFlow.Confirmation.route) {
                    val flowVm: TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    ConfirmationScreen(
                        onStartTest = { testIds ->
                            flowVm.setLastTestIds(testIds)
                            navController.currentBackStackEntry?.savedStateHandle?.set("last_test_ids", testIds)
                        },
                        onCompleted = {
                            // Navigate to Success screen when all tests complete
                            navController.navigate(Screen.TestFlow.Success.route)
                        },
                        onNavigateBack = {
                            // Return to Cart screen
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.TestFlow.Execution.route) {
                    // Tracking is started inside TestExecutionScreen's ViewModel after it reads ids from TestFlowViewModel
                    com.example.teost.presentation.screens.test.TestExecutionScreen(
                        onNavigateToSuccess = {
                            navController.navigate(Screen.TestFlow.Success.route)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.TestFlow.Success.route) {
                    val testIds = navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>("last_test_ids") ?: emptyList()
                    com.example.teost.presentation.screens.test.TestSuccessScreen(
                        testIds = testIds,
                        onGoToHistory = {
                            navController.navigate(Screen.Bottom.History.route) {
                                popUpTo(Screen.Bottom.Test.route) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        onBackToTest = {
                            navController.popBackStack(Screen.TestFlow.CategorySelect.route, inclusive = false)
                        }
                    )
                }

                
            }

            // Removed duplicate wizard routes outside nested graph
            navigation(startDestination = Screen.HistoryFlow.Main.route, route = Screen.Bottom.History.route) {
                composable(Screen.HistoryFlow.Main.route) {
                    HistoryScreen(
                        onNavigateToDetail = { resultId ->
                            val route = Screen.HistoryFlow.Detail.route.replace("{resultId}", resultId)
                            navController.navigate(route)
                        },
                        onNavigateToExecution = { /* no-op: Execution screen deprecated for overlay */ }
                    )
                }
                composable(
                    route = Screen.HistoryFlow.Detail.route,
                    arguments = listOf(navArgument("resultId") { type = NavType.StringType }),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "edgeone://history/detail/{resultId}" }
                    )
                ) { backStackEntry ->
                    val resultId = backStackEntry.arguments?.getString("resultId").orEmpty()
                    com.example.teost.presentation.screens.history.HistoryDetailScreen(
                        resultId = resultId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            navigation(startDestination = Screen.ProfileFlow.Main.route, route = Screen.Bottom.Profile.route) {
                composable(Screen.ProfileFlow.Main.route) {
                    ProfileScreen(
                        onNavigateToCredits = { navController.navigate(Screen.ProfileFlow.Credits.route) },
                        onNavigateToHelp = { navController.navigate(Screen.ProfileFlow.Help.route) },
                        onNavigateToPrivacy = { navController.navigate(Screen.ProfileFlow.Privacy.route) },
                        onLogout = onLogout
                    )
                }
                composable(Screen.ProfileFlow.Credits.route) {
                    CreditsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                // Favorites and Settings removed per product decision
                composable(Screen.ProfileFlow.Help.route) {
                    com.example.teost.presentation.screens.profile.HelpScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.ProfileFlow.Privacy.route) {
                    com.example.teost.presentation.screens.profile.PrivacyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            // About removed per Profile 4-card spec

            // moved History detail into History nested graph

            // Removed duplicate Execution route (kept inside Test nested graph)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    EdgeOneTheme {
        MainScreen()
    }
}
