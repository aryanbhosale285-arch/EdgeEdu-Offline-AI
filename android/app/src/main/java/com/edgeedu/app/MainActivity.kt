package com.edgeedu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.edgeedu.app.ui.theme.EdgeEduGradients
import com.edgeedu.app.ui.theme.EdgeEduTheme
import com.edgeedu.app.ui.theme.ThemeMode
import com.edgeedu.app.ui.theme.ThemePrefs

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Chat("Ask AI", Icons.Filled.MailOutline),
    Search("Search", Icons.Filled.Search),
    Browse("Browse", Icons.AutoMirrored.Filled.List),
    Saved("Saved", Icons.Filled.Star),
    Settings("Profile", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = this
            var themeMode by remember { mutableStateOf(ThemePrefs.load(context)) }
            val darkTheme = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }
            EdgeEduTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EdgeEduApp(
                        themeMode = themeMode,
                        onThemeChange = {
                            themeMode = it
                            ThemePrefs.save(context, it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EdgeEduApp(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    viewModel: AppViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is AppState.Loading -> Centered { CircularProgressIndicator() }

        is AppState.NeedsLogin -> LoginScreen(
            onSubmit = { name, standard, medium -> viewModel.login(name, standard, medium) }
        )

        is AppState.Provisioning -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (s.filesTotal > 0) {
                    LinearProgressIndicator(
                        progress = { s.filesDone.toFloat() / s.filesTotal },
                        modifier = Modifier.fillMaxWidth(0.7f),
                    )
                } else {
                    CircularProgressIndicator()
                }
                Text(
                    "Downloading & verifying\n${s.scopeLabel}…" +
                        if (s.filesTotal > 0) "\n${s.filesDone} / ${s.filesTotal} files" else "",
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
                if (s.retryable) {
                    Button(onClick = { viewModel.retry() }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry download")
                    }
                }
                TextButton(onClick = { viewModel.logout() }, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Back to login")
                }
            }
        }

        is AppState.Ready -> ReadyScaffold(viewModel, s, themeMode, onThemeChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyScaffold(
    viewModel: AppViewModel,
    ready: AppState.Ready,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val session by viewModel.session.collectAsState()
    val sessionLoading by viewModel.sessionLoading.collectAsState()
    // Tab state lives here, so logging out (which leaves Ready) resets to Home.
    var tab by remember { mutableStateOf(Tab.Home) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Home and Profile carry their own headers; only show a top bar
            // for the session/utility screens.
            if (tab != Tab.Home && tab != Tab.Settings) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    title = {
                        Text(
                            when {
                                session != null -> "${session!!.subject.label} session"
                                sessionLoading -> "Starting session…"
                                else -> "EdgeEdu"
                            },
                            style = MaterialTheme.typography.titleLarge,
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
            }
        },
        bottomBar = { EdgeEduBottomBar(tab) { tab = it } },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Home -> HomeScreen(viewModel, ready.info, ready.profile, onSessionStarted = { tab = Tab.Chat })
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
                Tab.Settings -> SettingsScreen(viewModel, ready.profile, themeMode, onThemeChange)
            }
        }
    }
}

/** Design-kit bottom nav: a gradient pill behind the active icon. */
@Composable
private fun EdgeEduBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { entry ->
                val active = entry == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onSelect(entry) }
                        .padding(horizontal = 2.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 40.dp, height = 30.dp)
                            .background(
                                if (active) EdgeEduGradients.Header else Brush.linearGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                ),
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            entry.icon,
                            contentDescription = entry.label,
                            tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
