package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.TawthiqViewModel

@Composable
fun AdminAppRoot(
    viewModel: TawthiqViewModel
) {
    var adminAuthenticated by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!adminAuthenticated) {
                AdminLoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        adminAuthenticated = true
                    }
                )
            } else {
                BackHandler {
                    // Lock console on back press
                    adminAuthenticated = false
                }

                AdminDashboardScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        adminAuthenticated = false
                    }
                )
            }
        }
    }
}
