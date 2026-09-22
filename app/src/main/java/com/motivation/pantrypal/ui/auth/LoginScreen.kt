package com.motivation.pantrypal.ui.auth

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.motivation.pantrypal.R

private const val TAG = "LoginScreen"

/**
 * Screen 3 from the design doc. Supports email/password login plus
 * Google Single Sign-On via the classic GoogleSignInClient flow: the
 * returned Google ID token is exchanged with our custom auth API's
 * POST /api/v1/auth/sso/google endpoint, which verifies it against Google
 * and returns a ready-to-use Firebase session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val googleSignInClient = remember {
        // requestIdToken (not requestServerAuthCode) gives us a Google ID
        // token directly on-device, which our backend hands straight to
        // Firebase's Identity Toolkit signInWithIdp REST call.
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            runCatching { task.getResult(com.google.android.gms.common.api.ApiException::class.java) }
                .onSuccess { account ->
                    val idToken = account.idToken
                    if (idToken != null) {
                        viewModel.ssoLoginWithGoogle(idToken)
                    } else {
                        Log.e(TAG, "Google sign-in succeeded but no ID token returned")
                    }
                }
                .onFailure { Log.e(TAG, "Google sign-in failed: ${it.message}", it) }
        }
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onLoggedIn()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Log In") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            OutlinedTextField(email, { email = it }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                password, { password = it }, label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
            )

            uiState.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("LOG IN")
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google (SSO)")
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Sign Up") }
        }
    }
}
