package com.example.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AiImageModel
import com.example.data.ArtDirectionPlan
import com.example.data.CompositionPreset
import com.example.data.ImageRealismMode
import com.example.data.ReferenceUsageOption
import com.example.data.ThumbnailAspectRatio
import com.example.data.VideoCategory
import com.example.data.VisualStylePreset
import com.example.util.ThumbnailCanvasRenderer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiGeneratorScreen(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val videoTopic by viewModel.videoTopic.collectAsState()
    val headlineText by viewModel.headlineText.collectAsState()
    val subheadlineText by viewModel.subheadlineText.collectAsState()
    val includeTextOverlay by viewModel.includeTextOverlay.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedRealism by viewModel.selectedRealism.collectAsState()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsState()
    val customWidth by viewModel.customWidth.collectAsState()
    val customHeight by viewModel.customHeight.collectAsState()
    val selectedComposition by viewModel.selectedComposition.collectAsState()
    val referenceImages by viewModel.referenceImages.collectAsState()
    val additionalInstructions by viewModel.additionalInstructions.collectAsState()
    val selectedAiModel by viewModel.selectedAiModel.collectAsState()
    val suggestedHeadlines by viewModel.suggestedHeadlines.collectAsState()
    val isSuggestingHeadlines by viewModel.isSuggestingHeadlines.collectAsState()
    val artDirectionPlan by viewModel.artDirectionPlan.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgressStage by viewModel.generationProgressStage.collectAsState()
    val generationError by viewModel.generationError.collectAsState()

    // Zero-permission Android Photo Picker for Reference Images
    val referencePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        viewModel.addReferenceImage(
                            bitmap = bmp,
                            label = "Ref #${referenceImages.size + 1}",
                            usage = ReferenceUsageOption.MAIN_SUBJECT
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore failed decode
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFF59E0B),
        unfocusedBorderColor = Color(0xFF1E293B),
        focusedContainerColor = Color(0xFF111827),
        unfocusedContainerColor = Color(0xFF111827),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = Color(0xFFFBBF24),
        unfocusedLabelColor = Color(0xFF94A3B8)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "AI Thumbnail Generator & Art Director",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Describe your video topic. Our AI Art Director designs the subject, lighting, negative space, and photorealistic composition.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Error Banner (Transparent Error Propagation)
        if (generationError != null) {
            item {
                Surface(
                    color = Color(0xFF450A0A),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generationError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearGenerationError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss error", tint = Color(0xFFFCA5A5))
                        }
                    }
                }
            }
        }

        // 1. Video Topic Input
        item {
            SectionCard(title = "1. Video Topic", subtitle = "What is your video about?") {
                OutlinedTextField(
                    value = videoTopic,
                    onValueChange = viewModel::updateVideoTopic,
                    label = { Text("What is your video about?") },
                    placeholder = { Text("e.g., The Dark Truth Behind India's Coaching Industry") },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_topic_input")
                )
            }
        }

        // 2. Thumbnail Headline & Smart Typography
        item {
            SectionCard(
                title = "2. Thumbnail Headline",
                subtitle = "Rendered crisply via the Smart Typography Engine (never baked with AI spelling flaws)"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Include Editable Headline Overlay",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Switch(
                        checked = includeTextOverlay,
                        onCheckedChange = viewModel::updateIncludeTextOverlay,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B))
                    )
                }

                AnimatedVisibility(visible = includeTextOverlay) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = headlineText,
                            onValueChange = viewModel::updateHeadlineText,
                            label = { Text("Thumbnail Text (Headline)") },
                            placeholder = { Text("e.g., EDUCATION SCAM") },
                            colors = fieldColors,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("thumbnail_headline_input")
                        )

                        OutlinedTextField(
                            value = subheadlineText,
                            onValueChange = viewModel::updateSubheadlineText,
                            label = { Text("Optional Subheadline") },
                            placeholder = { Text("e.g., THE BILLION DOLLAR FACTORY") },
                            colors = fieldColors,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = viewModel::requestAiHeadlines,
                            enabled = !isSuggestingHeadlines,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggest_headline_ai_btn")
                        ) {
                            if (isSuggestingHeadlines) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFFFBBF24),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Concise Headlines...", color = Color(0xFFFBBF24))
                            } else {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Suggest Headline with AI", color = Color(0xFFFBBF24))
                            }
                        }

                        if (suggestedHeadlines.isNotEmpty()) {
                            Text(
                                text = "Tap a recommendation to apply:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestedHeadlines.forEach { suggestion ->
                                    FilterChip(
                                        selected = headlineText.equals(suggestion, ignoreCase = true),
                                        onClick = { viewModel.updateHeadlineText(suggestion) },
                                        label = { Text(suggestion) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFF59E0B),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Video Category (21 Categories)
        item {
            SectionCard(
                title = "3. Video Category",
                subtitle = "Influences camera lens, lighting atmosphere, and subject tone"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category.displayName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }
                Text(
                    text = "Direction Hint: ${selectedCategory.artDirectionHint}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF38BDF8)
                )
            }
        }

        // 4. Visual Style Presets (18 Presets)
        item {
            SectionCard(
                title = "4. Visual Style Preset",
                subtitle = "Choose from 18 art-directed photographic presets"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VisualStylePreset.entries.forEach { style ->
                        val isSelected = style == selectedStyle
                        val c1 = Color(ThumbnailCanvasRenderer.parseColorSafe(style.primaryColorHex))
                        val c2 = Color(ThumbnailCanvasRenderer.parseColorSafe(style.secondaryColorHex))
                        val acc = Color(ThumbnailCanvasRenderer.parseColorSafe(style.accentColorHex))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(210.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectStyle(style) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(74.dp)
                                        .background(Brush.linearGradient(listOf(c1, c2)))
                                        .padding(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(acc)
                                            .align(Alignment.TopEnd)
                                    )
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    )
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = style.shortDescription,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Image Realism & Aspect Ratio
        item {
            SectionCard(
                title = "5. Image Realism & Output Aspect Ratio",
                subtitle = "Default: Photorealistic • 16:9 YouTube Thumbnail (1280×720)"
            ) {
                Text("Realism Control:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageRealismMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == selectedRealism,
                            onClick = { viewModel.selectRealism(mode) },
                            label = { Text(mode.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Output Format & Dimensions:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThumbnailAspectRatio.entries.forEach { ratio ->
                        FilterChip(
                            selected = ratio == selectedAspectRatio,
                            onClick = { viewModel.selectAspectRatio(ratio) },
                            label = {
                                Text("${ratio.displayName} (${ratio.ratioLabel})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Text(
                    text = "Active Canvas Resolution: ${customWidth} × ${customHeight} px",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFBBF24)
                )
            }
        }

        // 6. Professional Thumbnail Composition Preset (12 Presets)
        item {
            SectionCard(
                title = "6. Thumbnail Composition Layout",
                subtitle = "Controls subject placement, negative space, and visual hierarchy"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompositionPreset.entries.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                val isSel = preset == selectedComposition
                                Surface(
                                    color = if (isSel) Color(0xFF1E293B) else Color(0xFF0B0F19),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (isSel) 1.5.dp else 1.dp,
                                            color = if (isSel) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.selectComposition(preset) }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSel) Color(0xFFFBBF24) else Color.White,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Reference Images Uploader
        item {
            SectionCard(
                title = "7. Visual Reference Images (Optional)",
                subtitle = "Upload subject portraits, backgrounds, or style references and choose how AI interprets each"
            ) {
                OutlinedButton(
                    onClick = {
                        referencePhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_reference_image_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Reference Image", color = Color.White)
                }

                if (referenceImages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        referenceImages.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            bitmap = item.bitmap.asImageBitmap(),
                                            contentDescription = item.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                            Text(
                                                item.usageOption.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFFBBF24)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.removeReferenceImage(item.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove reference", tint = Color(0xFFFCA5A5))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ReferenceUsageOption.entries.forEach { usage ->
                                            FilterChip(
                                                selected = item.usageOption == usage,
                                                onClick = { viewModel.updateReferenceUsage(item.id, usage) },
                                                label = { Text(usage.displayName) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Additional Instructions & Configurable AI Model
        item {
            SectionCard(
                title = "8. Additional Art Direction & AI Model",
                subtitle = "Describe specific details and choose your Gemini Image generation model"
            ) {
                OutlinedTextField(
                    value = additionalInstructions,
                    onValueChange = viewModel::updateAdditionalInstructions,
                    label = { Text("Describe exactly how you want your thumbnail to look") },
                    minLines = 3,
                    maxLines = 6,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("additional_instructions_input")
                )

                Text(
                    text = "Configurable Image-Generation Model:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiImageModel.entries.forEach { model ->
                        FilterChip(
                            selected = model == selectedAiModel,
                            onClick = { viewModel.selectAiModel(model) },
                            label = { Text("${model.displayName} (${model.badge})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6366F1),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }
        }

        // 9. AI Art Direction Plan Preview & Generate CTA
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isGenerating && generationProgressStage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = generationProgressStage ?: "Generating...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Live Firebase AI Logic execution — waiting for authentic model result",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::runArtDirectionAnalysisOnly,
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("preview_art_direction_btn")
                    ) {
                        Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Art Plan", color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.generateFullThumbnail(context) },
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF090D16)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(54.dp)
                            .testTag("generate_thumbnail_cta_btn")
                    ) {
                        Icon(Icons.Default.MovieCreation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Thumbnail",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 10. Structured AI Art Direction Blueprint Display (When Available)
        if (artDirectionPlan != null) {
            item {
                ArtDirectionBlueprintCard(plan = artDirectionPlan!!)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
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
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
            }
            content()
        }
    }
}

@Composable
private fun ArtDirectionBlueprintCard(plan: ArtDirectionPlan) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161F33)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Art Direction Plan (16-Point Studio Blueprint)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            PlanFieldRow("Main Subject", plan.mainSubject)
            PlanFieldRow("Supporting Subjects", plan.supportingSubjects)
            PlanFieldRow("Background Environment", plan.backgroundEnvironment)
            PlanFieldRow("Foreground Elements", plan.foregroundElements)
            PlanFieldRow("Camera Angle & Shot", "${plan.cameraAngle} • ${plan.shotType}")
            PlanFieldRow("Lighting Setup", plan.lightingSetup)
            PlanFieldRow("Color Palette", plan.colorPalette)
            PlanFieldRow("Mood & Emotional Tone", "${plan.mood} • ${plan.emotionalTone}")
            PlanFieldRow("Composition & Hierarchy", "${plan.composition} • ${plan.visualHierarchy}")
            PlanFieldRow("Text Placement & Negative Space", "${plan.textPlacement} • ${plan.negativeSpace}")
            PlanFieldRow("Depth of Field", plan.depthOfField)
            PlanFieldRow("Realism Requirements", plan.realismRequirements)
            PlanFieldRow("Things to Avoid", plan.thingsToAvoid)
        }
    }
}

@Composable
private fun PlanFieldRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B0F19))
            .padding(10.dp)
    ) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE2E8F0))
    }
}
