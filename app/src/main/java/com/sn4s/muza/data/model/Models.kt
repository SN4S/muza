package com.sn4s.muza.data.model

import java.time.LocalDateTime
import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val email: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean = false,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    val songs: List<SongNested> = emptyList(),
    val albums: List<AlbumNested> = emptyList()
)

data class UserNested(
    val id: Int,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean
)

data class Song(
    val id: Int,
    val title: String,
    val duration: Int? = null,
    @SerializedName("file_path")
    val filePath: String,
    @SerializedName("album_id")
    val albumId: Int? = null,
    @SerializedName("creator_id")
    val creatorId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val creator: UserNested
)

data class SongNested(
    val id: Int,
    val title: String,
    val duration: Int,
    @SerializedName("file_path")
    val filePath: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class Album(
    val id: Int,
    val title: String,
    @SerializedName("release_date")
    val releaseDate: String,
    @SerializedName("cover_image")
    val coverImage: String? = null,
    @SerializedName("creator_id")
    val creatorId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val songs: List<SongNested> = emptyList(),
    val creator: UserNested
)

data class AlbumNested(
    val id: Int,
    val title: String,
    @SerializedName("release_date")
    val releaseDate: String,
    @SerializedName("cover_image")
    val coverImage: String? = null,
    @SerializedName("created_at")
    val createdAt: String
)

data class Playlist(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val songs: List<SongNested> = emptyList()
)

data class Genre(
    val id: Int,
    val name: String,
    val description: String? = null,
    val songs: List<SongNested> = emptyList()
)

data class Token(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

data class UserCreate(
    val email: String,
    val username: String,
    val password: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean = false
)

data class UserUpdate(
    val username: String? = null,
    val bio: String? = null,
    val image: String? = null
)

data class UserBase(
    val email: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean = false
) 