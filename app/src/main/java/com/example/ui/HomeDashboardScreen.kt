package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThumbnailProject
import com.example.data.VisualStylePreset
import com.example.ui.theme.BebasNeueFontFamily
import com.example.util.ThumbnailCanvasRenderer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    projectsState: DataUiState<List<ThumbnailProject>>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateNewThumbnail: () -> Unit,
    onExploreStyles: () -> Unit,
    onNavigate: (StudioDestination) -> Unit,
    onEditProject: (ThumbnailProject) -> Unit,
    onDuplicateProject: (ThumbnailProject) -> Unit,
    onRenameProject: (ThumbnailProject, String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var projectToRename by remember { mutableStateOf<ThumbnailProject?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (projectToRename != null) {
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            containerColor = Color(0xFF161F33),
            title = { Text("Rename Project", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectToRename?.let { onRenameProject(it, renameText) }
                        projectToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text("Search your thumbnails, topics, headlines, or styles...", color = Color(0xFF64748B))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF111827),
                    unfocusedContainerColor = Color(0xFF111827),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_bar")
            )
        }

        // 2. Hero Studio Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF111827),
                                Color(0xFF1E1B4B),
                                Color(0xFF3B1D0A)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(900f, 500f)
                        )
                    )
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), CircleShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PHOTOREALISTIC AI ART DIRECTOR",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }

                    Text(
                        text = "Turn Your Ideas Into Powerful Thumbnails",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )

                    Text(
                        text = "Create cinematic, realistic, high-impact thumbnails with AI.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onCreateNewThumbnail,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF090D16)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_thumbnail_primary_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Thumbnail", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onExploreStyles,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("explore_styles_secondary_btn")
                        ) {
                            Icon(Icons.Default.Style, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Explore Styles", color = Color.White)
                        }
                    }
                }
            }
        }

        // 3. Studio Quick Modules Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Studio Workspaces",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkspaceQuickCard(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI Thumbnail Generator",
                        subtitle = "Topic → Art Direction → Image",
                        accent = Color(0xFFF59E0B),
                        onClick = { onNavigate(StudioDestination.GENERATOR) }
                    )
                    WorkspaceQuickCard(
                        icon = Icons.Default.Layers,
                        title = "Layer & Canvas Editor",
                        subtitle = "Typography, Grading & AI Edit",
                        accent = Color(0xFF38BDF8),
                        onClick = { onNavigate(StudioDestination.EDITOR) }
                    )
                    WorkspaceQuickCard(
                        icon = Icons.Default.Style,
                        title = "Templates & Style Presets",
                        subtitle = "18 Cinematic Presets & Layouts",
                        accent = Color(0xFFA855F7),
                        onClick = { onNavigate(StudioDestination.STYLES) }
                    )
                    WorkspaceQuickCard(
                        icon = Icons.Default.Palette,
                        title = "Brand Kit",
                        subtitle = "Channel Fonts, Colors & Tags",
                        accent = Color(0xFF10B981),
                        onClick = { onNavigate(StudioDestination.BRAND_KIT) }
                    )
                    WorkspaceQuickCard(
                        icon = Icons.Default.History,
                        title = "Generation History",
                        subtitle = "Past Prompts & Art Plans",
                        accent = Color(0xFFF43F5E),
                        onClick = { onNavigate(StudioDestination.HISTORY) }
                    )
                    WorkspaceQuickCard(
                        icon = Icons.Default.Settings,
                        title = "Account & AI Settings",
                        subtitle = "Model Config & Preferences",
                        accent = Color(0xFF6366F1),
                        onClick = { onNavigate(StudioDestination.SETTINGS) }
                    )
                }
            }
        }

        // 4. Recent Projects / My Thumbnails Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Projects & My Thumbnails",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Synced with Cloud Firestore • Tap any project to open in the multi-layer editor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        when (projectsState) {
            is DataUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B))
                    }
                }
            }

            is DataUiState.Error -> {
                item {
                    Surface(
                        color = Color(0xFF450A0A),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = projectsState.message,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is DataUiState.Success -> {
                val filtered = projectsState.data.filter { proj ->
                    searchQuery.isBlank() ||
                        proj.title.contains(searchQuery, ignoreCase = true) ||
                        proj.videoTopic.contains(searchQuery, ignoreCase = true) ||
                        proj.headline.contains(searchQuery, ignoreCase = true) ||
                        proj.visualStyle.contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    item {
                        StarterBlueprintSection(
                            onOpenStarterInGenerator = onCreateNewThumbnail,
                            onExploreStyles = onExploreStyles
                        )
                    }
                } else {
                    items(filtered, key = { it.id.ifBlank { it.title } }) { project ->
                        ProjectDashboardCard(
                            project = project,
                            onEdit = { onEditProject(project) },
                            onDuplicate = { onDuplicateProject(project) },
                            onRename = {
                                renameText = project.title
                                projectToRename = project
                            },
                            onDelete = { onDeleteProject(project.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceQuickCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(195.dp)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectDashboardCard(
    project: ThumbnailProject,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val stylePreset = remember(project.visualStyle) {
        VisualStylePreset.entries.find { it.displayName == project.visualStyle }
            ?: VisualStylePreset.REALISTIC_DOCUMENTARY
    }

    val cachedBitmap = remember(project.localImagePath) {
        if (project.localImagePath.isNotBlank()) {
            val file = File(project.localImagePath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } else {
            null
        }
    }

    val formattedDate = remember(project.updatedAt, project.createdAt) {
        val ts = project.updatedAt ?: project.createdAt
        if (ts != null) {
            SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(ts.toDate())
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .clickable(onClick = onEdit)
            .testTag("project_card_${project.id}")
    ) {
        Column {
            // 16:9 Thumbnail Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF0B0F19))
            ) {
                if (cachedBitmap != null) {
                    Image(
                        bitmap = cachedBitmap.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Render studio style preview with headline
                    val c1 = Color(ThumbnailCanvasRenderer.parseColorSafe(stylePreset.primaryColorHex))
                    val c2 = Color(ThumbnailCanvasRenderer.parseColorSafe(stylePreset.secondaryColorHex))
                    val acc = Color(ThumbnailCanvasRenderer.parseColorSafe(stylePreset.accentColorHex))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(c1, c2, Color(0xFF090D16))
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(acc.copy(alpha = 0.35f), Color.Transparent),
                                center = Offset(size.width * 0.75f, size.height * 0.5f),
                                radius = size.width * 0.45f
                            )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = Color(ThumbnailCanvasRenderer.parseColorSafe(project.accentColorHex)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = project.category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = project.headline.ifBlank { project.title }.uppercase(),
                            fontFamily = BebasNeueFontFamily,
                            fontSize = 32.sp,
                            lineHeight = 32.sp,
                            color = Color.White
                        )
                        if (project.subheadline.isNotBlank()) {
                            Text(
                                text = project.subheadline,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }

                // Dimensions badge top-right
                Surface(
                    color = Color(0xFF090D16).copy(alpha = 0.82f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${project.width}×${project.height} • ${project.aspectRatio}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Metadata & Action Bar
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Edited $formattedDate • ${project.visualStyle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF090D16)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Project", tint = Color.White)
                    }

                    IconButton(
                        onClick = onRename,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename Project", tint = Color.White)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF450A0A))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Color(0xFFFCA5A5))
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterBlueprintSection(
    onOpenStarterInGenerator: () -> Unit,
    onExploreStyles: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "No Saved Projects Yet — Start Your First Art-Directed Thumbnail",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Text(
                text = "Enter a video topic in the AI Thumbnail Generator to produce a complete Art Direction Plan, generate a photorealistic 16:9 composition, and customize crisp vector typography in the Studio Editor.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenStarterInGenerator,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Launch AI Generator", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onExploreStyles,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Browse 18 Style Presets", color = Color.White)
                }
            }
        }
    }
}
