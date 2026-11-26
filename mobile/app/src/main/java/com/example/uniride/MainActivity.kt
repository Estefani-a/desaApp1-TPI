package com.example.uniride

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.uniride.presentation.navigation.AppAction
import com.example.uniride.presentation.navigation.AppViewModel
import com.example.uniride.presentation.navigation.UniRideAppRoute
import com.example.uniride.presentation.navigation.pushDestination
import com.example.uniride.presentation.theme.UniRideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Same instance that UniRideAppRoute gets with hiltViewModel(): both use the activity as owner
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on the first creation: after a rotation the intent is the same and was already handled
        if (savedInstanceState == null) openPush(intent)
        setContent {
            UniRideTheme {
                UniRideAppRoute()
            }
        }
    }

    /** A push tapped with the app already open (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openPush(intent)
    }

    private fun openPush(intent: Intent) {
        intent.pushDestination()?.let { appViewModel.onAction(AppAction.OpenFromPush(it)) }
    }
}
