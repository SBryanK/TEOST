package com.example.teost.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.work.WorkManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.teost.feature.main.MainScreen
import com.example.teost.feature.auth.ForgotPasswordScreen
import com.example.teost.feature.auth.LoginScreen
import com.example.teost.feature.auth.SignUpScreen
import com.example.teost.presentation.navigation.Screen
import com.example.teost.core.ui.theme.EdgeOneTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the Android 12+ splash screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Keep splash screen visible until we're ready
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        // Emergency timeout to prevent ANR
        lifecycleScope.launch {
            android.util.Log.d("MainActivity", "Splash screen delay started")
            delay(700)
            keepSplashScreen = false
            android.util.Log.d("MainActivity", "Splash screen dismissed")
        }
        
        // Emergency fallback - force dismiss after 5 seconds
        lifecycleScope.launch {
            delay(5000)
            if (keepSplashScreen) {
                android.util.Log.w("MainActivity", "Emergency splash timeout - forcing dismiss")
                keepSplashScreen = false
            }
        }

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val prefs by viewModel.userPreferences.collectAsStateWithLifecycle(
                initialValue = null,
                minActiveState = androidx.lifecycle.Lifecycle.State.CREATED
            )
            
            // Debug logging for prefs loading
            LaunchedEffect(prefs) {
                android.util.Log.d("MainActivity", "Prefs loaded: ${prefs != null}, isLoggedIn: ${prefs?.isLoggedIn}")
            }

            val navController = rememberNavController()
            
            // More reliable authentication state with timeout fallback
            val isLoggedIn = remember(prefs) { 
                try {
                    prefs?.isLoggedIn == true && prefs?.isSessionValid() == true
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Auth state check failed: ${e.message}")
                    false // Default to logged out if error
                }
            }
            
            // Stable start destination - only calculate once when prefs first loads
            val startDestination = remember(prefs) {
                val destination = if (isLoggedIn) Screen.Main.route else Screen.Auth.Login.route
                android.util.Log.d("MainActivity", "Starting with destination: $destination")
                destination
            }
            
            LaunchedEffect(prefs) {
                prefs?.let {
                    try {
                        // Session heartbeat on resume/launch
                        viewModel.updateLastActiveTime()
                        
                        // Trigger cloud sync to restore history if needed
                        if (it.isLoggedIn) {
                            android.util.Log.d("MainActivity", "User logged in, triggering cloud sync")
                            com.example.teost.services.SyncScheduler.runOneTimeNow(this@MainActivity)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MainActivity", "Failed to update last active time or trigger sync: ${e.message}")
                    }
                }
            }
            
            // Emergency timeout for prefs loading
            var showEmergencyLogin by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(10000) // 10 second timeout
                if (prefs == null) {
                    android.util.Log.w("MainActivity", "Preferences loading timeout, forcing login screen")
                    showEmergencyLogin = true
                }
            }

            EdgeOneTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Show loading or navigate based on state
                    when {
                        showEmergencyLogin -> {
                            android.util.Log.d("MainActivity", "Showing emergency login")
                            // Force show login screen
                            LoginScreen(
                                onNavigateToSignUp = { /* Simplified for emergency mode */ },
                                onNavigateToForgotPassword = { /* Simplified for emergency mode */ },
                                onNavigateToEmailVerification = { /* no-op */ },
                                onNavigateToMain = { 
                                    // Restart activity to reset state
                                    recreate()
                                }
                            )
                        }
                        prefs == null -> {
                            // Show minimal loading while prefs load
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        else -> {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination
                            ) {
                                composable(Screen.Auth.Login.route) {
                                    LoginScreen(
                                        onNavigateToSignUp = { navController.navigate(Screen.Auth.SignUp.route) },
                                        onNavigateToForgotPassword = { navController.navigate(Screen.Auth.ForgotPassword.route) },
                                        onNavigateToEmailVerification = { /* no-op */ },
                                        onNavigateToMain = {
                                            navController.navigate(Screen.Main.route) {
                                                popUpTo(Screen.Auth.Login.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable(Screen.Auth.SignUp.route) {
                                    SignUpScreen(
                                        onNavigateToLogin = { navController.popBackStack() },
                                        onNavigateToMain = {
                                            navController.navigate(Screen.Main.route) {
                                                popUpTo(Screen.Auth.Login.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable(Screen.Auth.ForgotPassword.route) {
                                    ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
                                }

                                // EmailVerification route removed per remediation plan

                                composable(Screen.Main.route) { 
                                    val currentUserId = prefs?.userId.orEmpty()
                                    MainScreen(
                                        onLogout = {},
                                        currentUserId = currentUserId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
