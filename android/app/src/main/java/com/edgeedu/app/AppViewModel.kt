package com.edgeedu.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgeedu.app.data.CorpusRepository
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.search.SearchFilters
import com.edgeedu.app.search.SearchHit
import com.edgeedu.app.tutor.MockLlmEngine
import com.edgeedu.app.tutor.Tutor
import com.edgeedu.app.tutor.TutorReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ChatItem {
    data class User(val text: String) : ChatItem()
    data class Bot(val reply: TutorReply) : ChatItem()
}

data class CorpusInfo(
    val fileCount: Int,
    val chunkCount: Int,
    val verifiedSolutionChunks: Int,
    val contentVersion: Int,
)

sealed class AppState {
    data object Loading : AppState()
    data class Ready(val info: CorpusInfo) : AppState()
    data class Failed(val reason: String) : AppState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _chat = MutableStateFlow<List<ChatItem>>(emptyList())
    val chat: StateFlow<List<ChatItem>> = _chat.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _searchHits = MutableStateFlow<List<SearchHit>>(emptyList())
    val searchHits: StateFlow<List<SearchHit>> = _searchHits.asStateFlow()

    private lateinit var repository: CorpusRepository
    private lateinit var index: Bm25Index
    private lateinit var tutor: Tutor

    init {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Integrity check happens inside: hash mismatch -> Failed.
                    repository = CorpusRepository.get(getApplication())
                    index = Bm25Index(repository.chunks)
                    tutor = Tutor(index, MockLlmEngine())
                }
                _state.value = AppState.Ready(
                    CorpusInfo(
                        fileCount = repository.fileCount,
                        chunkCount = repository.chunks.size,
                        verifiedSolutionChunks = repository.verifiedSolutionChunks,
                        contentVersion = repository.contentVersion,
                    )
                )
            } catch (e: Exception) {
                _state.value = AppState.Failed(e.message ?: "failed to load content")
            }
        }
    }

    fun repositoryOrNull(): CorpusRepository? =
        if (state.value is AppState.Ready) repository else null

    fun ask(question: String, language: String?) {
        if (_busy.value || question.isBlank()) return
        _chat.value += ChatItem.User(question)
        _busy.value = true
        viewModelScope.launch {
            try {
                val reply = withContext(Dispatchers.Default) {
                    tutor.ask(question, SearchFilters(language = language))
                }
                _chat.value += ChatItem.Bot(reply)
            } finally {
                _busy.value = false
            }
        }
    }

    fun search(query: String, language: String?, standard: Int?) {
        viewModelScope.launch {
            _searchHits.value = withContext(Dispatchers.Default) {
                index.search(query, SearchFilters(language = language, standard = standard), k = 15)
            }
        }
    }
}
