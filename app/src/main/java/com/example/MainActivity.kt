package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StudioRepository
import com.example.ui.AccountSettingsScreen
import com.example.ui.AiGeneratorScreen
import com.example.ui.AuthScreen
import com.example.ui.BrandKitScreen
import com.example.ui.GenerationHistoryScreen
import com.example.ui.HomeDashboardScreen
import com.example.ui.StudioDestination
import com.example.ui.StudioViewModel
import com.example.ui.TemplatesAndStylesScreen
import com.example.ui.ThumbnailEditorScreen
import com.example.ui.theme.ThumbnailAIStudioTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ThumbnailAIStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        AuthenticatedStudioApp(currentUserId = user.uid)
    }
}

@Composable
private fun AuthenticatedStudioApp(currentUserId: String) {
    val context = LocalContext.current
    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = StudioViewModel.provideFactory(
            repository = StudioRepository(
                FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
            ),
            currentUserId = currentUserId
        )
    )

    val currentDestination by viewModel.currentDestination.collectAsState()
    val projectsState by viewModel.projectsState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()

    // Mandatory BackHandler for custom state-switched secondary screens
    if (currentDestination != StudioDestination.HOME) {
        BackHandler {
            viewModel.navigateTo(StudioDestination.HOME)
        }
    }

    val navItems = listOf(
        StudioDestination.HOME to Icons.Default.Dashboard,
        StudioDestination.GENERATOR to Icons.Default.AutoAwesome,
        StudioDestination.EDITOR to Icons.Default.Layers,
        StudioDestination.STYLES to Icons.Default.Style,
        StudioDestination.BRAND_KIT to Icons.Default.Palette
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color(0xFF090D16),
            topBar = {
                StudioTopHeaderBar(
                    currentDestination = currentDestination,
                    onNavigateHome = { viewModel.navigateTo(StudioDestination.HOME) },
                    onNavigateHistory = { viewModel.navigateTo(StudioDestination.HISTORY) },
                    onNavigateSettings = { viewModel.navigateTo(StudioDestination.SETTINGS) }
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = Color(0xFF0B0F19),
                        contentColor = Color.White
                    ) {
                        navItems.forEach { (dest, icon) ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = {
                                    Icon(icon, contentDescription = dest.label)
                                },
                                label = {
                                    Text(dest.label, maxLines = 1)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF090D16),
                                    selectedTextColor = Color(0xFFFBBF24),
                                    indicatorColor = Color(0xFFF59E0B),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = Color(0xFF0B0F19),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { (dest, icon) ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = { Icon(icon, contentDescription = dest.label) },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedVisibility(visible = statusBanner != null) {
                        Surface(
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = statusBanner ?: "",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = viewModel::clearStatusBanner,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentDestination) {
                            StudioDestination.HOME -> {
                                HomeDashboardScreen(
                                    projectsState = projectsState,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onCreateNewThumbnail = viewModel::startNewBlankProject,
                                    onExploreStyles = { viewModel.navigateTo(StudioDestination.STYLES) },
                                    onNavigate = viewModel::navigateTo,
                                    onEditProject = viewModel::openProjectInEditor,
                                    onDuplicateProject = viewModel::duplicateExistingProject,
                                    onRenameProject = viewModel::renameExistingProject,
                                    onDeleteProject = viewModel::deleteExistingProject
                                )
                            }

                            StudioDestination.GENERATOR -> {
                                AiGeneratorScreen(viewModel = viewModel)
                            }

                            StudioDestination.EDITOR -> {
                                ThumbnailEditorScreen(viewModel = viewModel)
                            }

                            StudioDestination.STYLES -> {
                                TemplatesAndStylesScreen(viewModel = viewModel)
                            }

                            StudioDestination.BRAND_KIT -> {
                                BrandKitScreen(viewModel = viewModel)
                            }

                            StudioDestination.HISTORY -> {
                                GenerationHistoryScreen(viewModel = viewModel)
                            }

                            StudioDestination.SETTINGS -> {
                                AccountSettingsScreen(
                                    viewModel = viewModel,
                                    onSignedOut = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTopHeaderBar(
    currentDestination: StudioDestination,
    onNavigateHome: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Surface(
        color = Color(0xFF0B0F19),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onNavigateHome)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = "Thumbnail AI Studio",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Thumbnail AI Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateHistory,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161F33))
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Generation History",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161F33))
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Account Settings",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
