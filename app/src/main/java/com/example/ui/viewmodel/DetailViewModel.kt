package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Report
import com.example.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReportRepository(database.reportDao())
    private val sessionManager = com.example.data.local.SessionManager(application)

    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()

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

    fun loadReport(id: Long) {
        viewModelScope.launch {
            repository.getReportById(id).collectLatest {
                _report.value = it
            }
        }
    }

    fun deleteReport(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteReport(id)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus laporan")
            }
        }
    }
}
