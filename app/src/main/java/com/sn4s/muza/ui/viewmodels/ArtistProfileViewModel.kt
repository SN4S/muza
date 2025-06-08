// REPLACE YOUR EXISTING app/src/main/java/com/sn4s/muza/ui/viewmodels/ArtistProfileViewModel.kt

package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.*
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    // Keep existing fields for backward compatibility
    private val _artist = MutableStateFlow<UserNested?>(null)
    val artist: StateFlow<UserNested?> = _artist

    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList())
    val artistSongs: StateFlow<List<Song>> = _artistSongs

    private val _artistAlbums = MutableStateFlow<List<Album>>(emptyList())
    val artistAlbums: StateFlow<List<Album>> = _artistAlbums

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // NEW: Add social features
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _followStatus = MutableStateFlow<FollowResponse?>(null)
    val followStatus: StateFlow<FollowResponse?> = _followStatus

    // Enhanced load function that gets both basic info and social info
    fun loadArtist(artistId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load basic artist profile (existing logic)
                val artist = repository.getUser(artistId)
                _artist.value = artist

                // NEW: Load enhanced profile with social stats
                try {
                    val enhancedProfile = repository.getUserProfile(artistId)
                    _userProfile.value = enhancedProfile

                    val followStatus = repository.getFollowStatus(artistId)
                    _followStatus.value = followStatus

                    Log.d("ArtistProfileViewModel", "Loaded social profile: followers=${enhancedProfile.followerCount}")
                } catch (e: Exception) {
                    Log.w("ArtistProfileViewModel", "Social features not available", e)
                    // Fallback: Create basic UserProfile from UserNested
                    _userProfile.value = UserProfile(
                        id = artist.id,
                        username = artist.username,
                        bio = artist.bio,
                        image = artist.image,
                        isArtist = artist.isArtist,
                        followerCount = 0,
                        followingCount = 0,
                        songCount = 0,
                        isFollowing = null
                    )
                }

                // Load content (existing logic)
                if (artist.isArtist) {
                    // Load artist's songs
                    repository.getPublicUserSongs(artistId)
                        .catch { e ->
                            Log.e("ArtistProfileViewModel", "Failed to load artist songs", e)
                        }
                        .collect { songs ->
                            _artistSongs.value = songs
                        }

                    // Load artist's albums
                    repository.getPublicUserAlbums(artistId)
                        .catch { e ->
                            Log.e("ArtistProfileViewModel", "Failed to load artist albums", e)
                        }
                        .collect { albums ->
                            _artistAlbums.value = albums
                        }
                } else {
                    // For non-artists, just load their songs if any
                    try {
                        repository.getPublicUserSongs(artistId)
                            .catch { e ->
                                Log.e("ArtistProfileViewModel", "Failed to load user songs", e)
                            }
                            .collect { songs ->
                                _artistSongs.value = songs
                            }
                    } catch (e: Exception) {
                        Log.d("ArtistProfileViewModel", "User has no public songs")
                    }
                }

                Log.d("ArtistProfileViewModel", "Loaded artist: ${artist.username}")
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Failed to load artist", e)
                _error.value = e.message ?: "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW: Social features
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

                Log.d("ArtistProfileViewModel", "Follow toggled: ${newStatus.isFollowing}")
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Failed to toggle follow", e)
                _error.value = e.message ?: "Failed to toggle follow"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Computed property to determine if we should show social features
    val hasSocialFeatures: StateFlow<Boolean> = _userProfile.map { profile ->
        profile != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
}