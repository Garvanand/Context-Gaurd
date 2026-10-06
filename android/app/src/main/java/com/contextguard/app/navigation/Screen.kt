package com.contextguard.app.navigation

sealed class Screen(val route: String, val title: String) {
    object Welcome : Screen("welcome", "Welcome")
    object Home : Screen("home", "ContextGuard")
    object Analyze : Screen("analyze", "Pre-Action Analysis")
    object Result : Screen("result", "Safety Intervention")
    object Privacy : Screen("privacy", "Privacy & Redaction")
    object Settings : Screen("settings", "System Settings")
    object Demo : Screen("demo", "Central Demonstration")
    object Supervisor : Screen("supervisor", "Supervisor Viva Mode")

    companion object {
        val bottomNavItems = listOf(
            Home,
            Analyze,
            Demo,
            Supervisor,
            Privacy,
            Settings
        )
    }
}
