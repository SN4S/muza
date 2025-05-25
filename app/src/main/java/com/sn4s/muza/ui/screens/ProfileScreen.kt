package com.sn4s.muza.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sn4s.muza.ui.viewmodels.AuthViewModel
import com.sn4s.muza.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editedUsername by viewModel.editedUsername.collectAsState()
    val editedBio by viewModel.editedBio.collectAsState()
    val editedIsArtist by viewModel.editedIsArtist.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(
                    text = "Error: ${error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            user != null -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedUsername,
                                onValueChange = { viewModel.updateUsername(it) },
                                label = { Text("Username") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = editedBio,
                                onValueChange = { viewModel.updateBio(it) },
                                label = { Text("Bio") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = editedIsArtist,
                                    onCheckedChange = { viewModel.updateIsArtist(it) }
                                )
                                Text(
                                    text = "I am an artist",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = { viewModel.cancelEditing() }
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { viewModel.saveProfile() }
                                ) {
                                    Text("Save")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = user!!.username,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = user!!.email,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    if (user!!.bio != null) {
                                        Text(
                                            text = user!!.bio!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                    if (user!!.isArtist) {
                                        Text(
                                            text = "Artist",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.startEditing() }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { 
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout")
                }
            }
            else -> {
                Text("No user data available")
            }
        }
    }
} 