package com.edgeedu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeedu.app.content.ContentScope

/**
 * Login / Profile screen (PRD §12.1, §16): a *local* profile form — not an
 * account. Name + class + medium; submitting triggers the one-time content
 * download (§12.2). Shown on first run and again after Logout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onSubmit: (name: String, standard: Int, medium: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var standard by remember { mutableIntStateOf(10) }
    var medium by remember { mutableStateOf("English") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("EdgeEdu", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Create a local profile. No account, no password — this stays on " +
                "your device and just sets which class and medium to download.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Class", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContentScope.STANDARDS.forEach { std ->
                FilterChip(
                    selected = standard == std,
                    onClick = { standard = std },
                    label = { Text("Class $std") },
                )
            }
        }

        Text("Medium", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContentScope.MEDIUMS.forEach { med ->
                FilterChip(
                    selected = medium == med,
                    onClick = { medium = med },
                    label = { Text(med) },
                )
            }
        }

        Button(
            onClick = { onSubmit(name, standard, medium) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Download & start")
        }

        Text(
            "First login needs internet once to download Class $standard · $medium " +
                "content. After that, everything works fully offline until you log out.",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
