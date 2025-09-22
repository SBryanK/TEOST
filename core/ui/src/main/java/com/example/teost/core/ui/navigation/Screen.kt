package com.example.teost.presentation.navigation

sealed class Screen(val route: String) {
    
    // Splash
    data object Splash : Screen("splash")
    
    // Main Navigation
    data object Main : Screen("main")
    
    // Auth Screens
    sealed class Auth(route: String) : Screen(route) {
        data object Login : Auth("auth/login")
        data object SignUp : Auth("auth/signup")
        data object ForgotPassword : Auth("auth/forgot_password")
        data object EmailVerification : Auth("auth/email_verification")
    }
    
    // Bottom Navigation Screens
    sealed class Bottom(route: String) : Screen(route) {
        data object Search : Bottom("bottom/search")
        data object Test : Bottom("bottom/test")
        data object History : Bottom("bottom/history")
        data object Profile : Bottom("bottom/profile")
    }
    
    // Test Flow Screens
    sealed class TestFlow(route: String) : Screen(route) {
        data object CategorySelect : TestFlow("test/category")
        // Optional query param 'target' to carry validated domain/ip/url into test flow
        data object TypeSelect : TestFlow("test/type/{category}?target={target}")
        data object Configure : TestFlow("test/configure/{category}/{type}?target={target}")
        data object Cart : TestFlow("test/cart")
        data object Confirmation : TestFlow("test/confirmation")
        data object Execution : TestFlow("test/execution")
        data object Success : TestFlow("test/success")
        data object Result : TestFlow("test/result/{resultId}")
    }
    
    // Profile Sub-screens
    sealed class ProfileFlow(route: String) : Screen(route) {
        data object Main : ProfileFlow("profile/main")
        data object Credits : ProfileFlow("profile/credits")
        data object Help : ProfileFlow("profile/help")
        data object Privacy : ProfileFlow("profile/privacy")
    }
    
    // History Sub-screens
    sealed class HistoryFlow(route: String) : Screen(route) {
        data object Main : HistoryFlow("history/main")
        data object Detail : HistoryFlow("history/detail/{resultId}")
        data object Filter : HistoryFlow("history/filter")
        data object Export : HistoryFlow("history/export")
    }
}
