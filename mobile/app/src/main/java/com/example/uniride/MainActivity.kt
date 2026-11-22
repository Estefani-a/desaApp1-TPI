package com.example.uniride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uniride.presentation.navigation.UniRideNavHost
import com.example.uniride.ui.theme.UniRideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniRideTheme {
                UniRideNavHost()
            }
        }
    }
}
