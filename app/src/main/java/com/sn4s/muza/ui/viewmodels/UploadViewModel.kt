package com.sn4s.muza.ui.viewmodels

import android.content.Context
import android.net.Uri
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
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

    init {
        loadUserContent()
    }

    private fun loadUserContent() {
        loadUserAlbums()
        loadUserSongs()
    }

    private fun loadUserAlbums() {
        viewModelScope.launch {
            try {
                repository.getUserAlbums()
                    .catch { e ->
                        Log.e("UploadViewModel", "Failed to load albums", e)
                        _error.value = "Failed to load albums: ${e.message}"
                    }
                    .collect { albums ->
                        _userAlbums.value = albums
                    }
            } catch (e: Exception) {
                Log.e("UploadViewModel", "Error loading albums", e)
                _error.value = "Error loading albums: ${e.message}"
            }
        }
    }

    private fun loadUserSongs() {
        viewModelScope.launch {
            try {
                repository.getUserSongs()
                    .catch { e ->
                        Log.e("UploadViewModel", "Failed to load user songs", e)
                        _error.value = "Failed to load songs: ${e.message}"
                    }
                    .collect { songs ->
                        _userSongs.value = songs
                    }
            } catch (e: Exception) {
                Log.e("UploadViewModel", "Error loading user songs", e)
                _error.value = "Error loading songs: ${e.message}"
            }
        }
    }

    fun uploadSong(
        title: String,
        albumId: Int?,
        audioFileUri: Uri
    ) {
        if (title.isBlank()) {
            _error.value = "Song title cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _uploadProgress.value = 0f

            try {
                Log.d("UploadViewModel", "Starting upload for: $title")

                // Get file from URI
                val audioFile = createFileFromUri(audioFileUri)
                if (audioFile == null || !audioFile.exists()) {
                    _error.value = "Could not access audio file"
                    return@launch
                }

                Log.d("UploadViewModel", "File created: ${audioFile.name}, size: ${audioFile.length()}")
                _uploadProgress.value = 0.2f

                // Prepare request bodies
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val albumIdBody = albumId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                // Create file part
                val requestFile = audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

                _uploadProgress.value = 0.4f

                Log.d("UploadViewModel", "Uploading to server...")
                val uploadedSong = repository.uploadSong(
                    title = titleBody,
                    albumId = albumIdBody,
                    file = filePart
                )

                _uploadProgress.value = 1f
                _uploadSuccess.value = true

                Log.d("UploadViewModel", "Upload successful: ${uploadedSong.title}")

                // Clean up temp file
                try {
                    audioFile.delete()
                } catch (e: Exception) {
                    Log.w("UploadViewModel", "Could not delete temp file", e)
                }

                // Refresh content
                loadUserSongs()

            } catch (e: Exception) {
                Log.e("UploadViewModel", "Upload failed", e)
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

    fun createAlbum(
        title: String,
        releaseDate: String? = null
    ) {
        if (title.isBlank()) {
            _error.value = "Album title cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("UploadViewModel", "Creating album: $title")

                val albumCreate = AlbumCreate(
                    title = title,
                    releaseDate = releaseDate ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )

                val createdAlbum = repository.createAlbum(albumCreate)

                Log.d("UploadViewModel", "Album created successfully: ${createdAlbum.title}")

                // Refresh albums list
                loadUserAlbums()

            } catch (e: Exception) {
                Log.e("UploadViewModel", "Album creation failed", e)
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
                Log.d("UploadViewModel", "Deleting song: $songId")
                repository.deleteSong(songId)

                Log.d("UploadViewModel", "Song deleted successfully")

                // Refresh songs list
                loadUserSongs()

            } catch (e: Exception) {
                Log.e("UploadViewModel", "Delete song failed", e)
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
                Log.d("UploadViewModel", "Deleting album: $albumId")
                repository.deleteAlbum(albumId)

                Log.d("UploadViewModel", "Album deleted successfully")

                // Refresh albums and songs lists (songs might be affected)
                loadUserAlbums()
                loadUserSongs()

            } catch (e: Exception) {
                Log.e("UploadViewModel", "Delete album failed", e)
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

    fun refreshContent() {
        loadUserContent()
    }

    private fun createFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("UploadViewModel", "Could not open input stream for URI: $uri")
                return null
            }

            val fileName = getFileNameFromUri(uri) ?: "audio_${System.currentTimeMillis()}.mp3"
            val file = File(context.cacheDir, fileName)

            Log.d("UploadViewModel", "Creating temp file: ${file.absolutePath}")

            inputStream.use { input ->
                file.outputStream().use { output ->
                    val bytes = input.copyTo(output)
                    Log.d("UploadViewModel", "Copied $bytes bytes to temp file")
                }
            }

            if (file.length() == 0L) {
                Log.e("UploadViewModel", "Created file is empty")
                file.delete()
                return null
            }

            file
        } catch (e: Exception) {
            Log.e("UploadViewModel", "Failed to create file from URI: $uri", e)
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("UploadViewModel", "Failed to get file name from URI", e)
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearUploadSuccess() {
        _uploadSuccess.value = false
    }
}