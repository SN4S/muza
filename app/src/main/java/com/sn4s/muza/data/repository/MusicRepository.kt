package com.sn4s.muza.data.repository

import android.util.Log
import com.sn4s.muza.data.model.*
import com.sn4s.muza.data.network.ApiService
import com.sn4s.muza.data.security.TokenManager
import com.sn4s.muza.player.MusicPlayerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val playerManager: MusicPlayerManager
) {
    // Auth
    suspend fun login(username: String, password: String): Token {
        Log.d("MusicRepository", "Attempting login for user: $username")
        val response = apiService.login(username, password)
        Log.d("MusicRepository", "Login response: accessToken=${response.accessToken}, tokenType=${response.tokenType}")
        return response
    }

    suspend fun register(user: UserCreate): User {
        return apiService.register(user)
    }

    fun getCurrentUser(): Flow<User> = flow {
        emit(apiService.getCurrentUser())
    }

    suspend fun updateProfile(username: String, bio: String?, isArtist: Boolean): User {
        Log.d("MusicRepository", "Updating profile with isArtist: $isArtist")
        val currentUser = getCurrentUser().first()
        val userBase = UserBase(
            email = currentUser.email,
            username = username,
            bio = bio,
            image = currentUser.image,
            isArtist = isArtist
        )
        Log.d("MusicRepository", "Sending update request with UserBase: $userBase")
        val updatedUser = apiService.updateProfile(userBase)
        Log.d("MusicRepository", "Received updated user: $updatedUser")
        return updatedUser
    }

    // Songs
    fun getSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getSongs(skip, limit))
    }

    suspend fun getSong(songId: Int): Song {
        return apiService.getSong(songId)
    }

    fun getUserSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getUserSongs(skip, limit))
    }

    suspend fun uploadSong(
        title: RequestBody,
        albumId: RequestBody?,
        file: MultipartBody.Part
    ): Song {
        return apiService.createSong(title, albumId, null, file)
    }

    suspend fun updateSong(
        songId: Int,
        title: RequestBody?,
        albumId: RequestBody?,
        file: MultipartBody.Part?
    ): Song {
        return apiService.updateSong(songId, title, albumId, null, file)
    }

    suspend fun deleteSong(songId: Int) {
        apiService.deleteSong(songId)
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
        emit(apiService.getUserAlbums(skip, limit))
    }

    suspend fun createAlbum(album: AlbumCreate): Album {
        return apiService.createAlbum(album)
    }

    suspend fun updateAlbum(albumId: Int, album: AlbumCreate): Album {
        return apiService.updateAlbum(albumId, album)
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
        emit(apiService.getUserPlaylists(skip, limit))
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

    fun getUserSongs(userId: Int, skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getUserSongs(userId, skip, limit))
    }

    fun getUserAlbums(userId: Int, skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getUserAlbums(userId, skip, limit))
    }

    suspend fun logout() {
        playerManager.stop()
        tokenManager.clearToken()
    }
}