package com.sn4s.muza.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.User
import com.sn4s.muza.data.model.UserUpdate
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
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

    private val _editedEmail = MutableStateFlow("")
    val editedEmail: StateFlow<String> = _editedEmail

    private val _editedBio = MutableStateFlow("")
    val editedBio: StateFlow<String> = _editedBio

    private val _editedIsArtist = MutableStateFlow(false)
    val editedIsArtist: StateFlow<Boolean> = _editedIsArtist

    // New image-related states
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _imageUpdateSuccess = MutableStateFlow(false)
    val imageUpdateSuccess: StateFlow<Boolean> = _imageUpdateSuccess

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
                    _editedEmail.value = user.email
                    _editedBio.value = user.bio ?: ""
                    _editedIsArtist.value = user.isArtist
                    _isLoading.value = false
                }
        }
    }

    fun startEditing() {
        _isEditing.value = true
        _selectedImageUri.value = null
        _imageUpdateSuccess.value = false
    }

    fun cancelEditing() {
        _isEditing.value = false
        _user.value?.let { user ->
            _editedUsername.value = user.username
            _editedEmail.value = user.email
            _editedBio.value = user.bio ?: ""
            _editedIsArtist.value = user.isArtist
        }
        _selectedImageUri.value = null
        _error.value = null
    }

    fun updateUsername(username: String) {
        _editedUsername.value = username
    }

    fun updateEmail(email: String) {
        _editedEmail.value = email
    }

    fun updateBio(bio: String) {
        _editedBio.value = bio
    }

    fun updateIsArtist(isArtist: Boolean) {
        Log.d("ProfileViewModel", "Updating isArtist to: $isArtist")
        _editedIsArtist.value = isArtist
    }

    fun selectImage(uri: Uri) {
        _selectedImageUri.value = uri
        Log.d("ProfileViewModel", "Image selected: $uri")
    }

    fun removeSelectedImage() {
        _selectedImageUri.value = null
    }

    fun saveProfile() {
        if (_editedUsername.value.isBlank() || _editedEmail.value.isBlank()) {
            _error.value = "Username and email cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userUpdate = UserUpdate(
                    username = _editedUsername.value,
                    email = _editedEmail.value,
                    bio = _editedBio.value.takeIf { it.isNotBlank() },
                    isArtist = _editedIsArtist.value,
                    imageUri = _selectedImageUri.value
                )

                Log.d("ProfileViewModel", "Saving profile with isArtist: ${_editedIsArtist.value}")
                val updatedUser = repository.updateUserProfile(userUpdate, context)
                Log.d("ProfileViewModel", "Profile updated successfully, new isArtist: ${updatedUser.isArtist}")

                _user.value = updatedUser
                _isEditing.value = false
                _selectedImageUri.value = null
                _imageUpdateSuccess.value = true

                Log.d("ProfileViewModel", "Profile updated successfully")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile", e)
                _error.value = e.message ?: "Failed to update profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUserImage() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteUserImage()
                // Reload user profile to get updated data
                loadUserProfile()
                Log.d("ProfileViewModel", "Profile image deleted successfully")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to delete image", e)
                _error.value = e.message ?: "Failed to delete image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearImageUpdateSuccess() {
        _imageUpdateSuccess.value = false
    }

    // Helper method to get user image URL
    fun getUserImageUrl(userId: Int): String {
        return repository.getUserImageUrl(userId)
    }

    // Helper method to check if current user has an image
    val hasProfileImage: StateFlow<Boolean> = _user.map { user ->
        !user?.image.isNullOrBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}