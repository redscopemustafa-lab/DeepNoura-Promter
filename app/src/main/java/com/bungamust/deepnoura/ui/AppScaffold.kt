package com.bungamust.deepnoura.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bungamust.deepnoura.ui.builder.BuilderScreen
import com.bungamust.deepnoura.ui.player.PlayerScreen
import com.bungamust.deepnoura.viewmodel.ProjectViewModel

private enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    Builder("builder", "Builder", Icons.Default.ViewList),
    Player("player", "Player", Icons.Default.PlayArrow)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: ProjectViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var currentScreen by remember { mutableStateOf(Screen.Builder) }
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.importProject(it) { success ->
                if (success) {
                    currentScreen = Screen.Player
                    navController.navigate(Screen.Player.route) {
                        popUpTo(Screen.Builder.route) { inclusive = false }
                    }
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiState.project.projectName) },
                actions = {
                    Row {
                        IconButton(onClick = { viewModel.toggleDarkMode(!uiState.isDarkMode) }) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Toggle dark")
                        }
                        IconButton(onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import JSON")
                        }
                        IconButton(onClick = {
                            viewModel.exportProject { intent ->
                                exportLauncher.launch(Intent.createChooser(intent, "Share project"))
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export JSON")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Screen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Builder.route,
            modifier = Modifier.fillMaxSize(),
            builder = {
                composable(Screen.Builder.route) {
                    BuilderScreen(
                        paddingValues = padding,
                        viewModel = viewModel,
                        onNavigateToPlayer = {
                            currentScreen = Screen.Player
                            navController.navigate(Screen.Player.route)
                        }
                    )
                }
                composable(Screen.Player.route) {
                    PlayerScreen(paddingValues = padding, viewModel = viewModel)
                }
            }
        )
    }
}
