package com.edgeedu.app

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgeedu.app.content.AssetContentSource
import com.edgeedu.app.content.ContentProvisioner
import com.edgeedu.app.content.ContentSource
import com.edgeedu.app.content.HttpContentSource
import com.edgeedu.app.content.Profile
import com.edgeedu.app.content.ProfileStore
import com.edgeedu.app.data.BookmarkEntry
import com.edgeedu.app.data.CorpusRepository
import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.data.UserDataStore
import com.edgeedu.app.notes.CustomSubject
import com.edgeedu.app.notes.CustomSubjectStore
import com.edgeedu.app.notes.ImportedFile
import com.edgeedu.app.notes.NoteChunker
import com.edgeedu.app.notes.NoteImport
import com.edgeedu.app.notes.NoteStructurer
import com.edgeedu.app.notes.NotesStore
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.search.SearchFilters
import com.edgeedu.app.search.SearchHit
import com.edgeedu.app.session.Subject
import com.edgeedu.app.session.SubjectRef
import com.edgeedu.app.session.SubjectSession
import com.edgeedu.app.tutor.LlmEngine
import com.edgeedu.app.tutor.ModelConfig
import com.edgeedu.app.tutor.Tutor
import com.edgeedu.app.tutor.TutorReply
import java.io.File
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
    /** Reading the saved profile on cold start. */
    data object Loading : AppState()

    /** No profile yet (first run or after logout): show the login screen. */
    data object NeedsLogin : AppState()

    /**
     * Downloading + verifying the profile's class+medium content (§12.2).
     * [filesTotal] == 0 means the count isn't known yet (indeterminate).
     */
    data class Provisioning(
        val scopeLabel: String,
        val filesDone: Int = 0,
        val filesTotal: Int = 0,
    ) : AppState()

    data class Ready(val info: CorpusInfo, val profile: Profile) : AppState()

    /** [retryable] is true when a saved profile lets the user retry the download. */
    data class Failed(val reason: String, val retryable: Boolean = false) : AppState()
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

    /** Imported note files for the active subject (PRD §8). */
    private val _importedFiles = MutableStateFlow<List<ImportedFile>>(emptyList())
    val importedFiles: StateFlow<List<ImportedFile>> = _importedFiles.asStateFlow()

    /** User-created subjects for the logged-in class ("add your own subject"). */
    private val _customSubjects = MutableStateFlow<List<CustomSubject>>(emptyList())
    val customSubjects: StateFlow<List<CustomSubject>> = _customSubjects.asStateFlow()

    /** Transient one-line status for imports (success/error), shown then dismissed. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()
    fun clearNotice() { _notice.value = null }

    private lateinit var repository: CorpusRepository
    private lateinit var userData: UserDataStore

    private val app: Application get() = getApplication()
    private val contentDir: File get() = File(app.filesDir, "content")
    private val profileStore by lazy { ProfileStore(app) }
    private val notesStore by lazy { NotesStore(app) }
    private val customSubjectStore by lazy { CustomSubjectStore(app) }
    /** Config-driven model (Qwen2.5-3B default), or the mock when unavailable. */
    private val llmEngine: LlmEngine by lazy { ModelConfig.createEngine(app) }
    private val provisioner by lazy {
        ContentProvisioner(contentDir, File(app.filesDir, "content_version"))
    }
    /**
     * The download origin: the configured static host when [BuildConfig.CONTENT_BASE_URL]
     * is set, otherwise the bundled-asset origin so the app still works fully
     * offline (the verification step is identical for both — §14.1).
     */
    private val contentSource: ContentSource
        get() = BuildConfig.CONTENT_BASE_URL.takeIf { it.isNotBlank() }
            ?.let { HttpContentSource(it) }
            ?: AssetContentSource(app.assets)

    /** Last profile we tried to load, so a failed download can be retried. */
    private var lastProfile: Profile? = null

    init {
        userData = UserDataStore(app)
        viewModelScope.launch {
            // User data (bookmarks/notes) persists across logout, so it loads
            // regardless of login state (§12.4).
            val profile = withContext(Dispatchers.IO) {
                _bookmarks.value = userData.bookmarks()
                _notes.value = userData.notes()
                profileStore.current()
            }
            if (profile == null) {
                _state.value = AppState.NeedsLogin
            } else {
                // Cold start with a saved profile: content is already downloaded
                // (unless a prior crash interrupted it), so load without re-fetching.
                loadForProfile(profile, forceDownload = false)
            }
        }
    }

    /** Submits the local profile (§12.1) and triggers the one-time download. */
    fun login(name: String, standard: Int, medium: String) {
        val profile = Profile(name.trim(), standard, medium)
        if (profile.name.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { profileStore.save(profile) }
            // Logging in always (re-)downloads this class+medium (§12.5).
            loadForProfile(profile, forceDownload = true)
        }
    }

    /** Logout (§12.4): wipe profile + downloaded content; keep user data. */
    fun logout() {
        viewModelScope.launch {
            endSession()
            _customSubjects.value = emptyList()
            withContext(Dispatchers.IO) {
                profileStore.clear()
                provisioner.clear()
            }
            _state.value = AppState.NeedsLogin
        }
    }

    /** Retry a failed download for the saved profile (PRD §12.2 retry path). */
    fun retry() {
        val profile = lastProfile ?: return
        viewModelScope.launch { loadForProfile(profile, forceDownload = true) }
    }

    private suspend fun loadForProfile(profile: Profile, forceDownload: Boolean) {
        lastProfile = profile
        _state.value = AppState.Provisioning(profile.scope.label)
        try {
            repository = withContext(Dispatchers.IO) {
                if (forceDownload || !provisioner.isProvisioned()) {
                    provisioner.provision(contentSource, profile.scope) { done, total ->
                        _state.value = AppState.Provisioning(profile.scope.label, done, total)
                    }
                }
                CorpusRepository.load(contentDir, profile.scope)
            }
            _state.value = AppState.Ready(corpusInfo(), profile)
            refreshCustomSubjects()
        } catch (e: Exception) {
            _state.value = AppState.Failed(e.message ?: "failed to load content", retryable = true)
        }
    }

    private fun corpusInfo() = CorpusInfo(
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

    fun repositoryOrNull(): CorpusRepository? =
        if (state.value is AppState.Ready) repository else null

    /**
     * Starts a focused session: indexes only [subject]'s chunks and wires the
     * tutor with math tooling only for Mathematics. Replaces any previous
     * session (its index becomes garbage) and clears session-scoped state.
     */
    fun startSession(ref: SubjectRef) {
        if (_sessionLoading.value || _session.value?.subject == ref) return
        _sessionLoading.value = true
        _session.value = null
        _chat.value = emptyList()
        _searchHits.value = emptyList()
        viewModelScope.launch {
            try {
                _session.value = withContext(Dispatchers.Default) { buildSession(ref) }
                refreshImportedFiles(ref)
            } finally {
                _sessionLoading.value = false
            }
        }
    }

    /** Creates a user-defined subject and immediately opens its (empty) session. */
    fun createCustomSubject(name: String) {
        val profile = lastProfile ?: return
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { customSubjectStore.create(clean, profile.standard) }
            refreshCustomSubjects()
            startSession(SubjectRef.Custom(id, clean))
        }
    }

    fun deleteCustomSubject(subject: CustomSubject) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                customSubjectStore.delete(subject.id)
                File(File(app.filesDir, "custom"), "${subject.id}.json").delete()
            }
            if ((_session.value?.subject as? SubjectRef.Custom)?.id == subject.id) endSession()
            refreshCustomSubjects()
        }
    }

    private suspend fun refreshCustomSubjects() {
        val profile = lastProfile ?: return
        _customSubjects.value = withContext(Dispatchers.IO) { customSubjectStore.subjects(profile.standard) }
    }

    /**
     * Builds the focused session index. Built-in subjects index the textbook
     * PLUS the student's imported notes (PRD §8.1 dual corpus); a custom subject
     * indexes only its own structured notes. Notes carry
     * [com.edgeedu.app.data.ChunkSource.Notes] so answers can show their source.
     */
    private fun buildSession(ref: SubjectRef): SubjectSession {
        val profile = lastProfile
        val all = when (ref) {
            is SubjectRef.Builtin -> {
                val textbook = repository.chunks.filter { Subject.of(it.subject) == ref.subject }
                val notes = if (profile != null) {
                    notesStore.chunksFor(ref.subject.name, profile.standard, profile.medium)
                } else emptyList()
                textbook + notes
            }
            is SubjectRef.Custom ->
                if (profile != null) customSubjectStore.chunksFor(ref.id, profile.medium) else emptyList()
        }
        val index = Bm25Index(all)
        return SubjectSession(
            subject = ref,
            chunks = all,
            index = index,
            tutor = Tutor(index, llmEngine, mathSession = ref.isMath),
        )
    }

    private suspend fun refreshImportedFiles(ref: SubjectRef) {
        val profile = lastProfile ?: return
        _importedFiles.value = withContext(Dispatchers.IO) {
            when (ref) {
                is SubjectRef.Builtin -> notesStore.importedFiles(ref.subject.name, profile.standard)
                is SubjectRef.Custom -> emptyList()
            }
        }
    }

    /**
     * Imports a notes file (PRD §8): validate → extract text (OCR for photos,
     * PdfBox for PDFs) → then for a built-in subject store as imported notes,
     * or for a custom subject run the structuring AI (chunks + headings +
     * keywords) and (re)write its offline JSON. The new content is retrievable
     * immediately.
     */
    fun importNotes(uri: Uri) {
        val session = _session.value ?: return
        val profile = lastProfile ?: return
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val custom = withContext(Dispatchers.IO) {
                    val name = queryName(uri)
                    NoteImport.validateType(name)
                    val text = extractImportedText(uri, name)
                    when (val ref = session.subject) {
                        is SubjectRef.Custom -> {
                            val structured = NoteStructurer.structure(text)
                            if (structured.isEmpty()) throw com.edgeedu.app.notes.ImportException(
                                "No readable text found in the file."
                            )
                            customSubjectStore.appendChunks(ref.id, structured)
                            writeCustomJson(ref, profile)
                            structured.size
                        }
                        is SubjectRef.Builtin -> {
                            val chunks = NoteChunker.chunk(text)
                            if (chunks.isEmpty()) throw com.edgeedu.app.notes.ImportException(
                                "No readable text found in the file."
                            )
                            notesStore.addImport(name, ref.subject.name, profile.standard, chunks)
                            null
                        }
                    }
                }
                _session.value = withContext(Dispatchers.Default) { buildSession(session.subject) }
                refreshImportedFiles(session.subject)
                refreshCustomSubjects()
                _notice.value =
                    if (custom != null) "Structured into $custom chunks ✓" else "Notes imported ✓"
            } catch (e: Exception) {
                _notice.value = e.message ?: "Import failed."
            } finally {
                _busy.value = false
            }
        }
    }

    /** Extracts plain text from any supported import type (image/PDF/text/JSON). */
    private fun extractImportedText(uri: Uri, name: String): String = when {
        NoteImport.isImage(name) -> {
            querySize(uri)?.let {
                if (it > NoteImport.MAX_BYTES) throw com.edgeedu.app.notes.ImportException(
                    "File is too large (max ${NoteImport.MAX_BYTES / 1_000_000} MB)."
                )
            }
            com.edgeedu.app.notes.ImageTextExtractor.extract(app, uri)
        }
        NoteImport.isPdf(name) -> {
            val bytes = readBytes(uri)
            NoteImport.validate(name, bytes.size)
            com.edgeedu.app.notes.PdfTextExtractor.extract(app, bytes)
        }
        else -> NoteImport.extractText(name, readBytes(uri))
    }

    /** (Re)writes the custom subject's structured notes as an offline JSON file. */
    private fun writeCustomJson(ref: SubjectRef.Custom, profile: Profile) {
        val structured = customSubjectStore.structuredChunks(ref.id)
        val jsonText = NoteStructurer.buildJson(ref.label, profile.standard, profile.medium, structured)
        val dir = File(app.filesDir, "custom").apply { mkdirs() }
        File(dir, "${ref.id}.json").writeText(jsonText)
    }

    fun deleteImport(file: ImportedFile) {
        val session = _session.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { notesStore.deleteFile(file.id) }
            _session.value = withContext(Dispatchers.Default) { buildSession(session.subject) }
            refreshImportedFiles(session.subject)
        }
    }

    private fun queryName(uri: Uri): String =
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: uri.lastPathSegment ?: "notes.txt"

    private fun querySize(uri: Uri): Long? =
        app.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }

    private fun readBytes(uri: Uri): ByteArray =
        app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw com.edgeedu.app.notes.ImportException("Couldn't open the file.")

    fun endSession() {
        _session.value = null
        _chat.value = emptyList()
        _searchHits.value = emptyList()
        _importedFiles.value = emptyList()
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
