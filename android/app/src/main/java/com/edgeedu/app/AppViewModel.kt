package com.edgeedu.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgeedu.app.data.BookmarkEntry
import com.edgeedu.app.data.CorpusRepository
import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.data.UserDataStore
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.search.SearchFilters
import com.edgeedu.app.search.SearchHit
import com.edgeedu.app.session.Subject
import com.edgeedu.app.session.SubjectSession
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
    val subjectChunkCounts: Map<Subject, Int>,
)

sealed class AppState {
    data object Loading : AppState()
    data class Ready(val info: CorpusInfo) : AppState()
    data class Failed(val reason: String) : AppState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    /** The focused subject session; null until the student picks a subject. */
    private val _session = MutableStateFlow<SubjectSession?>(null)
    val session: StateFlow<SubjectSession?> = _session.asStateFlow()

    private val _sessionLoading = MutableStateFlow(false)
    val sessionLoading: StateFlow<Boolean> = _sessionLoading.asStateFlow()

    private val _chat = MutableStateFlow<List<ChatItem>>(emptyList())
    val chat: StateFlow<List<ChatItem>> = _chat.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _searchHits = MutableStateFlow<List<SearchHit>>(emptyList())
    val searchHits: StateFlow<List<SearchHit>> = _searchHits.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntry>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntry>> = _bookmarks.asStateFlow()

    private val _notes = MutableStateFlow<Map<String, String>>(emptyMap())
    val notes: StateFlow<Map<String, String>> = _notes.asStateFlow()

    private lateinit var repository: CorpusRepository
    private lateinit var userData: UserDataStore

    init {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Integrity check happens inside: hash mismatch -> Failed.
                    repository = CorpusRepository.get(getApplication())
                    userData = UserDataStore(getApplication())
                    _bookmarks.value = userData.bookmarks()
                    _notes.value = userData.notes()
                }
                _state.value = AppState.Ready(
                    CorpusInfo(
                        fileCount = repository.fileCount,
                        chunkCount = repository.chunks.size,
                        verifiedSolutionChunks = repository.verifiedSolutionChunks,
                        contentVersion = repository.contentVersion,
                        subjectChunkCounts = repository.chunks
                            .groupingBy { Subject.of(it.subject) }
                            .eachCount()
                            .filterKeys { it != null }
                            .mapKeys { it.key!! },
                    )
                )
            } catch (e: Exception) {
                _state.value = AppState.Failed(e.message ?: "failed to load content")
            }
        }
    }

    fun repositoryOrNull(): CorpusRepository? =
        if (state.value is AppState.Ready) repository else null

    /**
     * Starts a focused session: indexes only [subject]'s chunks and wires the
     * tutor with math tooling only for Mathematics. Replaces any previous
     * session (its index becomes garbage) and clears session-scoped state.
     */
    fun startSession(subject: Subject) {
        if (_sessionLoading.value || _session.value?.subject == subject) return
        _sessionLoading.value = true
        _session.value = null
        _chat.value = emptyList()
        _searchHits.value = emptyList()
        viewModelScope.launch {
            try {
                _session.value = withContext(Dispatchers.Default) {
                    val chunks = repository.chunks.filter { Subject.of(it.subject) == subject }
                    val index = Bm25Index(chunks)
                    SubjectSession(
                        subject = subject,
                        chunks = chunks,
                        index = index,
                        tutor = Tutor(index, MockLlmEngine(), mathSession = subject.isMath),
                    )
                }
            } finally {
                _sessionLoading.value = false
            }
        }
    }

    fun endSession() {
        _session.value = null
        _chat.value = emptyList()
        _searchHits.value = emptyList()
    }

    fun ask(question: String, language: String?) {
        val session = _session.value ?: return
        if (_busy.value || question.isBlank()) return
        _chat.value += ChatItem.User(question)
        _busy.value = true
        viewModelScope.launch {
            try {
                val reply = withContext(Dispatchers.Default) {
                    session.tutor.ask(question, SearchFilters(language = language))
                }
                _chat.value += ChatItem.Bot(reply)
            } finally {
                _busy.value = false
            }
        }
    }

    fun search(query: String, language: String?, standard: Int?) {
        val session = _session.value ?: return
        viewModelScope.launch {
            _searchHits.value = withContext(Dispatchers.Default) {
                session.index.search(
                    query, SearchFilters(language = language, standard = standard), k = 15
                )
            }
        }
    }

    fun isBookmarked(chunk: IndexedChunk): Boolean =
        _bookmarks.value.any { it.key == UserDataStore.keyOf(chunk) }

    fun toggleBookmark(chunk: IndexedChunk) {
        val key = UserDataStore.keyOf(chunk)
        viewModelScope.launch(Dispatchers.IO) {
            if (_bookmarks.value.any { it.key == key }) {
                userData.removeBookmark(key)
            } else {
                userData.addBookmark(
                    BookmarkEntry(
                        key = key,
                        file = chunk.file,
                        chunkId = chunk.chunk.chunk_id,
                        heading = chunk.chunk.heading,
                        subject = chunk.subject,
                        standard = chunk.standard,
                        language = chunk.language,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }
            _bookmarks.value = userData.bookmarks()
        }
    }

    fun removeBookmark(entry: BookmarkEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            userData.removeBookmark(entry.key)
            _bookmarks.value = userData.bookmarks()
        }
    }

    fun saveNote(key: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userData.putNote(key, text)
            _notes.value = userData.notes()
        }
    }
}
