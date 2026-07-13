package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.model.Report
import com.example.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReportRepository(database.reportDao())
    private val sessionManager = SessionManager(application)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Observe reports flow from Room DB, automatically updates UI reactively
    val reports: StateFlow<List<Report>> = repository.allReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userEmail: String
        get() = sessionManager.userEmail ?: "Warga"

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
        refreshReports()
    }

    fun refreshReports() {
        _isRefreshing.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = repository.refreshReports()
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Gagal sinkronisasi data"
            }
            _isRefreshing.value = false
        }
    }

    fun logout() {
        sessionManager.logout()
        viewModelScope.launch {
            com.example.data.network.SupabaseService.logout()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
