package com.edgeedu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeedu.app.AppViewModel
import com.edgeedu.app.data.IndexedChunk

@Composable
fun BrowseScreen(viewModel: AppViewModel) {
    val repository = viewModel.repositoryOrNull() ?: return
    var selectedFile by remember { mutableStateOf<String?>(null) }

    if (selectedFile == null) {
        val groups = repository.chunks
            .groupBy { Triple(it.standard, it.subject, it.language) }
            .toSortedMap(compareBy({ it.first }, { it.second }, { it.third }))

        LazyColumn(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(groups.entries.toList()) { (key, chunks) ->
                val (standard, subject, language) = key
                Card(
                    Modifier.fillMaxWidth().clickable { selectedFile = chunks.first().file },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Std $standard — $subject", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$language · ${chunks.size} chunks · " +
                                "${chunks.count { !it.chunk.solution_steps.isNullOrEmpty() }} verified solutions",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    } else {
        val chunks = repository.chunks.filter { it.file == selectedFile }
        LazyColumn(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf<IndexedChunk?>(null) + chunks) { item ->
                if (item == null) {
                    Text(
                        "← Back to subjects",
                        Modifier.clickable { selectedFile = null }.padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${item.chunk.chunk_id} — ${item.chunk.heading}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "difficulty ${item.chunk.difficulty} · importance ${item.chunk.importance}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(item.chunk.text, style = MaterialTheme.typography.bodySmall)
                            item.chunk.latex?.let {
                                KatexView(it, Modifier.fillMaxWidth().height(52.dp))
                            }
                            item.chunk.solution_steps?.forEach { step ->
                                Text(
                                    (if (step.verified) "✓ " else "• ") + step.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
