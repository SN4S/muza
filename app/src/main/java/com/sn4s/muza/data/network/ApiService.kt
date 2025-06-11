package com.sn4s.muza.data.network

import com.sn4s.muza.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {
    // Authentication
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Token

    @POST("auth/register")
    suspend fun register(@Body user: UserCreate): User

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Token

    // Current User
    @GET("users/me")
    suspend fun getCurrentUser(): User

    @PUT("users/me")
    suspend fun updateCurrentUser(@Body user: UserBase): User

    // NEW: User profile with image upload
    @Multipart
    @PUT("users/me")
    suspend fun updateCurrentUserWithImage(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("bio") bio: RequestBody?,
        @Part("is_artist") isArtist: RequestBody,
        @Part image: MultipartBody.Part?
    ): User

    @DELETE("users/me/image")
    suspend fun deleteUserImage(): Map<String, String>

    @GET("users/{user_id}/image")
    suspend fun getUserImage(@Path("user_id") userId: Int): ResponseBody

    @GET("users/me/songs")
    suspend fun getCurrentUserSongs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @GET("users/me/albums")
    suspend fun getCurrentUserAlbums(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Album>

    @GET("users/me/playlists")
    suspend fun getCurrentUserPlaylists(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Playlist>

    @GET("users/me/liked-songs")
    suspend fun getLikedSongs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    // Public User Profiles
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

    // Songs
    @Multipart
    @POST("songs/")
    suspend fun createSong(
        @Part("title") title: RequestBody,
        @Part("album_id") albumId: RequestBody?,
        @Part("genre_ids") genreIds: RequestBody?,
        @Part file: MultipartBody.Part,
        @Part cover: MultipartBody.Part?
    ): Song

    @GET("songs/")
    suspend fun getSongs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @GET("songs/{song_id}")
    suspend fun getSong(@Path("song_id") songId: Int): Song

    @Multipart
    @PUT("songs/{song_id}")
    suspend fun updateSong(
        @Path("song_id") songId: Int,
        @Part("title") title: RequestBody?,
        @Part("album_id") albumId: RequestBody?,
        @Part("genre_ids") genreIds: RequestBody?,
        @Part file: MultipartBody.Part?,
        @Part cover: MultipartBody.Part?
    ): Song

    @GET("songs/{song_id}/cover")
    suspend fun getSongCover(@Path("song_id") songId: Int): ResponseBody

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

    // Albums
    @Multipart
    @POST("albums/")
    suspend fun createAlbum(
        @Part("title") title: RequestBody,
        @Part("release_date") releaseDate: RequestBody,
        @Part cover: MultipartBody.Part?
    ): Album

    // Album likes
    @POST("albums/{albumId}/like")
    suspend fun likeAlbum(@Path("albumId") albumId: Int)

    @DELETE("albums/{albumId}/like")
    suspend fun unlikeAlbum(@Path("albumId") albumId: Int)

    @GET("albums/{albumId}/is-liked")
    suspend fun isAlbumLiked(@Path("albumId") albumId: Int): LikeResponse

    @GET("users/me/liked-albums")
    suspend fun getLikedAlbums(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Album>

    @GET("albums/")
    suspend fun getAlbums(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Album>

    @GET("albums/{album_id}")
    suspend fun getAlbum(@Path("album_id") albumId: Int): Album

    @Multipart
    @PUT("albums/{album_id}")
    suspend fun updateAlbum(
        @Path("album_id") albumId: Int,
        @Part("title") title: RequestBody?,
        @Part("release_date") releaseDate: RequestBody?,
        @Part cover: MultipartBody.Part?
    ): Album

    @DELETE("albums/{album_id}")
    suspend fun deleteAlbum(@Path("album_id") albumId: Int)

    @GET("albums/{album_id}/songs")
    suspend fun getAlbumSongs(
        @Path("album_id") albumId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Song>

    @GET("albums/{album_id}/cover")
    suspend fun getAlbumCover(@Path("album_id") albumId: Int): ResponseBody

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
    @POST("playlists/")
    suspend fun createPlaylist(@Body playlist: PlaylistCreate): Playlist

    @GET("playlists/")
    suspend fun getPlaylists(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Playlist>

    @GET("playlists/{playlist_id}")
    suspend fun getPlaylist(@Path("playlist_id") playlistId: Int): Playlist

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
    @POST("genres/")
    suspend fun createGenre(@Body genre: GenreCreate): Genre

    @GET("genres/")
    suspend fun getGenres(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<Genre>

    @GET("genres/{genre_id}")
    suspend fun getGenre(@Path("genre_id") genreId: Int): Genre

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

    @POST("songs/check-likes")
    suspend fun checkMultipleLikes(@Body songIds: List<Int>): Map<Int, Boolean>

    // Social Features
    @POST("users/follow/{user_id}")
    suspend fun followUser(@Path("user_id") userId: Int): FollowResponse

    @DELETE("users/follow/{user_id}")
    suspend fun unfollowUser(@Path("user_id") userId: Int): FollowResponse

    @GET("users/follow/{user_id}/status")
    suspend fun getFollowStatus(@Path("user_id") userId: Int): FollowResponse

    @GET("users/following")
    suspend fun getMyFollowing(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): List<UserProfile>

    @GET("users/followers")
    suspend fun getMyFollowers(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): List<UserProfile>

    @GET("users/{user_id}/profile")
    suspend fun getUserProfile(@Path("user_id") userId: Int): UserProfile
}