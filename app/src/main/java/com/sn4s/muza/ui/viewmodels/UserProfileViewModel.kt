package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.*
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _followStatus = MutableStateFlow<FollowResponse?>(null)
    val followStatus: StateFlow<FollowResponse?> = _followStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadUserProfile(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val profile = repository.getUserProfile(userId)
                _userProfile.value = profile

                val followStatus = repository.getFollowStatus(userId)
                _followStatus.value = followStatus

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load user profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollow(userId: Int) {
        viewModelScope.launch {
            try {
                val currentStatus = _followStatus.value
                val newStatus = if (currentStatus?.isFollowing == true) {
                    repository.unfollowUser(userId)
                } else {
                    repository.followUser(userId)
                }
                _followStatus.value = newStatus

                // Update profile with new counts
                _userProfile.value = _userProfile.value?.copy(
                    followerCount = newStatus.followerCount,
                    followingCount = newStatus.followingCount,
                    isFollowing = newStatus.isFollowing
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to toggle follow"
            }
        }
    }
}