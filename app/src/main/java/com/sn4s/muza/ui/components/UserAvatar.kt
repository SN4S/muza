package com.sn4s.muza.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sn4s.muza.di.NetworkModule

@Composable
fun UserAvatar(
    userId: Int,
    username: String,
    imageUrl: String? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    showFallback: Boolean = true
) {
    val context = LocalContext.current
    val actualImageUrl = if (!imageUrl.isNullOrBlank()) {
        "${NetworkModule.BASE_URL}users/$userId/image"
    } else {
        null
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Always show fallback background first
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (size >= 48.dp) {
                    Text(
                        text = username.take(2).uppercase(),
                        style = when {
                            size >= 120.dp -> MaterialTheme.typography.headlineLarge
                            size >= 64.dp -> MaterialTheme.typography.headlineMedium
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.6f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Overlay image if available
        if (actualImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(actualImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$username's avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// Convenience composable for users with profile data
@Composable
fun UserAvatar(
    userProfile: com.sn4s.muza.data.model.UserProfile,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    UserAvatar(
        userId = userProfile.id,
        username = userProfile.username,
        imageUrl = userProfile.image,
        size = size,
        modifier = modifier
    )
}

@Composable
fun UserAvatar(
    userNested: com.sn4s.muza.data.model.UserNested,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    UserAvatar(
        userId = userNested.id,
        username = userNested.username,
        imageUrl = userNested.image,
        size = size,
        modifier = modifier
    )
}

@Composable
fun UserAvatar(
    user: com.sn4s.muza.data.model.User,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    UserAvatar(
        userId = user.id,
        username = user.username,
        imageUrl = user.image,
        size = size,
        modifier = modifier
    )
}