package com.edgeedu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.CorpusInfo
import com.edgeedu.app.session.Subject

/**
 * Subject picker (PRD §6.1): tapping a subject starts a focused session
 * loading only that subject's chunks and index.
 */
@Composable
fun HomeScreen(viewModel: AppViewModel, info: CorpusInfo, onSessionStarted: () -> Unit) {
    val session by viewModel.session.collectAsState()

    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("EdgeEdu", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Offline AI tutor for Maharashtra Board, Class 9 & 10 — " +
                "English, Hindi and Marathi. No internet needed, ever.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Pick a subject to start a session", style = MaterialTheme.typography.titleSmall)
        Subject.entries.forEach { subject ->
            val active = session?.subject == subject
            Card(
                Modifier.fillMaxWidth().clickable {
                    viewModel.startSession(subject)
                    onSessionStarted()
                },
                colors = if (active) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    CardDefaults.cardColors()
                },
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        subject.label + if (active) "  ·  active session" else "",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${info.subjectChunkCounts[subject] ?: 0} chunks" +
                            if (subject.isMath) " · verified math engine" else "",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Content verified ✓", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Signed manifest v${info.contentVersion}: all ${info.fileCount} curriculum " +
                        "files passed SHA-256 verification at startup.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("${info.fileCount}", "files")
                Stat("${info.chunkCount}", "chunks")
                Stat("${info.verifiedSolutionChunks}", "verified\nsolutions")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Trustworthy maths", style = MaterialTheme.typography.titleSmall)
                Text(
                    "The language model never does arithmetic. In maths sessions, " +
                        "computations are routed to a math engine through structured " +
                        "<calc> calls and verified before you see them; textbook " +
                        "solutions were pre-verified with a symbolic engine at build time.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
