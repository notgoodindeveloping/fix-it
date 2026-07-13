package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.network.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val email: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // If already logged in, update state immediately
        if (sessionManager.isLoggedIn) {
            _uiState.value = LoginUiState.Success(sessionManager.userEmail ?: "")
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email dan password tidak boleh kosong")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = SupabaseService.login(email, password)
            if (result.isSuccess) {
                sessionManager.isLoggedIn = true
                sessionManager.userEmail = email
                sessionManager.sessionToken = SupabaseService.sessionToken
                sessionManager.userId = SupabaseService.userId
                _uiState.value = LoginUiState.Success(email)
            } else {
                val exception = result.exceptionOrNull()
                _uiState.value = LoginUiState.Error(exception?.message ?: "Login gagal")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    fun logout() {
        sessionManager.logout()
        _uiState.value = LoginUiState.Idle
    }
}
