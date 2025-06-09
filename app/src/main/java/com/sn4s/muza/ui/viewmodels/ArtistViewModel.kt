package com.sn4s.muza.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.AlbumCreate
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress

    private val _userAlbums = MutableStateFlow<List<Album>>(emptyList())
    val userAlbums: StateFlow<List<Album>> = _userAlbums

    private val _userSongs = MutableStateFlow<List<Song>>(emptyList())
    val userSongs: StateFlow<List<Song>> = _userSongs

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    private val _selectedTab = MutableStateFlow(0) // 0: Songs, 1: Albums, 2: Upload
    val selectedTab: StateFlow<Int> = _selectedTab

    init {
        loadArtistContent()
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    private fun loadArtistContent() {
        loadUserAlbums()
        loadUserSongs()
    }

    private fun loadUserAlbums() {
        viewModelScope.launch {
            try {
                repository.getUserAlbums()
                    .catch { e ->
                        Log.e("ArtistViewModel", "Failed to load albums", e)
                        _error.value = "Failed to load albums: ${e.message}"
                    }
                    .collect { albums ->
                        _userAlbums.value = albums
                    }
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Error loading albums", e)
                _error.value = "Error loading albums: ${e.message}"
            }
        }
    }

    private fun loadUserSongs() {
        viewModelScope.launch {
            try {
                repository.getCurrentUserSongs()
                    .catch { e ->
                        Log.e("ArtistViewModel", "Failed to load user songs", e)
                        _error.value = "Failed to load songs: ${e.message}"
                    }
                    .collect { songs ->
                        _userSongs.value = songs
                    }
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Error loading user songs", e)
                _error.value = "Error loading songs: ${e.message}"
            }
        }
    }

    fun uploadSong(title: String, albumId: Int?, audioFileUri: Uri) {
        if (title.isBlank()) {
            _error.value = "Song title cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _uploadProgress.value = 0f

            try {
                Log.d("ArtistViewModel", "Starting upload for: $title")

                val audioFile = createFileFromUri(audioFileUri)
                if (audioFile == null || !audioFile.exists()) {
                    _error.value = "Could not access audio file"
                    return@launch
                }

                Log.d("ArtistViewModel", "File created: ${audioFile.name}, size: ${audioFile.length()}")
                _uploadProgress.value = 0.2f

                _uploadProgress.value = 0.4f

                Log.d("ArtistViewModel", "Uploading to server...")
                // FIXED: Use createSong instead of uploadSong
                val uploadedSong = repository.createSong(
                    title = title,
                    file = audioFile,
                    albumId = albumId,
                    genreIds = null
                )

                _uploadProgress.value = 1f
                _uploadSuccess.value = true

                Log.d("ArtistViewModel", "Upload successful: ${uploadedSong.title}")

                try {
                    audioFile.delete()
                } catch (e: Exception) {
                    Log.w("ArtistViewModel", "Could not delete temp file", e)
                }

                loadUserSongs()

            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Upload failed", e)
                _error.value = when {
                    e.message?.contains("401") == true -> "Authentication failed. Please login again."
                    e.message?.contains("413") == true -> "File too large. Please select a smaller file."
                    e.message?.contains("415") == true -> "Unsupported file format. Please select an audio file."
                    e.message?.contains("network") == true -> "Network error. Please check your connection."
                    else -> e.message ?: "Upload failed. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAlbum(title: String, releaseDate: String? = null) {
        if (title.isBlank()) {
            _error.value = "Album title cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("ArtistViewModel", "Creating album: $title")

                val albumCreate = AlbumCreate(
                    title = title,
                    releaseDate = releaseDate ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )

                val createdAlbum = repository.createAlbum(albumCreate)

                Log.d("ArtistViewModel", "Album created successfully: ${createdAlbum.title}")
                loadUserAlbums()

            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Album creation failed", e)
                _error.value = when {
                    e.message?.contains("401") == true -> "Authentication failed. Please login again."
                    e.message?.contains("409") == true -> "Album already exists with this name."
                    else -> e.message ?: "Failed to create album. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSong(songId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("ArtistViewModel", "Deleting song: $songId")
                repository.deleteSong(songId)
                Log.d("ArtistViewModel", "Song deleted successfully")
                loadUserSongs()
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Delete song failed", e)
                _error.value = when {
                    e.message?.contains("401") == true -> "Authentication failed. Please login again."
                    e.message?.contains("404") == true -> "Song not found."
                    e.message?.contains("403") == true -> "You don't have permission to delete this song."
                    else -> e.message ?: "Failed to delete song. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAlbum(albumId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("ArtistViewModel", "Deleting album: $albumId")
                repository.deleteAlbum(albumId)
                Log.d("ArtistViewModel", "Album deleted successfully")
                loadUserAlbums()
                loadUserSongs()
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Delete album failed", e)
                _error.value = when {
                    e.message?.contains("401") == true -> "Authentication failed. Please login again."
                    e.message?.contains("404") == true -> "Album not found."
                    e.message?.contains("403") == true -> "You don't have permission to delete this album."
                    e.message?.contains("409") == true -> "Cannot delete album with songs. Remove songs first."
                    else -> e.message ?: "Failed to delete album. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("ArtistViewModel", "Could not open input stream for URI: $uri")
                return null
            }

            val fileName = getFileNameFromUri(uri) ?: "audio_${System.currentTimeMillis()}.mp3"
            val file = File(context.cacheDir, fileName)

            Log.d("ArtistViewModel", "Creating temp file: ${file.absolutePath}")

            inputStream.use { input ->
                file.outputStream().use { output ->
                    val bytes = input.copyTo(output)
                    Log.d("ArtistViewModel", "Copied $bytes bytes to temp file")
                }
            }

            if (file.length() == 0L) {
                Log.e("ArtistViewModel", "Created file is empty")
                file.delete()
                return null
            }

            file
        } catch (e: Exception) {
            Log.e("ArtistViewModel", "Failed to create file from URI: $uri", e)
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = c.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun clearError() {
        _error.value = null
    }

    fun clearUploadSuccess() {
        _uploadSuccess.value = false
    }

    fun refreshContent() {
        loadArtistContent()
    }
}