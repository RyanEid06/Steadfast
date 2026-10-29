package com.steadfast.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Journey : Screen("journey")
    data object Bible : Screen("bible")
    data object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: Int) = "book_detail/$bookId"
    }
    data object Struggling : Screen("struggling")
    data object Breathing : Screen("breathing")
    data object Milestones : Screen("milestones")
    data object Motivation : Screen("motivation")
    data object Settings : Screen("settings")
    data object Tutorial : Screen("tutorial")
}

enum class BottomBarDestination(
    val route: String,
    val title: String,
    val iconName: String
) {
    DASHBOARD(Screen.Dashboard.route, "Dashboard", "home"),
    JOURNEY(Screen.Journey.route, "Journey", "trending_up"),
    BIBLE(Screen.Bible.route, "Bible", "auto_stories")
}
