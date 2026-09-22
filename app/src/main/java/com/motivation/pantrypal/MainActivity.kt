package com.motivation.pantrypal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.motivation.pantrypal.di.AppContainer
import com.motivation.pantrypal.di.ViewModelFactory
import com.motivation.pantrypal.ui.auth.AuthViewModel
import com.motivation.pantrypal.ui.auth.LoginScreen
import com.motivation.pantrypal.ui.auth.RegisterScreen
import com.motivation.pantrypal.ui.dashboard.DashboardScreen
import com.motivation.pantrypal.ui.dashboard.DashboardViewModel
import com.motivation.pantrypal.ui.favourites.FavouritesScreen
import com.motivation.pantrypal.ui.favourites.FavouritesViewModel
import com.motivation.pantrypal.ui.mealplan.MealPlanScreen
import com.motivation.pantrypal.ui.mealplan.MealPlanViewModel
import com.motivation.pantrypal.ui.navigation.BottomTab
import com.motivation.pantrypal.ui.navigation.Routes
import com.motivation.pantrypal.ui.onboarding.OnboardingScreen
import com.motivation.pantrypal.ui.pantry.PantryScreen
import com.motivation.pantrypal.ui.pantry.PantryViewModel
import com.motivation.pantrypal.ui.profile.ProfileScreen
import com.motivation.pantrypal.ui.profile.ProfileViewModel
import com.motivation.pantrypal.ui.recipes.RecipeDetailScreen
import com.motivation.pantrypal.ui.recipes.RecipeDetailViewModel
import com.motivation.pantrypal.ui.recipes.RecipeSearchScreen
import com.motivation.pantrypal.ui.recipes.RecipeSearchViewModel
import com.motivation.pantrypal.ui.shoppinglist.ShoppingListScreen
import com.motivation.pantrypal.ui.shoppinglist.ShoppingListViewModel
import com.motivation.pantrypal.ui.theme.PantryPalTheme

/**
 * Single-activity host for the whole prototype. Wires the [AppContainer]
 * (Room + Retrofit + repositories) into every screen's ViewModel via the
 * lightweight [ViewModelFactory], and hosts the Compose Navigation graph that
 * implements the onboarding -> auth -> 5-tab dashboard wireflow from the
 * Planning & Design document (Screens 1-11).
 *
 * Every lifecycle callback is logged so Logcat gives a clear, demonstrable
 * trace of the activity's state transitions for the video-demo requirement.
 */
class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as PantryPalApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: launching PantryPal UI")
        setContent {
            val isDark by container.sessionManager.darkThemeEnabled.collectAsState(initial = false)
            PantryPalTheme(darkTheme = isDark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PantryPalNavHost(container)
                }
            }
        }
    }

    override fun onStart() { super.onStart(); Log.i(TAG, "onStart") }
    override fun onResume() { super.onResume(); Log.i(TAG, "onResume") }
    override fun onPause() { super.onPause(); Log.i(TAG, "onPause") }
    override fun onStop() { super.onStop(); Log.i(TAG, "onStop") }
    override fun onDestroy() { super.onDestroy(); Log.i(TAG, "onDestroy") }

    companion object { private const val TAG = "MainActivity" }
}

@Composable
fun PantryPalNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val isLoggedIn by container.sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val startDestination = if (isLoggedIn) Routes.DASHBOARD_ROOT else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = { navController.navigate(Routes.REGISTER) },
                onLogIn = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.REGISTER) {
            val vm: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(container.authRepository) })
            RegisterScreen(
                viewModel = vm,
                onRegistered = { navController.navigateToDashboardClearingBackStack() },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(container.authRepository) })
            LoginScreen(
                viewModel = vm,
                onLoggedIn = { navController.navigateToDashboardClearingBackStack() },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        // The bottom-nav shell (Search, Home, Plan, List, Profile) plus the
        // secondary screens reachable from within each tab (pantry, recipe
        // detail). Nesting it as one composable route keeps the bottom bar
        // visible while its own inner NavHost swaps tab content.
        composable(Routes.DASHBOARD_ROOT) {
            MainScaffold(container, onLoggedOut = {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            })
        }
    }
}

private fun NavHostController.navigateToDashboardClearingBackStack() {
    navigate(Routes.DASHBOARD_ROOT) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
}

/** Hosts the persistent bottom navigation bar and its own inner nav graph, per the wireflow diagram. */
@Composable
private fun MainScaffold(container: AppContainer, onLoggedOut: () -> Unit) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = { PantryPalBottomBar(innerNavController) }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                val vm: DashboardViewModel = viewModel(
                    factory = ViewModelFactory { DashboardViewModel(container.pantryRepository, container.sessionManager) }
                )
                DashboardScreen(
                    viewModel = vm,
                    onOpenPantry = { innerNavController.navigate(Routes.PANTRY) },
                    onOpenMealPlan = { innerNavController.navigate(Routes.MEAL_PLAN) },
                    onOpenShoppingList = { innerNavController.navigate(Routes.SHOPPING_LIST) },
                    onOpenFavourites = { innerNavController.navigate(Routes.FAVOURITES) }
                )
            }
            composable(Routes.PANTRY) {
                val vm: PantryViewModel = viewModel(factory = ViewModelFactory { PantryViewModel(container.pantryRepository) })
                PantryScreen(
                    viewModel = vm,
                    onFindRecipes = { innerNavController.navigate(Routes.RECIPE_RESULTS) },
                    onBack = { innerNavController.popBackStack() }
                )
            }
            composable(Routes.RECIPE_RESULTS) {
                val vm: RecipeSearchViewModel = viewModel(factory = ViewModelFactory { RecipeSearchViewModel(container.recipeRepository) })
                RecipeSearchScreen(
                    viewModel = vm,
                    onRecipeSelected = { id -> innerNavController.navigate(Routes.recipeDetail(id)) }
                )
            }
            composable(Routes.RECIPE_DETAIL) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId")?.toIntOrNull() ?: 0
                val vm: RecipeDetailViewModel = viewModel(
                    factory = ViewModelFactory {
                        RecipeDetailViewModel(recipeId, container.recipeRepository, container.mealPlanRepository, container.favouritesRepository)
                    }
                )
                RecipeDetailScreen(viewModel = vm, onBack = { innerNavController.popBackStack() })
            }
            composable(Routes.MEAL_PLAN) {
                val vm: MealPlanViewModel = viewModel(
                    factory = ViewModelFactory { MealPlanViewModel(container.mealPlanRepository, container.shoppingListRepository) }
                )
                MealPlanScreen(viewModel = vm)
            }
            composable(Routes.SHOPPING_LIST) {
                val vm: ShoppingListViewModel = viewModel(factory = ViewModelFactory { ShoppingListViewModel(container.shoppingListRepository) })
                ShoppingListScreen(viewModel = vm)
            }
            composable(Routes.FAVOURITES) {
                val vm: FavouritesViewModel = viewModel(factory = ViewModelFactory { FavouritesViewModel(container.favouritesRepository) })
                FavouritesScreen(
                    viewModel = vm,
                    onRecipeSelected = { id -> innerNavController.navigate(Routes.recipeDetail(id)) }
                )
            }
            composable(Routes.PROFILE) {
                val vm: ProfileViewModel = viewModel(
                    factory = ViewModelFactory { ProfileViewModel(container.authRepository, container.sessionManager) }
                )
                ProfileScreen(viewModel = vm, onLoggedOut = onLoggedOut)
            }
        }
    }
}

@Composable
private fun PantryPalBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
