package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportUiState {
    object Idle : ReportUiState()
    object Loading : ReportUiState()
    object Success : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReportRepository(database.reportDao())
    private val sessionManager = com.example.data.local.SessionManager(application)

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        if (sessionManager.isLoggedIn) {
            com.example.data.network.SupabaseService.sessionToken = sessionManager.sessionToken
            com.example.data.network.SupabaseService.userEmail = sessionManager.userEmail
            com.example.data.network.SupabaseService.userId = sessionManager.userId

            if (sessionManager.userId.isNullOrEmpty() && !sessionManager.sessionToken.isNullOrEmpty()) {
                viewModelScope.launch {
                    val result = com.example.data.network.SupabaseService.fetchCurrentUser()
                    if (result.isSuccess) {
                        sessionManager.userId = result.getOrNull()
                    }
                }
            }
        }
    }

    fun createReport(title: String, description: String, imageUri: android.net.Uri? = null) {
        if (title.isBlank()) {
            _uiState.value = ReportUiState.Error("Judul aduan ('Report name') tidak boleh kosong")
            return
        }
        if (description.isBlank()) {
            _uiState.value = ReportUiState.Error("Deskripsi aduan ('Description') tidak boleh kosong")
            return
        }

        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            var localImagePath: String? = null
            if (imageUri != null) {
                localImagePath = saveImageToInternalStorage(imageUri)
            }
            val result = repository.addReport(title, description, localImagePath)
            if (result.isSuccess) {
                _uiState.value = ReportUiState.Success
            } else {
                val exception = result.exceptionOrNull()
                // In local mode, even if Supabase sync fails, it saved locally, but we can treat it as a warning or success
                _uiState.value = ReportUiState.Error(exception?.localizedMessage ?: "Gagal membuat laporan")
            }
        }
    }

    private fun saveImageToInternalStorage(uri: android.net.Uri): String? {
        val context = getApplication<Application>()
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(context.filesDir, "report_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ReportViewModel", "Error saving image", e)
            null
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState.Idle
    }
}
