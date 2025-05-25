package com.sn4s.muza.data.model

import java.time.LocalDateTime
import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val email: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    val isArtist: Boolean = false,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val songs: List<SongNested> = emptyList(),
    val albums: List<AlbumNested> = emptyList()
)

data class UserNested(
    val id: Int,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    val isArtist: Boolean
)

data class Song(
    val id: Int,
    val title: String,
    val duration: Int? = null,
    val filePath: String,
    val albumId: Int? = null,
    val creatorId: Int,
    val createdAt: LocalDateTime,
    val creator: UserNested
)

data class SongNested(
    val id: Int,
    val title: String,
    val duration: Int,
    val filePath: String,
    val createdAt: LocalDateTime
)

data class Album(
    val id: Int,
    val title: String,
    val releaseDate: LocalDateTime,
    val coverImage: String? = null,
    val creatorId: Int,
    val createdAt: LocalDateTime,
    val songs: List<SongNested> = emptyList(),
    val creator: UserNested
)

data class AlbumNested(
    val id: Int,
    val title: String,
    val releaseDate: LocalDateTime,
    val coverImage: String? = null,
    val createdAt: LocalDateTime
)

data class Playlist(
    val id: Int,
    val name: String,
    val description: String? = null,
    val ownerId: Int,
    val createdAt: LocalDateTime,
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
    val isArtist: Boolean = false
) 