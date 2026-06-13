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
import androidx.compose.material.icons.filled.Settings
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
import com.edgeedu.app.ui.LoginScreen
import com.edgeedu.app.ui.SavedScreen
import com.edgeedu.app.ui.SearchScreen
import com.edgeedu.app.ui.SettingsScreen

private enum class Tab(val label: String) {
    Home("Home"), Chat("Chat"), Search("Search"), Browse("Browse"), Saved("Saved"), Settings("Settings")
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

    when (val s = state) {
        is AppState.Loading -> Centered { CircularProgressIndicator() }

        is AppState.NeedsLogin -> LoginScreen(
            onSubmit = { name, standard, medium -> viewModel.login(name, standard, medium) }
        )

        is AppState.Provisioning -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    "Downloading & verifying\n${s.scopeLabel}…",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        is AppState.Failed -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Couldn't load content:\n${s.reason}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { viewModel.logout() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Back to login")
                }
            }
        }

        is AppState.Ready -> ReadyScaffold(viewModel, s)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyScaffold(viewModel: AppViewModel, ready: AppState.Ready) {
    val session by viewModel.session.collectAsState()
    val sessionLoading by viewModel.sessionLoading.collectAsState()
    // Tab state lives here, so logging out (which leaves Ready) resets to Home.
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
                                Tab.Settings -> Icons.Filled.Settings
                            }
                            Icon(icon, contentDescription = entry.label)
                        },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Home -> HomeScreen(viewModel, ready.info, onSessionStarted = { tab = Tab.Chat })
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
                Tab.Settings -> SettingsScreen(viewModel, ready.profile)
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
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
        sessionLoading -> Centered { CircularProgressIndicator() }
        else -> Centered {
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
