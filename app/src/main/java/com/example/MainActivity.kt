package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.CrashShield
import com.example.utils.SettingsManager

class MainActivity : ComponentActivity() {

    // Eagerly declare lazy helper references
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { AppRepository(database.appDao()) }
    private val settings by lazy { SettingsManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register CrashShield handler immediately before any standard execution
        CrashShield.init(applicationContext)
        
        enableEdgeToEdge()

        setContent {
            var activeCrashLog by remember { mutableStateOf(CrashShield.getLatestCrash(this@MainActivity)) }

            if (activeCrashLog != null) {
                MyApplicationTheme(darkTheme = false) {
                    CrashShieldScreen(crashLog = activeCrashLog!!) {
                        CrashShield.clearCrash(this@MainActivity)
                        activeCrashLog = null
                    }
                }
            } else {
                // Initialize the MainViewModel using Jetpack Compose native ViewModel locator
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository, settings)
                )
                val isDarkTheme by viewModel.isDarkMode.collectAsState()
                
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    com.example.ui.DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CrashShieldScreen(crashLog: String, onReset: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Crash Logged",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Application Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "The application encountered an unexpected exception and paused. Details are listed below for debugging:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = crashLog,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Crash Log", crashLog)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied log to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Log")
                }

                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset & Restart")
                }
            }
        }
    }
}
