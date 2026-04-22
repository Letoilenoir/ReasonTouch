package com.reasontouch.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionSettingsViewModel @Inject constructor(
    private val repository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val session = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun setName(name: String) {
        val current = session.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(
                name = name.take(40),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun setBpm(bpm: Int) {
        val current = session.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(
                bpm = bpm.coerceIn(20, 300),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun setTotalBars(bars: Int) {
        val current = session.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(
                totalBars = bars.coerceIn(1, 64),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun setTimeSignatureNumerator(num: Int) {
        val current = session.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(
                timeSignatureNumerator = num.coerceIn(2, 12),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun setKeyRoot(root: String) {
        val current = session.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(
                keyRoot = root,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun setKeyQuality(quality: String) {
        val current = session.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(
                keyQuality = quality,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun notifySaved() { _saved.value = true }
}