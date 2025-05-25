package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.User
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getCurrentUser()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { user ->
                    _user.value = user
                    _isLoading.value = false
                }
        }
    }

    fun logout() {
        // Get the AuthViewModel from the SavedStateHandle
        val authViewModel = savedStateHandle.get<AuthViewModel>("authViewModel")
        authViewModel?.logout()
    }
} 