package com.example.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CanvasLayer
import com.example.data.LayerType
import com.example.data.ShapeKind
import com.example.data.StudioFont
import com.example.util.ThumbnailCanvasRenderer

enum class EditorToolTab(val label: String) {
    LAYERS("Layers"),
    TYPOGRAPHY("Typography"),
    GRADING("Bg & Grading"),
    ELEMENTS("Subject & Shapes")
}

private val STUDIO_SWATCHES = listOf(
    "#FFFFFF", "#FBBF24", "#F59E0B", "#EF4444", "#38BDF8",
    "#10B981", "#A855F7", "#EC4899", "#0F172A", "#000000"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThumbnailEditorScreen(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val projectTitle by viewModel.projectTitle.collectAsState()
    val backgroundBitmap by viewModel.backgroundBitmap.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val customWidth by viewModel.customWidth.collectAsState()
    val customHeight by viewModel.customHeight.collectAsState()
    val exposure by viewModel.exposure.collectAsState()
    val contrast by viewModel.contrast.collectAsState()
    val saturation by viewModel.saturation.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val vignette by viewModel.vignette.collectAsState()
    val backgroundBlur by viewModel.backgroundBlur.collectAsState()
    val showGrid by viewModel.showRuleOfThirdsGrid.collectAsState()
    val canvasZoom by viewModel.canvasZoom.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val aiEditCommand by viewModel.aiEditCommand.collectAsState()
    val isExecutingAiEdit by viewModel.isExecutingAiEdit.collectAsState()
    val aiEditFeedback by viewModel.aiEditFeedback.collectAsState()

    var activeToolTab by remember { mutableStateOf(EditorToolTab.LAYERS) }

    val bgUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        viewModel.setBackgroundBitmapFromUpload(context, bmp)
                    }
                }
            }
        }
    }

    val subjectUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        viewModel.addSubjectOrOverlayLayer(bmp, "Uploaded Subject", isOverlay = false)
                    }
                }
            }
        }
    }

    // Render live composite bitmap
    val compositeBitmap = remember(
        backgroundBitmap,
        selectedStyle,
        customWidth,
        customHeight,
        exposure,
        contrast,
        saturation,
        temperature,
        vignette,
        backgroundBlur,
        layers,
        showGrid
    ) {
        viewModel.renderCurrentComposite(context, includeGrid = showGrid)
    }

    val aspectRatioFloat = (customWidth.toFloat() / customHeight.toFloat().coerceAtLeast(1f)).coerceIn(0.5f, 2.2f)
    val selectedLayer = layers.find { it.id == selectedLayerId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Studio Toolbar (Undo, Redo, Grid, Zoom, Save, Export)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = projectTitle,
                            onValueChange = viewModel::updateProjectTitle,
                            singleLine = true,
                            label = { Text("Project Title") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_project_title_input")
                        )

                        Button(
                            onClick = { viewModel.saveCurrentProject(context, saveAsTemplate = false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("editor_save_project_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }

                        Button(
                            onClick = { viewModel.exportAndShareHighResThumbnail(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF090D16)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("editor_export_btn")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PNG", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::undo,
                            enabled = canUndo,
                            modifier = Modifier.testTag("editor_undo_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) Color.White else Color(0xFF475569)
                            )
                        }
                        IconButton(
                            onClick = viewModel::redo,
                            enabled = canRedo,
                            modifier = Modifier.testTag("editor_redo_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) Color.White else Color(0xFF475569)
                            )
                        }
                        IconButton(onClick = viewModel::toggleRuleOfThirdsGrid) {
                            Icon(
                                Icons.Default.GridOn,
                                contentDescription = "Toggle Rule of Thirds Grid",
                                tint = if (showGrid) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }
                        IconButton(onClick = { viewModel.adjustZoom(-0.15f) }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                        }
                        Surface(
                            color = Color(0xFF0B0F19),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { viewModel.resetZoom() }
                        ) {
                            Text(
                                text = "${(canvasZoom * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.adjustZoom(0.15f) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                        }
                        IconButton(onClick = viewModel::resetEditorAdjustments) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Grading", tint = Color(0xFF94A3B8))
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveCurrentProject(context, saveAsTemplate = true) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save as Template", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 2. Interactive Multi-Layer Canvas Preview
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF05070B))
                    .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatioFloat)
                            .clip(RoundedCornerShape(10.dp))
                            .pointerInput(selectedLayerId) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val dxPct = (dragAmount.x / w) * 100f
                                    val dyPct = (dragAmount.y / h) * 100f
                                    viewModel.moveSelectedLayerByDelta(dxPct, dyPct)
                                }
                            }
                            .testTag("interactive_thumbnail_canvas")
                    ) {
                        Image(
                            bitmap = compositeBitmap.asImageBitmap(),
                            contentDescription = "Live Thumbnail Canvas",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = canvasZoom,
                                    scaleY = canvasZoom
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tip: Drag directly on the canvas to position the selected layer (${selectedLayer?.name ?: "None"}) • ${customWidth}×${customHeight} px",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // 3. AI-Powered Editing Assistant Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F33)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Editing Assistant — Describe the changes you want",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiEditCommand,
                            onValueChange = viewModel::updateAiEditCommand,
                            placeholder = {
                                Text("e.g., Make the background darker and increase headline size")
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0B0F19),
                                unfocusedContainerColor = Color(0xFF0B0F19),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_editor_command_input")
                        )

                        Button(
                            onClick = { viewModel.executeAiEdit(context) },
                            enabled = !isExecutingAiEdit && aiEditCommand.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("ai_editor_apply_btn")
                        ) {
                            if (isExecutingAiEdit) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Quick AI Edit Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickCommands = listOf(
                            "Make the background darker",
                            "Make the headline larger",
                            "Change color grading to muted cinematic tones",
                            "Move the headline to the right side",
                            "Improve contrast between subject and background",
                            "Replace the background with a research laboratory"
                        )
                        quickCommands.forEach { cmd ->
                            Surface(
                                color = Color(0xFF0B0F19),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateAiEditCommand(cmd) }
                            ) {
                                Text(
                                    text = cmd,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (aiEditFeedback != null) {
                        Text(
                            text = aiEditFeedback ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }

        // 4. Editor Tool Category Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorToolTab.entries.forEach { tab ->
                    val selected = tab == activeToolTab
                    val icon = when (tab) {
                        EditorToolTab.LAYERS -> Icons.Default.Layers
                        EditorToolTab.TYPOGRAPHY -> Icons.Default.TextFields
                        EditorToolTab.GRADING -> Icons.Default.Tune
                        EditorToolTab.ELEMENTS -> Icons.Default.Palette
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { activeToolTab = tab },
                        label = { Text(tab.label) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF111827),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Active Editor Tool Panel
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (activeToolTab) {
                        EditorToolTab.LAYERS -> {
                            LayersToolPanel(
                                layers = layers,
                                selectedLayerId = selectedLayerId,
                                onSelectLayer = viewModel::selectLayer,
                                onAddHeadline = { viewModel.addTextLayer("NEW HEADLINE", false) },
                                onAddSubheadline = { viewModel.addTextLayer("SUBHEADLINE", true) },
                                onAddShape = { viewModel.addShapeLayer(it) },
                                onUploadSubject = {
                                    subjectUploadLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onToggleVisible = viewModel::toggleLayerVisibility,
                                onToggleLock = viewModel::toggleLayerLock,
                                onBringForward = viewModel::bringLayerForward,
                                onSendBackward = viewModel::sendLayerBackward,
                                onDuplicate = viewModel::duplicateLayer,
                                onDelete = viewModel::deleteLayer
                            )
                        }

                        EditorToolTab.TYPOGRAPHY -> {
                            TypographyToolPanel(
                                selectedLayer = selectedLayer,
                                onUpdateLayer = viewModel::updateLayer,
                                onAddHeadline = { viewModel.addTextLayer("HEADLINE", false) }
                            )
                        }

                        EditorToolTab.GRADING -> {
                            ColorGradingToolPanel(
                                exposure = exposure,
                                contrast = contrast,
                                saturation = saturation,
                                temperature = temperature,
                                vignette = vignette,
                                backgroundBlur = backgroundBlur,
                                onUpdateGrading = viewModel::updateColorGrading,
                                onUploadBackground = {
                                    bgUploadLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRegenerateWithAi = {
                                    viewModel.navigateTo(StudioDestination.GENERATOR)
                                }
                            )
                        }

                        EditorToolTab.ELEMENTS -> {
                            ElementsAndTransformPanel(
                                selectedLayer = selectedLayer,
                                onUpdateLayer = viewModel::updateLayer,
                                onAddShape = { kind, hex -> viewModel.addShapeLayer(kind, hex) },
                                onUploadOverlay = {
                                    subjectUploadLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayersToolPanel(
    layers: List<CanvasLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onAddHeadline: () -> Unit,
    onAddSubheadline: () -> Unit,
    onAddShape: (ShapeKind) -> Unit,
    onUploadSubject: () -> Unit,
    onToggleVisible: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onBringForward: (String) -> Unit,
    onSendBackward: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Text("Layer Stack & Quick Add", style = MaterialTheme.typography.titleLarge, color = Color.White)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onAddHeadline) { Text("+ Headline", color = Color.White) }
        OutlinedButton(onClick = onAddSubheadline) { Text("+ Subheadline", color = Color.White) }
        OutlinedButton(onClick = onUploadSubject) { Text("+ Subject/PNG", color = Color(0xFF38BDF8)) }
        OutlinedButton(onClick = { onAddShape(ShapeKind.ARROW) }) { Text("+ Arrow", color = Color(0xFFFBBF24)) }
        OutlinedButton(onClick = { onAddShape(ShapeKind.RECTANGLE) }) { Text("+ Highlight Box", color = Color(0xFFEF4444)) }
        OutlinedButton(onClick = { onAddShape(ShapeKind.BADGE) }) { Text("+ Badge Pill", color = Color(0xFF10B981)) }
    }

    if (layers.isEmpty()) {
        Text(
            text = "No overlay layers active. Use the buttons above to add text, shapes, or subject images.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            layers.sortedByDescending { it.zIndex }.forEach { layer ->
                val isSelected = layer.id == selectedLayerId
                Surface(
                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0B0F19),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectLayer(layer.id) }
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layer.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "${layer.type.label} • Z:${layer.zIndex} • Pos:(${layer.xPercent.toInt()}%, ${layer.yPercent.toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onBringForward(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Forward", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onSendBackward(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Backward", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onToggleVisible(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = if (layer.visible) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onToggleLock(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Toggle Lock",
                                    tint = if (layer.locked) Color(0xFFF59E0B) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDuplicate(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Layer", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDelete(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Layer", tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypographyToolPanel(
    selectedLayer: CanvasLayer?,
    onUpdateLayer: (CanvasLayer) -> Unit,
    onAddHeadline: () -> Unit
) {
    if (selectedLayer == null || selectedLayer.type != LayerType.TEXT) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Select a Text Layer from the Layers tab or add a new headline to customize typography.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
            Button(
                onClick = onAddHeadline,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
            ) {
                Text("Add Headline Layer", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Text("Smart Typography Engine — ${selectedLayer.name}", style = MaterialTheme.typography.titleLarge, color = Color.White)

    OutlinedTextField(
        value = selectedLayer.text,
        onValueChange = { onUpdateLayer(selectedLayer.copy(text = it)) },
        label = { Text("Headline Text (Supports multi-line)") },
        minLines = 2,
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFF59E0B),
            unfocusedBorderColor = Color(0xFF1E293B),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Text("Font Family (Bundled Studio Fonts):", style = MaterialTheme.typography.labelLarge, color = Color.White)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudioFont.entries.forEach { font ->
            FilterChip(
                selected = selectedLayer.fontName == font.displayName,
                onClick = { onUpdateLayer(selectedLayer.copy(fontName = font.displayName)) },
                label = { Text(font.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF59E0B),
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF0B0F19),
                    labelColor = Color.White
                )
            )
        }
    }

    LabeledSlider(
        label = "Font Size",
        value = selectedLayer.fontSizeSp,
        range = 14f..130f,
        unit = "sp",
        onValueChange = { onUpdateLayer(selectedLayer.copy(fontSizeSp = it)) }
    )

    LabeledSlider(
        label = "Outline Stroke Width",
        value = selectedLayer.outlineWidth,
        range = 0f..14f,
        unit = "px",
        onValueChange = { onUpdateLayer(selectedLayer.copy(outlineWidth = it)) }
    )

    LabeledSlider(
        label = "Drop Shadow Radius",
        value = selectedLayer.shadowRadius,
        range = 0f..32f,
        unit = "px",
        onValueChange = { onUpdateLayer(selectedLayer.copy(shadowRadius = it)) }
    )

    LabeledSlider(
        label = "Letter Spacing",
        value = selectedLayer.letterSpacing,
        range = -1f..10f,
        unit = "",
        onValueChange = { onUpdateLayer(selectedLayer.copy(letterSpacing = it)) }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Uppercase Formatting", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Switch(
            checked = selectedLayer.uppercase,
            onCheckedChange = { onUpdateLayer(selectedLayer.copy(uppercase = it)) }
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("LEFT", "CENTER", "RIGHT").forEach { align ->
            FilterChip(
                selected = selectedLayer.textAlign == align,
                onClick = { onUpdateLayer(selectedLayer.copy(textAlign = align)) },
                label = { Text("Align $align") }
            )
        }
    }

    Text("Text Fill Color:", style = MaterialTheme.typography.labelLarge, color = Color.White)
    ColorSwatchRow(
        selectedHex = selectedLayer.textColorHex,
        onSelectHex = { onUpdateLayer(selectedLayer.copy(textColorHex = it)) }
    )

    Text("Background Pill / Label Color (Tap active to clear):", style = MaterialTheme.typography.labelLarge, color = Color.White)
    ColorSwatchRow(
        selectedHex = selectedLayer.backgroundBadgeHex,
        onSelectHex = { hex ->
            val next = if (selectedLayer.backgroundBadgeHex.equals(hex, ignoreCase = true)) "" else hex
            onUpdateLayer(selectedLayer.copy(backgroundBadgeHex = next))
        }
    )
}

@Composable
private fun ColorGradingToolPanel(
    exposure: Int,
    contrast: Int,
    saturation: Int,
    temperature: Int,
    vignette: Int,
    backgroundBlur: Int,
    onUpdateGrading: (Int, Int, Int, Int, Int, Int) -> Unit,
    onUploadBackground: () -> Unit,
    onRegenerateWithAi: () -> Unit
) {
    Text("Background & Cinema Color Grading", style = MaterialTheme.typography.titleLarge, color = Color.White)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onUploadBackground,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Upload Bg", color = Color.White)
        }
        Button(
            onClick = onRegenerateWithAi,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate AI Bg", fontWeight = FontWeight.Bold)
        }
    }

    LabeledSlider(
        label = "Exposure / Brightness",
        value = exposure.toFloat(),
        range = -80f..80f,
        onValueChange = { onUpdateGrading(it.toInt(), contrast, saturation, temperature, vignette, backgroundBlur) }
    )
    LabeledSlider(
        label = "Contrast",
        value = contrast.toFloat(),
        range = -80f..80f,
        onValueChange = { onUpdateGrading(exposure, it.toInt(), saturation, temperature, vignette, backgroundBlur) }
    )
    LabeledSlider(
        label = "Saturation",
        value = saturation.toFloat(),
        range = -100f..100f,
        onValueChange = { onUpdateGrading(exposure, contrast, it.toInt(), temperature, vignette, backgroundBlur) }
    )
    LabeledSlider(
        label = "Color Temperature (Cool ↔ Warm)",
        value = temperature.toFloat(),
        range = -80f..80f,
        onValueChange = { onUpdateGrading(exposure, contrast, saturation, it.toInt(), vignette, backgroundBlur) }
    )
    LabeledSlider(
        label = "Cinema Vignette",
        value = vignette.toFloat(),
        range = 0f..100f,
        unit = "%",
        onValueChange = { onUpdateGrading(exposure, contrast, saturation, temperature, it.toInt(), backgroundBlur) }
    )
    LabeledSlider(
        label = "Background Depth Blur",
        value = backgroundBlur.toFloat(),
        range = 0f..80f,
        unit = "%",
        onValueChange = { onUpdateGrading(exposure, contrast, saturation, temperature, vignette, it.toInt()) }
    )
}

@Composable
private fun ElementsAndTransformPanel(
    selectedLayer: CanvasLayer?,
    onUpdateLayer: (CanvasLayer) -> Unit,
    onAddShape: (ShapeKind, String) -> Unit,
    onUploadOverlay: () -> Unit
) {
    Text("Subject, Shapes & Layer Transform", style = MaterialTheme.typography.titleLarge, color = Color.White)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onUploadOverlay) {
            Text("Upload Subject / PNG", color = Color(0xFF38BDF8))
        }
        ShapeKind.entries.forEach { kind ->
            OutlinedButton(onClick = { onAddShape(kind, "#EF4444") }) {
                Text("+ ${kind.label}", color = Color.White)
            }
        }
    }

    if (selectedLayer != null) {
        Text(
            text = "Transform Selected Layer: ${selectedLayer.name}",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFBBF24)
        )
        LabeledSlider(
            label = "Horizontal Position (X)",
            value = selectedLayer.xPercent,
            range = -10f..95f,
            unit = "%",
            onValueChange = { onUpdateLayer(selectedLayer.copy(xPercent = it)) }
        )
        LabeledSlider(
            label = "Vertical Position (Y)",
            value = selectedLayer.yPercent,
            range = -10f..95f,
            unit = "%",
            onValueChange = { onUpdateLayer(selectedLayer.copy(yPercent = it)) }
        )
        LabeledSlider(
            label = "Layer Width",
            value = selectedLayer.widthPercent,
            range = 5f..100f,
            unit = "%",
            onValueChange = { onUpdateLayer(selectedLayer.copy(widthPercent = it)) }
        )
        LabeledSlider(
            label = "Layer Height",
            value = selectedLayer.heightPercent,
            range = 4f..100f,
            unit = "%",
            onValueChange = { onUpdateLayer(selectedLayer.copy(heightPercent = it)) }
        )
        LabeledSlider(
            label = "Rotation Angle",
            value = selectedLayer.rotation,
            range = -45f..45f,
            unit = "°",
            onValueChange = { onUpdateLayer(selectedLayer.copy(rotation = it)) }
        )
        LabeledSlider(
            label = "Layer Opacity",
            value = selectedLayer.opacity * 100f,
            range = 10f..100f,
            unit = "%",
            onValueChange = { onUpdateLayer(selectedLayer.copy(opacity = it / 100f)) }
        )

        if (selectedLayer.type == LayerType.SHAPE) {
            Text("Shape Color:", style = MaterialTheme.typography.labelLarge, color = Color.White)
            ColorSwatchRow(
                selectedHex = selectedLayer.fillColorHex,
                onSelectHex = { onUpdateLayer(selectedLayer.copy(fillColorHex = it)) }
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color(0xFFCBD5E1))
            Text(text = "${value.toInt()}$unit", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFBBF24))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFF59E0B),
                activeTrackColor = Color(0xFFF59E0B),
                inactiveTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun ColorSwatchRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        STUDIO_SWATCHES.forEach { hex ->
            val color = Color(ThumbnailCanvasRenderer.parseColorSafe(hex))
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569),
                        shape = CircleShape
                    )
                    .clickable { onSelectHex(hex) }
            )
        }
    }
}
