package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.TawthiqViewModel

@Composable
fun AdminAppRoot(
    viewModel: TawthiqViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE) }
    var adminAuthenticated by remember {
        mutableStateOf(prefs.getBoolean("admin_authenticated_session", false))
    }

    LaunchedEffect(Unit) {
        viewModel.syncAdminDataFromCloud()
        viewModel.listenToPaymentMethods()
        viewModel.listenToSystemBroadcasts()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!adminAuthenticated) {
                AdminLoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        prefs.edit().putBoolean("admin_authenticated_session", true).apply()
                        adminAuthenticated = true
                    }
                )
            } else {
                BackHandler {
                    prefs.edit().putBoolean("admin_authenticated_session", false).apply()
                    adminAuthenticated = false
                }

                AdminDashboardScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        prefs.edit().putBoolean("admin_authenticated_session", false).apply()
                        adminAuthenticated = false
                    }
                )
            }
        }
    }
}
