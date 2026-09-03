package com.investmentmonitor.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.investmentmonitor.app.data.model.ThemeMode
import com.investmentmonitor.app.ui.navigation.AppNavGraph
import com.investmentmonitor.app.ui.navigation.Destination
import com.investmentmonitor.app.ui.navigation.bottomNavItems
import com.investmentmonitor.app.ui.navigation.isTopLevelRoute
import com.investmentmonitor.app.ui.navigation.rememberAppNavController
import com.investmentmonitor.app.ui.theme.InvestmentMonitorTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled via re-check on resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceLocator = (application as InvestmentMonitorApp).serviceLocator

        setContent {
            val themeMode by serviceLocator.settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val onboardingDone by serviceLocator.settingsRepository.onboardingDone.collectAsState(initial = null)

            InvestmentMonitorTheme(themeMode = themeMode) {
                // Wait for the first DataStore read before deciding the start destination so we
                // never briefly flash onboarding for returning users.
                if (onboardingDone != null) {
                    MainScaffold(
                        serviceLocator = serviceLocator,
                        startDestination = if (onboardingDone == true) Destination.Dashboard.route else Destination.Onboarding.route,
                        onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun MainScaffold(
    serviceLocator: ServiceLocator,
    startDestination: String,
    onRequestNotificationPermission: () -> Unit
) {
    val navController = rememberAppNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute(currentRoute)) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.destination.route,
                            onClick = {
                                navController.navigate(item.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavGraph(
                navController = navController,
                serviceLocator = serviceLocator,
                startDestination = startDestination
            )
        }
    }

    if (startDestination == Destination.Dashboard.route) {
        // Ask for notification permission (rationale is already shown during onboarding) the
        // first time a returning user lands on Dashboard - never on first cold start (spec #42).
        LaunchedEffect(Unit) {
            onRequestNotificationPermission()
        }
    }
}
