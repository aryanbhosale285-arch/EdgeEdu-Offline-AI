package com.edgeedu.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.ChatItem
import com.edgeedu.app.data.ChunkSource
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(viewModel: AppViewModel) {
    val chat by viewModel.chat.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        ImportBar(viewModel)

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chat) { item ->
                when (item) {
                    is ChatItem.User -> Box(Modifier.fillMaxWidth()) {
                        Text(
                            item.text,
                            Modifier
                                .align(Alignment.CenterEnd)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(10.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    is ChatItem.Bot -> Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(item.reply.text, style = MaterialTheme.typography.bodyMedium)
                        item.reply.latex?.let {
                            KatexView(it, Modifier.fillMaxWidth().height(56.dp))
                        }
                        item.reply.steps.forEach { step ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (step.verified) "✓ " else "• ",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Column {
                                    Text(step.text, style = MaterialTheme.typography.bodySmall)
                                    KatexView(step.latex, Modifier.fillMaxWidth().height(44.dp))
                                }
                            }
                        }
                        if (item.reply.sources.isNotEmpty()) {
                            Text(
                                "Sources: " + item.reply.sources.joinToString(" · ") { s ->
                                    when (s.source) {
                                        ChunkSource.Notes -> "📝 your notes: ${s.chunk.heading}"
                                        ChunkSource.Textbook ->
                                            "📄 [${s.chunk.chunk_id}] ${s.chunk.heading}"
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. solve 2x + 3 = 7") },
            )
            Button(
                onClick = {
                    viewModel.ask(input.trim(), language = null)
                    input = ""
                },
                enabled = !busy && input.isNotBlank(),
            ) {
                Text(if (busy) "…" else "Ask")
            }
        }
    }
}

/**
 * Bring-your-own-notes (PRD §8): import a .txt/.md file into the active subject,
 * list imported files, and surface a short status line. The picker is restricted
 * to text MIME types — the only formats this build parses.
 */
@Composable
private fun ImportBar(viewModel: AppViewModel) {
    val imported by viewModel.importedFiles.collectAsState()
    val notice by viewModel.notice.collectAsState()
    // OpenDocument lets us offer several types (text, JSON, PDF) at once.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importNotes(uri)
    }
    val importTypes = arrayOf("text/*", "application/json", "application/pdf", "image/*")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { picker.launch(importTypes) }) { Text("Import notes") }
            if (imported.isNotEmpty()) {
                Text(
                    "${imported.size} file(s) in this subject",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        imported.forEach { file ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "📝 ${file.name} · ${file.chunkCount} chunks",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.deleteImport(file) }) { Text("Remove") }
            }
        }
        notice?.let { msg ->
            Text(msg, style = MaterialTheme.typography.labelMedium)
            LaunchedEffect(msg) {
                delay(2500)
                viewModel.clearNotice()
            }
        }
    }
}
