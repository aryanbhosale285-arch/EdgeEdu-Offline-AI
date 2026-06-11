package com.edgeedu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgeedu.app.ui.BrowseScreen
import com.edgeedu.app.ui.ChatScreen
import com.edgeedu.app.ui.HomeScreen
import com.edgeedu.app.ui.SearchScreen

private enum class Tab(val label: String) {
    Home("Home"), Chat("Chat"), Search("Search"), Browse("Browse")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                EdgeEduApp()
            }
        }
    }
}

@Composable
private fun EdgeEduApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        label = { Text(entry.label) },
                        icon = {
                            val icon = when (entry) {
                                Tab.Home -> Icons.Filled.Home
                                Tab.Chat -> Icons.Filled.MailOutline
                                Tab.Search -> Icons.Filled.Search
                                Tab.Browse -> Icons.AutoMirrored.Filled.List
                            }
                            Icon(icon, contentDescription = entry.label)
                        },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is AppState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AppState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Content integrity check failed: ${s.reason}")
                }
                is AppState.Ready -> when (tab) {
                    Tab.Home -> HomeScreen(s.info)
                    Tab.Chat -> ChatScreen(viewModel)
                    Tab.Search -> SearchScreen(viewModel)
                    Tab.Browse -> BrowseScreen(viewModel)
                }
            }
        }
    }
}
