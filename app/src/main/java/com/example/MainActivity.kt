package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.DashboardScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize offline persistent helpers and local caching modules
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database.appDao())
        val settings = SettingsManager(this)

        // 2. Initialize ViewModel with custom factory for safe construction
        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(repository, settings)
        }

        setContent {
            val isDarkTheme by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                com.example.ui.DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
