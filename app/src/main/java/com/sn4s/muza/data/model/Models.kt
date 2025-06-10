package com.sn4s.muza.data.model

import android.net.Uri
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
    val songs: List<Song> = emptyList(),
    val albums: List<AlbumNested> = emptyList()
)

data class UserUpdate(
    val username: String,
    val email: String,
    val bio: String?,
    val isArtist: Boolean,
    val imageUri: Uri? = null // For local image selection
)

data class UserNested(
    val id: Int,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean
)

data class FollowResponse(
    @SerializedName("is_following")
    val isFollowing: Boolean,
    @SerializedName("follower_count")
    val followerCount: Int,
    @SerializedName("following_count")
    val followingCount: Int
)

data class UserProfile(
    val id: Int,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean,
    @SerializedName("follower_count")
    val followerCount: Int = 0,
    @SerializedName("following_count")
    val followingCount: Int = 0,
    @SerializedName("song_count")
    val songCount: Int = 0,
    @SerializedName("is_following")
    val isFollowing: Boolean? = null
)

// Main Song model - use everywhere
data class Song(
    val id: Int,
    val title: String,
    val duration: Int? = null,
    @SerializedName("file_path")
    val filePath: String,
    @SerializedName("cover_image")
    val coverImage: String? = null,
    @SerializedName("album_id")
    val albumId: Int? = null,
    @SerializedName("creator_id")
    val creatorId: Int = 0,
    @SerializedName("created_at")
    val createdAt: String,
    val creator: UserNested,
    @SerializedName("like_count")
    val likeCount: Int = 0
)

data class LikeResponse(
    @SerializedName("is_liked")
    val isLiked: Boolean
)

// Keep SongNested for backward compatibility but make it an alias
typealias SongNested = Song

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
    val songs: List<Song> = emptyList(),
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

data class AlbumCreate(
    val title: String,
    @SerializedName("release_date")
    val releaseDate: String,
    @SerializedName("cover_image")
    val coverImage: String? = null
)

data class PlaylistCreate(
    val name: String,
    val description: String? = null
)

data class GenreCreate(
    val name: String,
    val description: String? = null
)

data class Playlist(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val songs: List<Song> = emptyList()
)

data class Genre(
    val id: Int,
    val name: String,
    val description: String? = null,
    val songs: List<Song> = emptyList()
)

data class Token(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
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

data class UserBase(
    val email: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    @SerializedName("is_artist")
    val isArtist: Boolean = false
)

fun Song.getCoverImageUrl(baseUrl: String): String? {
    return if (coverImage != null) "$baseUrl/songs/$id/cover" else null
}

fun Album.getCoverImageUrl(baseUrl: String): String? {
    return if (coverImage != null) "$baseUrl/albums/$id/cover" else null
}

fun User.getImageUrl(baseUrl: String): String {
    return "$baseUrl/users/$id/image"
}

// Data classes for cover info
data class CoverInfo(
    val url: String?,
    val type: CoverType,
    val fallbackText: String?
)

enum class CoverType {
    SONG_COVER,
    ALBUM_COVER,
    ARTIST_AVATAR,
    INITIALS
}

// Extension function for initials
fun String.getInitials(): String {
    return split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifBlank { take(2).uppercase() }
}