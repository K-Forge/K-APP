package co.edu.konradlorenz.kapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.konradlorenz.kapp.ui.home.HomeScreen
import co.edu.konradlorenz.kapp.ui.invitation.InvitationScreen
import co.edu.konradlorenz.kapp.ui.login.LoginScreen

// Three routes and no arguments to carry, so plain strings are enough.
private const val LOGIN = "login"
private const val HOME = "home"
private const val INVITATION = "invitation"

@Composable
fun KAppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LOGIN) {
        composable(LOGIN) {
            LoginScreen(
                onSignIn = {
                    // Nobody goes back to the login with the back button once they are in.
                    navController.navigate(HOME) {
                        popUpTo(LOGIN) { inclusive = true }
                    }
                },
                onUseInvitationCode = { navController.navigate(INVITATION) },
            )
        }
        composable(HOME) { HomeScreen() }
        composable(INVITATION) {
            InvitationScreen(onBack = { navController.popBackStack() })
        }
    }
}
