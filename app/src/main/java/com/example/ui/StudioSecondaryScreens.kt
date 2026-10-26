package com.example.ui

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AiImageModel
import com.example.data.BrandKit
import com.example.data.CompositionPreset
import com.example.data.GenerationHistoryItem
import com.example.data.ImageRealismMode
import com.example.data.StudioFont
import com.example.data.ThumbnailAspectRatio
import com.example.data.ThumbnailProject
import com.example.data.VideoCategory
import com.example.data.VisualStylePreset
import com.example.util.ThumbnailCanvasRenderer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun TemplatesAndStylesScreen(
    viewModel: StudioViewModel
) {
    val projectsState by viewModel.projectsState.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedComposition by viewModel.selectedComposition.collectAsState()

    val savedTemplates = (projectsState as? DataUiState.Success)?.data?.filter { it.isTemplate } ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Templates, Style Presets & Layouts", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                "Explore 18 art-directed photographic styles, 12 rule-of-thirds composition presets, and your saved studio templates.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }

        if (savedTemplates.isNotEmpty()) {
            item {
                Text("Your Saved Custom Templates", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFBBF24))
            }
            items(savedTemplates, key = { it.id }) { tpl ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("${tpl.visualStyle} • ${tpl.compositionPreset}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = { viewModel.openProjectInEditor(tpl) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                        ) {
                            Text("Use Template", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("18 Visual Style Presets", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        items(VisualStylePreset.entries) { preset ->
            val c1 = Color(ThumbnailCanvasRenderer.parseColorSafe(preset.primaryColorHex))
            val c2 = Color(ThumbnailCanvasRenderer.parseColorSafe(preset.secondaryColorHex))
            val acc = Color(ThumbnailCanvasRenderer.parseColorSafe(preset.accentColorHex))
            val isSelected = preset == selectedStyle

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(c1, c2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(acc)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(preset.shortDescription, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                        Text("Lighting: ${preset.lightingGuide}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF38BDF8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.selectStyle(preset)
                            viewModel.navigateTo(StudioDestination.GENERATOR)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isSelected) "Active" else "Select")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("12 Composition Engine Layouts", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        items(CompositionPreset.entries) { comp ->
            val isSel = comp == selectedComposition
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        viewModel.selectComposition(comp)
                        viewModel.navigateTo(StudioDestination.GENERATOR)
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(comp.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(comp.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun BrandKitScreen(
    viewModel: StudioViewModel
) {
    val brandKitsState by viewModel.brandKitsState.collectAsState()
    var brandName by remember { mutableStateOf("Veritas Documentary Studio") }
    var channelTag by remember { mutableStateOf("VERITAS") }
    var primaryFont by remember { mutableStateOf(StudioFont.BEBAS_NEUE.displayName) }
    var secondaryFont by remember { mutableStateOf(StudioFont.MONTSERRAT.displayName) }
    var primaryColorHex by remember { mutableStateOf("#FFFFFF") }
    var secondaryColorHex by remember { mutableStateOf("#FBBF24") }
    var accentColorHex by remember { mutableStateOf("#EF4444") }
    var defaultStyle by remember { mutableStateOf(VisualStylePreset.CINEMATIC_INVESTIGATIVE.displayName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Channel Brand Kit", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                "Define your channel's signature fonts, colors, badge tag, and visual preset for consistent branding across thumbnails.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Create / Save Brand Kit", style = MaterialTheme.typography.titleLarge, color = Color.White)

                    OutlinedTextField(
                        value = brandName,
                        onValueChange = { brandName = it },
                        label = { Text("Brand / Channel Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = channelTag,
                        onValueChange = { channelTag = it },
                        label = { Text("Default Badge / Tag Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Primary Headline Font:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioFont.entries.forEach { font ->
                            FilterChip(
                                selected = primaryFont == font.displayName,
                                onClick = { primaryFont = font.displayName },
                                label = { Text(font.displayName) }
                            )
                        }
                    }

                    Text("Secondary Subheadline Font:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioFont.entries.forEach { font ->
                            FilterChip(
                                selected = secondaryFont == font.displayName,
                                onClick = { secondaryFont = font.displayName },
                                label = { Text(font.displayName) }
                            )
                        }
                    }

                    Text("Primary Text Color:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    ColorSwatchRow(selectedHex = primaryColorHex, onSelectHex = { primaryColorHex = it })

                    Text("Secondary / Highlight Color:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    ColorSwatchRow(selectedHex = secondaryColorHex, onSelectHex = { secondaryColorHex = it })

                    Text("Badge Accent Color:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    ColorSwatchRow(selectedHex = accentColorHex, onSelectHex = { accentColorHex = it })

                    Button(
                        onClick = {
                            viewModel.saveBrandKitItem(
                                BrandKit(
                                    brandName = brandName,
                                    primaryFont = primaryFont,
                                    secondaryFont = secondaryFont,
                                    primaryColorHex = primaryColorHex,
                                    secondaryColorHex = secondaryColorHex,
                                    accentColorHex = accentColorHex,
                                    defaultStyle = defaultStyle,
                                    channelTag = channelTag
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_brand_kit_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Brand Kit to Cloud Firestore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val kits = (brandKitsState as? DataUiState.Success)?.data ?: emptyList()
        if (kits.isNotEmpty()) {
            item {
                Text("Saved Brand Kits", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            items(kits, key = { it.id }) { kit ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kit.brandName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "${kit.primaryFont} + ${kit.secondaryFont} • Tag: ${kit.channelTag}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.applyBrandKitToEditor(kit)
                                    viewModel.navigateTo(StudioDestination.EDITOR)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black)
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deleteBrandKitItem(kit.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Brand Kit", tint = Color(0xFFFCA5A5))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenerationHistoryScreen(
    viewModel: StudioViewModel
) {
    val historyState by viewModel.historyState.collectAsState()
    val historyItems = (historyState as? DataUiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("AI Generation History", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                "Review past topics, art direction plans, lighting setups, and photographic prompts.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }

        if (historyItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No Generation History Yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "Every thumbnail you generate with the AI Art Director is automatically logged here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(historyItems, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.videoTopic, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    "Headline: ${item.suggestedHeadline} • ${item.visualStyle} (${item.aspectRatio})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteHistoryRecord(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete history item", tint = Color(0xFFFCA5A5))
                            }
                        }

                        if (item.mainSubject.isNotBlank()) {
                            Text("Subject: ${item.mainSubject}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
                        }
                        if (item.lightingSetup.isNotBlank()) {
                            Text("Lighting: ${item.lightingSetup}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF38BDF8))
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateVideoTopic(item.videoTopic)
                                viewModel.updateHeadlineText(item.suggestedHeadline)
                                viewModel.navigateTo(StudioDestination.GENERATOR)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reuse Topic in AI Generator", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSettingsScreen(
    viewModel: StudioViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedRealism by viewModel.selectedRealism.collectAsState()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsState()
    val selectedAiModel by viewModel.selectedAiModel.collectAsState()

    var displayName by remember(currentUser) {
        mutableStateOf(currentUser?.displayName ?: "Studio Creator")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Account & Studio Preferences", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                "Manage your Google profile, default AI Art Director settings, and configurable Gemini image provider.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }

        // User Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF59E0B))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName ?: "Authenticated Creator",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = currentUser?.email ?: "Signed in with Google",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Creator Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Default Image-Generation Model:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AiImageModel.entries.forEach { model ->
                            val isSel = model == selectedAiModel
                            Surface(
                                color = if (isSel) Color(0xFF1E293B) else Color(0xFF0B0F19),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isSel) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectAiModel(model) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(model.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text(model.badge, style = MaterialTheme.typography.labelMedium, color = Color(0xFF38BDF8))
                                    }
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF59E0B))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveUserPreferences(
                                displayName = displayName,
                                defaultCategory = selectedCategory,
                                defaultStyle = selectedStyle,
                                defaultRealism = selectedRealism,
                                defaultAspectRatio = selectedAspectRatio,
                                preferredModel = selectedAiModel
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_preferences_btn")
                    ) {
                        Text("Save Studio Preferences", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sign Out Card
        item {
            OutlinedButton(
                onClick = {
                    signOutUser(
                        context = context,
                        onSignOutComplete = onSignedOut,
                        scope = scope
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFCA5A5))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Google Account", color = Color(0xFFFCA5A5))
            }
        }
    }
}
