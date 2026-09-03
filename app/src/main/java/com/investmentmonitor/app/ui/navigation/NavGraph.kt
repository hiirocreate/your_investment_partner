package com.investmentmonitor.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.ui.companydetail.CompanyDetailScreen
import com.investmentmonitor.app.ui.dashboard.DashboardScreen
import com.investmentmonitor.app.ui.news.NewsListScreen
import com.investmentmonitor.app.ui.onboarding.OnboardingScreen
import com.investmentmonitor.app.ui.search.CompanySearchScreen
import com.investmentmonitor.app.ui.settings.SettingsScreen
import com.investmentmonitor.app.ui.trending.TrendingScreen
import com.investmentmonitor.app.ui.watchlist.WatchlistScreen

sealed class Destination(val route: String) {
    data object Onboarding : Destination("onboarding")
    data object Dashboard : Destination("dashboard")
    data object Watchlist : Destination("watchlist")
    data object News : Destination("news")
    data object Trending : Destination("trending")
    data object Settings : Destination("settings")
    data object Search : Destination("search")
    data object CompanyDetail : Destination("company_detail/{companyId}") {
        fun path(companyId: String) = "company_detail/$companyId"
        const val ARG_COMPANY_ID = "companyId"
    }
}

data class BottomNavItem(val destination: Destination, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Dashboard, "ホーム", Icons.Filled.Home),
    BottomNavItem(Destination.Watchlist, "監視企業", Icons.Filled.Star),
    BottomNavItem(Destination.News, "ニュース", Icons.Filled.Notifications),
    BottomNavItem(Destination.Trending, "注目企業", Icons.Filled.List),
    BottomNavItem(Destination.Settings, "設定", Icons.Filled.Settings)
)

@Composable
fun AppNavGraph(
    navController: NavHostController,
    serviceLocator: ServiceLocator,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destination.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                    }
                },
                serviceLocator = serviceLocator
            )
        }
        composable(Destination.Dashboard.route) {
            DashboardScreen(
                serviceLocator = serviceLocator,
                onCompanyClick = { navController.navigate(Destination.CompanyDetail.path(it)) },
                onSearchClick = { navController.navigate(Destination.Search.route) },
                onSeeAllNews = { navController.navigate(Destination.News.route) },
                onSeeAllTrending = { navController.navigate(Destination.Trending.route) }
            )
        }
        composable(Destination.Watchlist.route) {
            WatchlistScreen(
                serviceLocator = serviceLocator,
                onCompanyClick = { navController.navigate(Destination.CompanyDetail.path(it)) },
                onAddClick = { navController.navigate(Destination.Search.route) }
            )
        }
        composable(Destination.News.route) {
            NewsListScreen(serviceLocator = serviceLocator, onCompanyClick = { navController.navigate(Destination.CompanyDetail.path(it)) })
        }
        composable(Destination.Trending.route) {
            TrendingScreen(serviceLocator = serviceLocator, onCompanyClick = { navController.navigate(Destination.CompanyDetail.path(it)) })
        }
        composable(Destination.Settings.route) {
            SettingsScreen(serviceLocator = serviceLocator)
        }
        composable(Destination.Search.route) {
            CompanySearchScreen(
                serviceLocator = serviceLocator,
                onBack = { navController.popBackStack() },
                onRegistered = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.CompanyDetail.route,
            arguments = listOf(navArgument(Destination.CompanyDetail.ARG_COMPANY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString(Destination.CompanyDetail.ARG_COMPANY_ID).orEmpty()
            CompanyDetailScreen(companyId = companyId, serviceLocator = serviceLocator, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

/** Helper for the bottom bar to know the "current top-level tab" even on nested destinations. */
fun isTopLevelRoute(route: String?): Boolean = bottomNavItems.any { it.destination.route == route }
