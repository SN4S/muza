package com.sn4s.muza.data.network

import com.sn4s.muza.data.model.*
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

    @GET("users/me")
    suspend fun getCurrentUser(): User

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

    @PUT("users/me")
    suspend fun updateProfile(
        @Body user: UserBase
    ): User
} 