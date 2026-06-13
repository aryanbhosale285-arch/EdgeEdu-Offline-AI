package com.edgeedu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgeedu.app.ui.BrowseScreen
import com.edgeedu.app.ui.ChatScreen
import com.edgeedu.app.ui.HomeScreen
import com.edgeedu.app.ui.SavedScreen
import com.edgeedu.app.ui.SearchScreen

private enum class Tab(val label: String) {
    Home("Home"), Chat("Chat"), Search("Search"), Browse("Browse"), Saved("Saved")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EdgeEduApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val session by viewModel.session.collectAsState()
    val sessionLoading by viewModel.sessionLoading.collectAsState()
    var tab by remember { mutableStateOf(Tab.Home) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            session != null -> "${session!!.subject.label} session"
                            sessionLoading -> "Starting session…"
                            else -> "EdgeEdu"
                        }
                    )
                },
                actions = {
                    if (session != null) {
                        TextButton(onClick = {
                            viewModel.endSession()
                            tab = Tab.Home
                        }) { Text("End session") }
                    }
                },
            )
        },
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
                                Tab.Saved -> Icons.Filled.Star
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
                    Tab.Home -> HomeScreen(viewModel, s.info, onSessionStarted = { tab = Tab.Chat })
                    Tab.Chat -> RequireSession(viewModel, sessionLoading, onGoHome = { tab = Tab.Home }) {
                        ChatScreen(viewModel)
                    }
                    Tab.Search -> RequireSession(viewModel, sessionLoading, onGoHome = { tab = Tab.Home }) {
                        SearchScreen(viewModel)
                    }
                    Tab.Browse -> RequireSession(viewModel, sessionLoading, onGoHome = { tab = Tab.Home }) {
                        BrowseScreen(viewModel)
                    }
                    Tab.Saved -> SavedScreen(viewModel)
                }
            }
        }
    }
}

/**
 * Chat, search and browse are session-scoped (one subject at a time, PRD
 * §6.1): without an active session they point the student back to the
 * subject picker.
 */
@Composable
private fun RequireSession(
    viewModel: AppViewModel,
    sessionLoading: Boolean,
    onGoHome: () -> Unit,
    content: @Composable () -> Unit,
) {
    val session by viewModel.session.collectAsState()
    when {
        session != null -> content()
        sessionLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No active session.\nPick a subject to get started.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onGoHome, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Choose subject")
                }
            }
        }
    }
}
