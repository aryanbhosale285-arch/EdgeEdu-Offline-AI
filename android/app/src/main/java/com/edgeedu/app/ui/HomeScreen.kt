package com.edgeedu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.CorpusInfo
import com.edgeedu.app.content.Profile
import com.edgeedu.app.session.Subject
import com.edgeedu.app.ui.theme.EdgeEduGradients
import com.edgeedu.app.ui.theme.style

/**
 * Subject picker (PRD §6.1) in the design-kit look: a gradient header with the
 * profile + real content stats, a featured subject card, then a grid of the
 * rest. Tapping a subject starts a focused session over only its chunks.
 */
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    info: CorpusInfo,
    profile: Profile,
    onSessionStarted: () -> Unit,
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header(info, profile)

        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Subjects", style = MaterialTheme.typography.titleLarge)

            val subjects = Subject.entries
            val featured = subjects.first()
            FeaturedSubject(featured, info.subjectChunkCounts[featured] ?: 0) {
                viewModel.startSession(featured); onSessionStarted()
            }

            subjects.drop(1).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { subject ->
                        Box(Modifier.weight(1f)) {
                            SubjectCard(subject, info.subjectChunkCounts[subject] ?: 0) {
                                viewModel.startSession(subject); onSessionStarted()
                            }
                        }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Trustworthy maths ✓", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "In maths sessions the model never does arithmetic — every " +
                            "computation is routed through a verified math engine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(info: CorpusInfo, profile: Profile) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(EdgeEduGradients.Header, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        profile.name.firstOrNull()?.uppercase() ?: "S",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        "Welcome back 👋",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        profile.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Pill("Offline")
                Box(Modifier.padding(start = 6.dp)) { Pill("Class ${profile.standard}") }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("${info.chunkCount}", "Topics")
                Stat("${info.verifiedSolutionChunks}", "Verified")
                Stat("${info.subjectChunkCounts.size}", "Subjects")
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FeaturedSubject(subject: Subject, chunks: Int, onClick: () -> Unit) {
    val style = subject.style()
    Box(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(style.gradient, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Column {
            Text(style.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(subject.label, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "$chunks topics · verified math engine",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, chunks: Int, onClick: () -> Unit) {
    val style = subject.style()
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                Modifier.size(36.dp).background(style.tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(style.emoji) }
            Text(subject.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            Text(
                "$chunks topics",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
