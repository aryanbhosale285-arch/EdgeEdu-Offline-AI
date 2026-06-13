package com.edgeedu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.content.Profile

/**
 * Settings (PRD §16): profile summary, offline status, and Logout — which
 * deletes downloaded content but keeps bookmarks and notes (§12.4).
 */
@Composable
fun SettingsScreen(viewModel: AppViewModel, profile: Profile) {
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Profile", style = MaterialTheme.typography.titleSmall)
                Text(profile.name, style = MaterialTheme.typography.bodyLarge)
                Text(profile.scope.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Local profile only — no account, nothing leaves this device.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Offline", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Content is downloaded and verified on your device. No internet " +
                        "is used until you log out and back in.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text("Log out")
        }
        Text(
            "Logging out deletes the downloaded content to free storage. Your " +
                "bookmarks and notes are kept and restored when you log back in " +
                "with the same name and class.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Unspecified,
        )
    }
}
