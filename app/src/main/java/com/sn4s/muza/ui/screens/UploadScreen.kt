package com.sn4s.muza.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.viewmodels.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    navController: NavController,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    LaunchedEffect(Unit) {
        NetworkModule.unauthorizedEvent.collect {
            if (currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    var title by remember { mutableStateOf("") }
    var selectedAlbumId by remember { mutableStateOf<Int?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumTitle by remember { mutableStateOf("") }

    val userAlbums by viewModel.userAlbums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAudioUri = uri
    }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            // Reset form
            title = ""
            selectedAlbumId = null
            selectedAudioUri = null
            viewModel.clearUploadSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Upload Music",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Song Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Song Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        )

        // Album Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Album:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 16.dp)
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = userAlbums.find { it.id == selectedAlbumId }?.title ?: "No Album",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    enabled = !isLoading
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No Album") },
                        onClick = {
                            selectedAlbumId = null
                            expanded = false
                        }
                    )
                    userAlbums.forEach { album ->
                        DropdownMenuItem(
                            text = { Text(album.title) },
                            onClick = {
                                selectedAlbumId = album.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            TextButton(
                onClick = { showCreateAlbumDialog = true },
                enabled = !isLoading
            ) {
                Text("New Album")
            }
        }

        // Audio File Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            onClick = { audioPickerLauncher.launch("audio/*") },
            enabled = !isLoading
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = "Select Audio File",
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = if (selectedAudioUri != null) "Audio file selected" else "Select audio file",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (selectedAudioUri != null) {
                        Text(
                            text = selectedAudioUri.toString().substringAfterLast("/"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Upload Progress
        if (isLoading) {
            LinearProgressIndicator(
                progress = uploadProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Error Message
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Upload Button
        Button(
            onClick = {
                selectedAudioUri?.let { uri ->
                    viewModel.uploadSong(title, selectedAlbumId, uri)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && title.isNotBlank() && selectedAudioUri != null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Upload",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Upload Song")
            }
        }
    }

    // Create Album Dialog
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("Create New Album") },
            text = {
                OutlinedTextField(
                    value = newAlbumTitle,
                    onValueChange = { newAlbumTitle = it },
                    label = { Text("Album Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumTitle.isNotBlank()) {
                            viewModel.createAlbum(newAlbumTitle)
                            newAlbumTitle = ""
                            showCreateAlbumDialog = false
                        }
                    },
                    enabled = newAlbumTitle.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateAlbumDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}