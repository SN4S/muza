package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Token
import com.sn4s.muza.data.model.User
import com.sn4s.muza.data.model.UserCreate
import com.sn4s.muza.data.repository.MusicRepository
import com.sn4s.muza.data.security.TokenManager
import com.sn4s.muza.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val tokenManager: TokenManager,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val token = tokenManager.getToken()
        Log.d("AuthViewModel", "Checking auth state, token exists: ${token != null}")
        _isAuthenticated.value = token != null
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("AuthViewModel", "Attempting login for user: $username")
                val token = repository.login(username, password)
                Log.d("AuthViewModel", "Login successful, saving token")
                tokenManager.saveToken(token)
                _isAuthenticated.value = true
                Log.d("AuthViewModel", "Authentication state updated to: true")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed", e)
                _error.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("AuthViewModel", "Attempting registration for user: $username")
                val user = UserCreate(
                    email = email,
                    username = username,
                    password = password
                )
                repository.register(user)
                // After registration, automatically log in
                Log.d("AuthViewModel", "Registration successful, attempting auto-login")
                val token = repository.login(username, password)
                Log.d("AuthViewModel", "Auto-login successful, saving token")
                tokenManager.saveToken(token)
                _isAuthenticated.value = true
                Log.d("AuthViewModel", "Authentication state updated to: true")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration failed", e)
                _error.value = e.message ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        Log.d("AuthViewModel", "Logging out")
        playerManager.stop()
        tokenManager.clearToken()
        _isAuthenticated.value = false
        Log.d("AuthViewModel", "Authentication state updated to: false")
    }
} 