package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClipItem
import com.example.data.SupabaseRepository
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SendUiState(
    val inputMode: Int = 0, // 0 = Text, 1 = File
    val textContent: String = "",
    val selectedFileName: String? = null,
    val selectedFileBytes: ByteArray? = null,
    val selectedFileIsImage: Boolean = false,
    val burnOnRead: Boolean = false,
    val isUploading: Boolean = false,
    val createdClip: ClipItem? = null,
    val qrBitmap: ImageBitmap? = null,
    val countdownSeconds: Long = 0,
    val errorMessage: String? = null
)

data class ReceiveUiState(
    val digits: List<String> = listOf("", "", "", ""),
    val isFetching: Boolean = false,
    val retrievedClip: ClipItem? = null,
    val wasBurned: Boolean = false,
    val errorMessage: String? = null
)

class ClipboardViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SupabaseRepository(application.applicationContext)

    private val _selectedTab = MutableStateFlow(0) // 0: Send, 1: Receive
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _sendState = MutableStateFlow(SendUiState())
    val sendState: StateFlow<SendUiState> = _sendState.asStateFlow()

    private val _receiveState = MutableStateFlow(ReceiveUiState())
    val receiveState: StateFlow<ReceiveUiState> = _receiveState.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isDocsOpen = MutableStateFlow(false)
    val isDocsOpen: StateFlow<Boolean> = _isDocsOpen.asStateFlow()

    private var countdownJob: Job? = null

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setInputMode(mode: Int) {
        _sendState.update { it.copy(inputMode = mode) }
    }

    fun updateTextContent(text: String) {
        _sendState.update { it.copy(textContent = text, errorMessage = null) }
    }

    fun setSelectedFile(name: String, bytes: ByteArray, isImage: Boolean) {
        _sendState.update {
            it.copy(
                selectedFileName = name,
                selectedFileBytes = bytes,
                selectedFileIsImage = isImage,
                errorMessage = null
            )
        }
    }

    fun clearSelectedFile() {
        _sendState.update {
            it.copy(
                selectedFileName = null,
                selectedFileBytes = null,
                selectedFileIsImage = false
            )
        }
    }

    fun toggleBurnOnRead(burn: Boolean) {
        _sendState.update { it.copy(burnOnRead = burn) }
    }

    fun createClip() {
        val currentState = _sendState.value
        val isText = currentState.inputMode == 0
        if (isText && currentState.textContent.isBlank()) {
            _sendState.update { it.copy(errorMessage = "Please enter some text, code, or a link.") }
            return
        }
        if (!isText && currentState.selectedFileBytes == null) {
            _sendState.update { it.copy(errorMessage = "Please select a file or image to upload.") }
            return
        }

        viewModelScope.launch {
            _sendState.update { it.copy(isUploading = true, errorMessage = null) }
            val result = repository.createClip(
                content = if (isText) currentState.textContent.trim() else null,
                fileBytes = if (!isText) currentState.selectedFileBytes else null,
                fileName = if (!isText) currentState.selectedFileName else null,
                isImage = currentState.selectedFileIsImage,
                burnOnRead = currentState.burnOnRead
            )

            result.fold(
                onSuccess = { clip ->
                    val qrUrl = "https://temporary-clipboard.web.app/?code=${clip.code}"
                    val qr = QrCodeGenerator.generate(qrUrl)
                    _sendState.update {
                        it.copy(
                            isUploading = false,
                            createdClip = clip,
                            qrBitmap = qr,
                            countdownSeconds = clip.remainingSeconds
                        )
                    }
                    startCountdown(clip.remainingSeconds)
                },
                onFailure = { error ->
                    _sendState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = error.message ?: "Failed to create clip"
                        )
                    }
                }
            )
        }
    }

    private fun startCountdown(totalSeconds: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var seconds = totalSeconds
            while (isActive && seconds > 0) {
                delay(1000)
                seconds--
                _sendState.update { it.copy(countdownSeconds = seconds) }
            }
        }
    }

    fun resetSendState() {
        countdownJob?.cancel()
        _sendState.value = SendUiState()
    }

    // -------------------------------------------------------------
    // Receiver Logic
    // -------------------------------------------------------------
    fun updateDigit(index: Int, char: String) {
        val cleanChar = char.filter { it.isDigit() }.takeLast(1)
        val newDigits = _receiveState.value.digits.toMutableList()
        newDigits[index] = cleanChar
        _receiveState.update { it.copy(digits = newDigits, errorMessage = null) }

        val fullCode = newDigits.joinToString("")
        if (fullCode.length == 4) {
            fetchClip(fullCode)
        }
    }

    fun pastePin(pastedText: String) {
        val digitsOnly = pastedText.filter { it.isDigit() }.take(4)
        if (digitsOnly.length == 4) {
            val newDigits = listOf(
                digitsOnly[0].toString(),
                digitsOnly[1].toString(),
                digitsOnly[2].toString(),
                digitsOnly[3].toString()
            )
            _receiveState.update { it.copy(digits = newDigits, errorMessage = null) }
            fetchClip(digitsOnly)
        }
    }

    fun fetchClip(code: String? = null) {
        val targetCode = code ?: _receiveState.value.digits.joinToString("")
        if (targetCode.length != 4) {
            _receiveState.update { it.copy(errorMessage = "Please enter a 4-digit code") }
            return
        }

        viewModelScope.launch {
            _receiveState.update { it.copy(isFetching = true, errorMessage = null) }
            val result = repository.getClip(targetCode)

            result.fold(
                onSuccess = { clip ->
                    if (clip == null) {
                        _receiveState.update {
                            it.copy(
                                isFetching = false,
                                retrievedClip = null,
                                errorMessage = "Clip expired or does not exist"
                            )
                        }
                    } else {
                        _receiveState.update {
                            it.copy(
                                isFetching = false,
                                retrievedClip = clip,
                                wasBurned = clip.burnOnRead,
                                errorMessage = null
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _receiveState.update {
                        it.copy(
                            isFetching = false,
                            errorMessage = error.message ?: "Failed to fetch clip"
                        )
                    }
                }
            )
        }
    }

    fun clearRetrievedClip() {
        _receiveState.value = ReceiveUiState()
    }

    // -------------------------------------------------------------
    // Settings & Dialogs
    // -------------------------------------------------------------
    fun openSettings(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun openDocs(open: Boolean) {
        _isDocsOpen.value = open
    }

    fun saveSupabaseConfig(url: String, key: String) {
        repository.supabaseUrl = url
        repository.supabaseAnonKey = key
        _isSettingsOpen.value = false
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
