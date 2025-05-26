package com.sn4s.muza.data.network

import com.sn4s.muza.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body user: UserCreate): User

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Token

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Token

    @GET("users/me")
    suspend fun getCurrentUser(): User

    @PUT("users/me")
    suspend fun updateProfile(@Body user: UserBase): User

    // Songs
    @GET("songs")
    suspend fun getSongs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @GET("songs/{song_id}")
    suspend fun getSong(@Path("song_id") songId: Int): Song

    @GET("songs/{song_id}/stream")
    suspend fun streamSong(
        @Path("song_id") songId: Int,
        @Query("range") range: String? = null
    ): ResponseBody

    @GET("songs/{song_id}/info")
    suspend fun getSongInfo(@Path("song_id") songId: Int): ResponseBody

    @Multipart
    @POST("songs/")
    suspend fun createSong(
        @Part("title") title: RequestBody,
        @Part("album_id") albumId: RequestBody?,
        @Part("genre_ids") genreIds: RequestBody?,
        @Part file: MultipartBody.Part
    ): Song

    @Multipart
    @PUT("songs/{song_id}")
    suspend fun updateSong(
        @Path("song_id") songId: Int,
        @Part("title") title: RequestBody?,
        @Part("album_id") albumId: RequestBody?,
        @Part("genre_ids") genreIds: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Song

    @DELETE("songs/{song_id}")
    suspend fun deleteSong(@Path("song_id") songId: Int)

    // Like System
    @POST("songs/{song_id}/like")
    suspend fun likeSong(@Path("song_id") songId: Int)

    @DELETE("songs/{song_id}/like")
    suspend fun unlikeSong(@Path("song_id") songId: Int)

    @GET("songs/{song_id}/is-liked")
    suspend fun isSongLiked(@Path("song_id") songId: Int): LikeResponse

    // Search
    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<Song>

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<User>

    @GET("search/albums")
    suspend fun searchAlbums(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<Album>

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<Playlist>

    @GET("search/genres")
    suspend fun searchGenres(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<Genre>

    // User content
    @GET("users/me/songs")
    suspend fun getUserSongs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @GET("users/me/playlists")
    suspend fun getUserPlaylists(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Playlist>

    @GET("users/me/albums")
    suspend fun getUserAlbums(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Album>

    @GET("users/me/liked-songs")
    suspend fun getLikedSongs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    // Public user profiles
    @GET("users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: Int): UserNested

    @GET("users/{user_id}/songs")
    suspend fun getUserSongs(
        @Path("user_id") userId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @GET("users/{user_id}/albums")
    suspend fun getUserAlbums(
        @Path("user_id") userId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Album>

    // Albums
    @GET("albums")
    suspend fun getAlbums(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Album>

    @GET("albums/{album_id}")
    suspend fun getAlbum(@Path("album_id") albumId: Int): Album

    @POST("albums/")
    suspend fun createAlbum(@Body album: AlbumCreate): Album

    @PUT("albums/{album_id}")
    suspend fun updateAlbum(
        @Path("album_id") albumId: Int,
        @Body album: AlbumCreate
    ): Album

    @DELETE("albums/{album_id}")
    suspend fun deleteAlbum(@Path("album_id") albumId: Int)

    @GET("albums/{album_id}/songs")
    suspend fun getAlbumSongs(
        @Path("album_id") albumId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @POST("albums/{album_id}/songs/{song_id}")
    suspend fun addSongToAlbum(
        @Path("album_id") albumId: Int,
        @Path("song_id") songId: Int
    )

    @DELETE("albums/{album_id}/songs/{song_id}")
    suspend fun removeSongFromAlbum(
        @Path("album_id") albumId: Int,
        @Path("song_id") songId: Int
    )

    // Playlists
    @GET("playlists")
    suspend fun getPlaylists(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Playlist>

    @GET("playlists/{playlist_id}")
    suspend fun getPlaylist(@Path("playlist_id") playlistId: Int): Playlist

    @POST("playlists/")
    suspend fun createPlaylist(@Body playlist: PlaylistCreate): Playlist

    @PUT("playlists/{playlist_id}")
    suspend fun updatePlaylist(
        @Path("playlist_id") playlistId: Int,
        @Body playlist: PlaylistCreate
    ): Playlist

    @DELETE("playlists/{playlist_id}")
    suspend fun deletePlaylist(@Path("playlist_id") playlistId: Int)

    @POST("playlists/{playlist_id}/songs/{song_id}")
    suspend fun addSongToPlaylist(
        @Path("playlist_id") playlistId: Int,
        @Path("song_id") songId: Int
    )

    @DELETE("playlists/{playlist_id}/songs/{song_id}")
    suspend fun removeSongFromPlaylist(
        @Path("playlist_id") playlistId: Int,
        @Path("song_id") songId: Int
    )

    // Genres
    @GET("genres")
    suspend fun getGenres(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Genre>

    @GET("genres/{genre_id}")
    suspend fun getGenre(@Path("genre_id") genreId: Int): Genre

    @POST("genres/")
    suspend fun createGenre(@Body genre: GenreCreate): Genre

    @PUT("genres/{genre_id}")
    suspend fun updateGenre(
        @Path("genre_id") genreId: Int,
        @Body genre: GenreCreate
    ): Genre

    @DELETE("genres/{genre_id}")
    suspend fun deleteGenre(@Path("genre_id") genreId: Int)

    @GET("genres/{genre_id}/songs")
    suspend fun getGenreSongs(
        @Path("genre_id") genreId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>
}