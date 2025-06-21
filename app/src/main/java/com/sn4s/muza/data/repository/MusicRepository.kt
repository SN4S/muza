package com.sn4s.muza.data.repository

import android.content.Context
import android.util.Log
import com.sn4s.muza.data.model.*
import com.sn4s.muza.data.network.ApiService
import com.sn4s.muza.data.security.TokenManager
import com.sn4s.muza.player.MusicPlayerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val playerManager: MusicPlayerManager
) {

    // Authentication
    suspend fun login(username: String, password: String): Token {
        return apiService.login(username, password)
    }

    suspend fun register(user: UserCreate): User {
        return apiService.register(user)
    }

    suspend fun refreshToken(refreshToken: String): Token {
        return apiService.refreshToken(RefreshTokenRequest(refreshToken))
    }

    // Current user
    fun getCurrentUser(): Flow<User> = flow {
        emit(apiService.getCurrentUser())
    }

    suspend fun updateCurrentUser(user: UserBase): User {
        return apiService.updateCurrentUser(user)
    }

    // NEW: User profile with image upload
    suspend fun updateUserProfile(userUpdate: UserUpdate, context: Context): User {
        val usernameBody = userUpdate.username.toRequestBody("text/plain".toMediaType())
        val emailBody = userUpdate.email.toRequestBody("text/plain".toMediaType())
        val bioBody = userUpdate.bio?.toRequestBody("text/plain".toMediaType())
        val isArtistBody = userUpdate.isArtist.toString().toRequestBody("text/plain".toMediaType())

        val imagePart = if (userUpdate.imageUri != null) {
            val uri = userUpdate.imageUri
            // Copy URI content to temporary file
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_profile_image.jpg")

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val requestFile = tempFile.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
            //tempFile.delete()
        } else {
            null
        }

        return apiService.updateCurrentUserWithImage(
            username = usernameBody,
            email = emailBody,
            bio = bioBody,
            isArtist = isArtistBody,
            image = imagePart
        )
    }

    suspend fun deleteUserImage() {
        apiService.deleteUserImage()
    }

    // Helper method to get user image URL
    fun getUserImageUrl(userId: Int): String {
        return "${com.sn4s.muza.di.NetworkModule.BASE_URL}users/$userId/image"
    }

    fun getCurrentUserSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getCurrentUserSongs(skip, limit))
    }

    fun getCurrentUserAlbums(skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getCurrentUserAlbums(skip, limit))
    }

    fun getCurrentUserPlaylists(skip: Int = 0, limit: Int = 100): Flow<List<Playlist>> = flow {
        emit(apiService.getCurrentUserPlaylists(skip, limit))
    }

    // Songs
    suspend fun createSong(
        title: String,
        file: File,
        cover: File? = null,
        albumId: Int? = null,
        genreIds: List<Int>? = null
    ): Song {
        val titleBody = title.toRequestBody("text/plain".toMediaType())
        val albumIdBody = albumId?.toString()?.toRequestBody("text/plain".toMediaType())
        val genreIdsBody = genreIds?.joinToString(",")?.toRequestBody("text/plain".toMediaType())
        val fileBody = file.asRequestBody("audio/*".toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)

        val coverPart = cover?.let { coverFile ->
            val coverBody = coverFile.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("cover", coverFile.name, coverBody)
        }

        return apiService.createSong(titleBody, albumIdBody, genreIdsBody, filePart,coverPart)
    }

    fun getSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getSongs(skip, limit))
    }

    suspend fun getSong(songId: Int): Song {
        return apiService.getSong(songId)
    }

    suspend fun updateSong(
        songId: Int,
        title: String? = null,
        albumId: Int? = null,
        genreIds: List<Int>? = null,
        file: File? = null,
        cover: File? = null
    ): Song {
        val titleBody = title?.toRequestBody("text/plain".toMediaType())
        val albumIdBody = albumId?.toString()?.toRequestBody("text/plain".toMediaType())
        val genreIdsBody = genreIds?.joinToString(",")?.toRequestBody("text/plain".toMediaType())
        val filePart = file?.let {
            val fileBody = it.asRequestBody("audio/*".toMediaType())
            MultipartBody.Part.createFormData("file", it.name, fileBody)
        }
        val coverPart = cover?.let { coverFile ->
            val coverBody = coverFile.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("cover", coverFile.name, coverBody)
        }

        return apiService.updateSong(songId, titleBody, albumIdBody, genreIdsBody, filePart, coverPart)
    }

    suspend fun deleteSong(songId: Int) {
        apiService.deleteSong(songId)
    }

    fun getSongCoverUrl(songId: Int): String {
        return "${com.sn4s.muza.di.NetworkModule.BASE_URL}songs/$songId/cover"
    }

    fun getAlbumCoverUrl(albumId: Int): String {
        return "${com.sn4s.muza.di.NetworkModule.BASE_URL}albums/$albumId/cover"
    }

    // Search
    fun searchSongs(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Song>> = flow {
        emit(apiService.searchSongs(query, skip, limit))
    }

    fun searchArtists(query: String, skip: Int = 0, limit: Int = 20): Flow<List<User>> = flow {
        emit(apiService.searchArtists(query, skip, limit))
    }

    fun searchAlbums(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Album>> = flow {
        emit(apiService.searchAlbums(query, skip, limit))
    }

    fun searchPlaylists(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Playlist>> = flow {
        emit(apiService.searchPlaylists(query, skip, limit))
    }

    fun searchGenres(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Genre>> = flow {
        emit(apiService.searchGenres(query, skip, limit))
    }

    // Albums
    fun getAlbums(skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getAlbums(skip, limit))
    }

    suspend fun getAlbum(albumId: Int): Album {
        return apiService.getAlbum(albumId)
    }

    fun getUserAlbums(skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getCurrentUserAlbums(skip, limit))  // For current user
    }

    suspend fun likeAlbum(albumId: Int) {
        apiService.likeAlbum(albumId)
    }

    suspend fun unlikeAlbum(albumId: Int) {
        apiService.unlikeAlbum(albumId)
    }

    suspend fun isAlbumLiked(albumId: Int): Boolean {
        return apiService.isAlbumLiked(albumId).isLiked
    }

    fun getLikedAlbums(skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getLikedAlbums(skip, limit))
    }

    suspend fun createAlbum(
        title: String,
        releaseDate: String,
        cover: File? = null
    ): Album {
        val titleBody = title.toRequestBody("text/plain".toMediaType())
        val releaseDateBody = releaseDate.toRequestBody("text/plain".toMediaType())

        val coverPart = cover?.let { coverFile ->
            val coverBody = coverFile.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("cover", coverFile.name, coverBody)
        }

        return apiService.createAlbum(titleBody, releaseDateBody, coverPart)
    }

    suspend fun updateAlbum(
        albumId: Int,
        title: String,
        releaseDate: String,
        cover: File? = null
    ): Album {
        val titleBody = title.toRequestBody("text/plain".toMediaType())
        val releaseDateBody = releaseDate.toRequestBody("text/plain".toMediaType())

        val coverPart = cover?.let { coverFile ->
            val coverBody = coverFile.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("cover", coverFile.name, coverBody)
        }

        return apiService.updateAlbum(albumId,titleBody,releaseDateBody,coverPart)
    }

    suspend fun deleteAlbum(albumId: Int) {
        apiService.deleteAlbum(albumId)
    }

    fun getAlbumSongs(albumId: Int, skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getAlbumSongs(albumId, skip, limit))
    }

    suspend fun addSongToAlbum(albumId: Int, songId: Int) {
        apiService.addSongToAlbum(albumId, songId)
    }

    suspend fun removeSongFromAlbum(albumId: Int, songId: Int) {
        apiService.removeSongFromAlbum(albumId, songId)
    }

    // Playlists
    fun getPlaylists(skip: Int = 0, limit: Int = 100): Flow<List<Playlist>> = flow {
        emit(apiService.getPlaylists(skip, limit))
    }

    suspend fun getPlaylist(playlistId: Int): Playlist {
        return apiService.getPlaylist(playlistId)
    }

    fun getUserPlaylists(skip: Int = 0, limit: Int = 100): Flow<List<Playlist>> = flow {
        emit(apiService.getCurrentUserPlaylists(skip, limit))
    }

    suspend fun createPlaylist(playlist: PlaylistCreate): Playlist {
        return apiService.createPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlistId: Int, playlist: PlaylistCreate): Playlist {
        return apiService.updatePlaylist(playlistId, playlist)
    }

    suspend fun deletePlaylist(playlistId: Int) {
        apiService.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: Int) {
        apiService.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: Int) {
        apiService.removeSongFromPlaylist(playlistId, songId)
    }

    // Liked Songs
    fun getLikedSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getLikedSongs(skip, limit))
    }

    // Like System
    suspend fun likeSong(songId: Int) {
        apiService.likeSong(songId)
    }

    suspend fun unlikeSong(songId: Int) {
        apiService.unlikeSong(songId)
    }

    suspend fun isSongLiked(songId: Int): Boolean {
        return apiService.isSongLiked(songId).isLiked
    }

    suspend fun checkMultipleLikes(songIds: List<Int>): Map<Int, Boolean> {
        return apiService.checkMultipleLikes(songIds)
    }

    // Genres
    fun getGenres(skip: Int = 0, limit: Int = 100): Flow<List<Genre>> = flow {
        emit(apiService.getGenres(skip, limit))
    }

    suspend fun getGenre(genreId: Int): Genre {
        return apiService.getGenre(genreId)
    }

    suspend fun createGenre(genre: GenreCreate): Genre {
        return apiService.createGenre(genre)
    }

    suspend fun updateGenre(genreId: Int, genre: GenreCreate): Genre {
        return apiService.updateGenre(genreId, genre)
    }

    suspend fun deleteGenre(genreId: Int) {
        apiService.deleteGenre(genreId)
    }

    fun getGenreSongs(genreId: Int, skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getGenreSongs(genreId, skip, limit))
    }

    // Public user profiles
    suspend fun getUser(userId: Int): UserNested {
        return apiService.getUser(userId)
    }

    fun getPublicUserSongs(userId: Int, skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getUserSongs(userId, skip, limit))  // For other users
    }

    fun getPublicUserAlbums(userId: Int, skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getUserAlbums(userId, skip, limit))  // For other users
    }

    suspend fun logout() {
        playerManager.stop()
        tokenManager.clearToken()
    }

    // Social Features
    suspend fun followUser(userId: Int): FollowResponse {
        return apiService.followUser(userId)
    }

    suspend fun unfollowUser(userId: Int): FollowResponse {
        return apiService.unfollowUser(userId)
    }

    suspend fun getFollowStatus(userId: Int): FollowResponse {
        return apiService.getFollowStatus(userId)
    }

    suspend fun getUserProfile(userId: Int): UserProfile {
        return apiService.getUserProfile(userId)
    }

//    fun getMyFollowing(skip: Int = 0, limit: Int = 50): Flow<List<UserProfile>> = flow {
//        emit(apiService.getMyFollowing(skip, limit))
//    }

    fun getMyFollowing(skip: Int = 0, limit: Int = 50): Flow<List<UserProfile>> = flow {
        try {
            val response = apiService.getMyFollowing(skip, limit)
            emit(response)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error getting following", e)
            emit(emptyList())
        }
    }

    fun getMyFollowers(skip: Int = 0, limit: Int = 50): Flow<List<UserProfile>> = flow {
        emit(apiService.getMyFollowers(skip, limit))
    }
}