package com.example.uniride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.uniride.presentation.auth.AuthGraphRoute
import com.example.uniride.presentation.common.LoadingState
import com.example.uniride.presentation.navigation.AppSessionUiState
import com.example.uniride.presentation.navigation.AppSessionViewModel
import com.example.uniride.presentation.navigation.MainGraphRoute
import com.example.uniride.presentation.navigation.UniRideApp
import com.example.uniride.presentation.theme.UniRideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniRideTheme {
                val sessionViewModel: AppSessionViewModel = hiltViewModel()
                val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()

                when (val state = sessionState) {
                    AppSessionUiState.Loading -> LoadingState()
                    is AppSessionUiState.Ready -> {
                        val navController = rememberNavController()
                        val initialStartDestination = remember {
                            if (state.isLoggedIn) MainGraphRoute else AuthGraphRoute
                        }
                        var wasLoggedIn by rememberSaveable { mutableStateOf(state.isLoggedIn) }

                        LaunchedEffect(state.isLoggedIn) {
                            if (wasLoggedIn && !state.isLoggedIn) {
                                navController.navigate(AuthGraphRoute) {
                                    popUpTo<MainGraphRoute> { inclusive = true }
                                }
                            }
                            wasLoggedIn = state.isLoggedIn
                        }

                        UniRideApp(
                            profile = state.profile,
                            navController = navController,
                            startDestination = initialStartDestination,
                        )
                    }
                }
            }
        }
    }
}
