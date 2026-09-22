package com.motivation.pantrypal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"

    /** Outer graph destination hosting the bottom-nav shell (see [DASHBOARD] for the Home tab itself). */
    const val DASHBOARD_ROOT = "dashboard_root"

    const val DASHBOARD = "dashboard"
    const val PANTRY = "pantry"
    const val RECIPE_RESULTS = "recipe_results"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val MEAL_PLAN = "meal_plan"
    const val SHOPPING_LIST = "shopping_list"
    const val FAVOURITES = "favourites"
    const val PROFILE = "profile"

    fun recipeDetail(recipeId: Int) = "recipe_detail/$recipeId"
}

/** Tabs shown on the bottom navigation bar of the main dashboard, per Screen 4's wireflow. */
enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    Search(Routes.RECIPE_RESULTS, "Search", Icons.Filled.Search),
    Home(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    Plan(Routes.MEAL_PLAN, "Plan", Icons.Filled.CalendarMonth),
    List(Routes.SHOPPING_LIST, "List", Icons.Filled.ShoppingCart),
    Profile(Routes.PROFILE, "Profile", Icons.Filled.Person)
}
