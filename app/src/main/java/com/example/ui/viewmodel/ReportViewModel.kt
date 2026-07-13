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

    fun createReport(title: String, description: String) {
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
            val result = repository.addReport(title, description)
            if (result.isSuccess) {
                _uiState.value = ReportUiState.Success
            } else {
                val exception = result.exceptionOrNull()
                // In local mode, even if Supabase sync fails, it saved locally, but we can treat it as a warning or success
                _uiState.value = ReportUiState.Error(exception?.localizedMessage ?: "Gagal membuat laporan")
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState.Idle
    }
}
