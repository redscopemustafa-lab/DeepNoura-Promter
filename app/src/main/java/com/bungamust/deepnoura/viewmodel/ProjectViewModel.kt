package com.bungamust.deepnoura.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bungamust.deepnoura.data.ProjectRepository
import com.bungamust.deepnoura.model.Project
import com.bungamust.deepnoura.model.Segment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class PlayerState(
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val mode: PlayerMode = PlayerMode.Prompter,
    val speedMultiplier: Float = 1f,
    val fontScale: Float = 1f,
    val lineSpacing: Float = 1.2f,
    val mirror: Boolean = false,
    val isDarkMode: Boolean = false,
    val lineIndex: Int = 0
)

enum class PlayerMode { Prompter, Karaoke }

data class UiState(
    val project: Project = Project(),
    val playerState: PlayerState = PlayerState(),
    val isDarkMode: Boolean = false,
    val snackbarMessage: String? = null
)

class ProjectViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext, Json { prettyPrint = true })

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { repository.load() }
                .onSuccess { project -> _uiState.update { it.copy(project = project) } }
                .onFailure { _uiState.update { it.copy(snackbarMessage = "Failed to load project") } }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled, playerState = it.playerState.copy(isDarkMode = enabled)) }
    }

    fun setMode(mode: PlayerMode) {
        _uiState.update { it.copy(playerState = it.playerState.copy(mode = mode)) }
    }

    fun playPause() {
        _uiState.update { it.copy(playerState = it.playerState.copy(isPlaying = !it.playerState.isPlaying)) }
    }

    fun restartSegment() {
        _uiState.update { it.copy(playerState = it.playerState.copy(lineIndex = 0, isPlaying = true)) }
    }

    fun nextSegment() {
        val total = _uiState.value.project.segments.size
        if (total == 0) return
        _uiState.update {
            val next = min(total - 1, it.playerState.currentIndex + 1)
            it.copy(playerState = it.playerState.copy(currentIndex = next, lineIndex = 0, isPlaying = false))
        }
    }

    fun previousSegment() {
        val total = _uiState.value.project.segments.size
        if (total == 0) return
        _uiState.update {
            val prev = max(0, it.playerState.currentIndex - 1)
            it.copy(playerState = it.playerState.copy(currentIndex = prev, lineIndex = 0, isPlaying = false))
        }
    }

    fun setSpeedMultiplier(value: Float) {
        _uiState.update { it.copy(playerState = it.playerState.copy(speedMultiplier = value)) }
    }

    fun setFontScale(value: Float) {
        _uiState.update { it.copy(playerState = it.playerState.copy(fontScale = value)) }
    }

    fun setLineSpacing(value: Float) {
        _uiState.update { it.copy(playerState = it.playerState.copy(lineSpacing = value)) }
    }

    fun toggleMirror(enabled: Boolean) {
        _uiState.update { it.copy(playerState = it.playerState.copy(mirror = enabled)) }
    }

    fun updateLineIndex(index: Int) {
        _uiState.update { it.copy(playerState = it.playerState.copy(lineIndex = index)) }
    }

    fun upsertSegment(segment: Segment) {
        val list = _uiState.value.project.segments.toMutableList()
        val idx = list.indexOfFirst { it.id == segment.id }
        if (idx >= 0) list[idx] = segment.copy(updatedAt = System.currentTimeMillis())
        else list.add(segment)
        persist(list)
    }

    fun deleteSegment(id: String) {
        val list = _uiState.value.project.segments.filterNot { it.id == id }
        persist(list)
    }

    fun moveSegmentUp(id: String) {
        val list = _uiState.value.project.segments.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx > 0) {
            list.removeAt(idx).also { item -> list.add(idx - 1, item) }
            persist(list)
        }
    }

    fun moveSegmentDown(id: String) {
        val list = _uiState.value.project.segments.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx in 0 until list.lastIndex) {
            list.removeAt(idx).also { item -> list.add(idx + 1, item) }
            persist(list)
        }
    }

    private fun persist(newList: List<Segment>) {
        val updated = _uiState.value.project.copy(
            segments = newList,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.update { it.copy(project = updated) }
        debounceSave(updated)
    }

    private fun debounceSave(project: Project) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            runCatching { repository.save(project) }
                .onFailure { _uiState.update { state -> state.copy(snackbarMessage = "Failed to save project") } }
        }
    }

    fun importProject(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            runCatching { repository.importFromUri(resolver, uri) }
                .onSuccess { imported ->
                    _uiState.update { it.copy(project = imported, playerState = it.playerState.copy(currentIndex = 0, lineIndex = 0)) }
                    onResult(true)
                }
                .onFailure {
                    _uiState.update { it.copy(snackbarMessage = "Failed to import project") }
                    onResult(false)
                }
        }
    }

    fun exportProject(onIntentReady: (Intent) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.exportToShareIntent(_uiState.value.project) }
                .onSuccess { intent -> onIntentReady(intent) }
                .onFailure { _uiState.update { it.copy(snackbarMessage = "Failed to export project") } }
        }
    }

    fun setCurrentIndex(index: Int) {
        _uiState.update { it.copy(playerState = it.playerState.copy(currentIndex = index, lineIndex = 0, isPlaying = false)) }
    }

    fun updateProjectName(name: String) {
        val updated = _uiState.value.project.copy(projectName = name, updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(project = updated) }
        debounceSave(updated)
    }

    fun addEmptySegment() {
        val segment = Segment(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            durationSec = 60
        )
        upsertSegment(segment)
    }
}
