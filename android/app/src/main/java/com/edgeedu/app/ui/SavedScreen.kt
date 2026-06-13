package com.edgeedu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.data.BookmarkEntry

/**
 * Bookmarked concepts with per-concept notes (PRD §6.7). Bookmarks span all
 * subjects — they are the student's own data, not session content — and are
 * stored on device only.
 */
@Composable
fun SavedScreen(viewModel: AppViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val notes by viewModel.notes.collectAsState()

    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Nothing saved yet.\nBookmark concepts with ☆ while browsing.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(bookmarks, key = { it.key }) { entry ->
            SavedCard(entry, notes[entry.key].orEmpty(), viewModel)
        }
    }
}

@Composable
private fun SavedCard(entry: BookmarkEntry, savedNote: String, viewModel: AppViewModel) {
    var note by remember(entry.key, savedNote) { mutableStateOf(savedNote) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${entry.chunkId} — ${entry.heading}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "★",
                    Modifier.clickable { viewModel.removeBookmark(entry) }
                        .padding(horizontal = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                "Std ${entry.standard} · ${entry.subject} · ${entry.language}",
                style = MaterialTheme.typography.labelSmall,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add a note…") },
                minLines = 1,
            )
            if (note != savedNote) {
                TextButton(onClick = { viewModel.saveNote(entry.key, note) }) {
                    Text("Save note")
                }
            }
        }
    }
}
