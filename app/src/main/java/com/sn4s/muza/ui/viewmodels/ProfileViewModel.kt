package com.sn4s.muza.ui.viewmodels

import android.util.Log
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

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _editedUsername = MutableStateFlow("")
    val editedUsername: StateFlow<String> = _editedUsername

    private val _editedBio = MutableStateFlow("")
    val editedBio: StateFlow<String> = _editedBio

    private val _editedIsArtist = MutableStateFlow(false)
    val editedIsArtist: StateFlow<Boolean> = _editedIsArtist

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
                    _editedUsername.value = user.username
                    _editedBio.value = user.bio ?: ""
                    _editedIsArtist.value = user.isArtist
                    _isLoading.value = false
                }
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
        _user.value?.let { user ->
            _editedUsername.value = user.username
            _editedBio.value = user.bio ?: ""
            _editedIsArtist.value = user.isArtist
        }
    }

    fun updateUsername(username: String) {
        _editedUsername.value = username
    }

    fun updateBio(bio: String) {
        _editedBio.value = bio
    }

    fun updateIsArtist(isArtist: Boolean) {
        Log.d("ProfileViewModel", "Updating isArtist to: $isArtist")
        _editedIsArtist.value = isArtist
    }

    fun saveProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("ProfileViewModel", "Saving profile with isArtist: ${_editedIsArtist.value}")
                val updatedUser = repository.updateProfile(
                    username = _editedUsername.value,
                    bio = _editedBio.value,
                    isArtist = _editedIsArtist.value
                )
                Log.d("ProfileViewModel", "Profile updated successfully, new isArtist: ${updatedUser.isArtist}")
                _user.value = updatedUser
                _isEditing.value = false
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile", e)
                _error.value = e.message ?: "Failed to update profile"
            } finally {
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