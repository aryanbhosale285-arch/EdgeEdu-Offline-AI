package com.edgeedu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.data.UserDataStore

private val LANGUAGES = listOf("English", "Hindi", "Marathi")

@Composable
fun SearchScreen(viewModel: AppViewModel) {
    val hits by viewModel.searchHits.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    var query by remember { mutableStateOf("") }
    var language by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("quadratic / द्विघात समीकरण") },
            )
            Button(
                onClick = { viewModel.search(query.trim(), language, standard = null) },
                enabled = query.isNotBlank(),
            ) { Text("Go") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LANGUAGES.forEach { lang ->
                FilterChip(
                    selected = language == lang,
                    onClick = { language = if (language == lang) null else lang },
                    label = { Text(lang) },
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hits) { hit ->
                val bookmarked = bookmarks.any { it.key == UserDataStore.keyOf(hit.chunk) }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                hit.chunk.chunk.heading,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                if (bookmarked) "★" else "☆",
                                Modifier.clickable { viewModel.toggleBookmark(hit.chunk) }
                                    .padding(horizontal = 6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            "Std ${hit.chunk.standard} · ${hit.chunk.subject} · ${hit.chunk.language}" +
                                " · ${hit.chunk.chunk.chunk_id}" +
                                if (!hit.chunk.chunk.solution_steps.isNullOrEmpty()) " · ✓ verified solution" else "",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            hit.chunk.chunk.text.take(280) +
                                if (hit.chunk.chunk.text.length > 280) " …" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
