package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.ThumbnailAiDirectorService
import com.example.data.AiImageModel
import com.example.data.ArtDirectionPlan
import com.example.data.BrandKit
import com.example.data.CanvasLayer
import com.example.data.CompositionPreset
import com.example.data.GenerationHistoryItem
import com.example.data.ImageRealismMode
import com.example.data.LayerType
import com.example.data.ReferenceImageItem
import com.example.data.ReferenceUsageOption
import com.example.data.ShapeKind
import com.example.data.StudioFont
import com.example.data.StudioRepository
import com.example.data.ThumbnailAspectRatio
import com.example.data.ThumbnailProject
import com.example.data.UserProfile
import com.example.data.VideoCategory
import com.example.data.VisualStylePreset
import com.example.util.ThumbnailCanvasRenderer
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "StudioViewModel"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

enum class StudioDestination(val label: String) {
    HOME("Dashboard"),
    GENERATOR("AI Generator"),
    EDITOR("Studio Editor"),
    STYLES("Templates & Styles"),
    BRAND_KIT("Brand Kit"),
    HISTORY("History"),
    SETTINGS("Settings")
}

data class EditorSnapshot(
    val backgroundBitmap: Bitmap?,
    val exposure: Int,
    val contrast: Int,
    val saturation: Int,
    val temperature: Int,
    val vignette: Int,
    val backgroundBlur: Int,
    val layers: List<CanvasLayer>
)

class StudioViewModel(
    private val repository: StudioRepository,
    private val aiService: ThumbnailAiDirectorService,
    private val currentUserId: String
) : ViewModel() {

    // Navigation & Search
    private val _currentDestination = MutableStateFlow(StudioDestination.HOME)
    val currentDestination: StateFlow<StudioDestination> = _currentDestination.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Firestore Real-time Streams (Two-tier error handling)
    val projectsState: StateFlow<DataUiState<List<ThumbnailProject>>> = repository.observeProjects(currentUserId)
        .map<List<ThumbnailProject>, DataUiState<List<ThumbnailProject>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing projects", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load projects"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val historyState: StateFlow<DataUiState<List<GenerationHistoryItem>>> = repository.observeHistory(currentUserId)
        .map<List<GenerationHistoryItem>, DataUiState<List<GenerationHistoryItem>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing history", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val brandKitsState: StateFlow<DataUiState<List<BrandKit>>> = repository.observeBrandKits(currentUserId)
        .map<List<BrandKit>, DataUiState<List<BrandKit>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing brand kits", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load brand kits"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val userProfileState: StateFlow<DataUiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, DataUiState<UserProfile?>> { DataUiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing user profile", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    // --- AI Generator Inputs ---
    private val _videoTopic = MutableStateFlow("The Dark Truth Behind India's Coaching Industry")
    val videoTopic: StateFlow<String> = _videoTopic.asStateFlow()

    private val _headlineText = MutableStateFlow("EDUCATION SCAM")
    val headlineText: StateFlow<String> = _headlineText.asStateFlow()

    private val _subheadlineText = MutableStateFlow("THE BILLION DOLLAR PRESSURE COOKER")
    val subheadlineText: StateFlow<String> = _subheadlineText.asStateFlow()

    private val _includeTextOverlay = MutableStateFlow(true)
    val includeTextOverlay: StateFlow<Boolean> = _includeTextOverlay.asStateFlow()

    private val _selectedCategory = MutableStateFlow(VideoCategory.DOCUMENTARY)
    val selectedCategory: StateFlow<VideoCategory> = _selectedCategory.asStateFlow()

    private val _selectedStyle = MutableStateFlow(VisualStylePreset.CINEMATIC_INVESTIGATIVE)
    val selectedStyle: StateFlow<VisualStylePreset> = _selectedStyle.asStateFlow()

    private val _selectedRealism = MutableStateFlow(ImageRealismMode.PHOTOREALISTIC)
    val selectedRealism: StateFlow<ImageRealismMode> = _selectedRealism.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(ThumbnailAspectRatio.YOUTUBE_16_9)
    val selectedAspectRatio: StateFlow<ThumbnailAspectRatio> = _selectedAspectRatio.asStateFlow()

    private val _customWidth = MutableStateFlow(1280)
    val customWidth: StateFlow<Int> = _customWidth.asStateFlow()

    private val _customHeight = MutableStateFlow(720)
    val customHeight: StateFlow<Int> = _customHeight.asStateFlow()

    private val _selectedComposition = MutableStateFlow(CompositionPreset.SUBJECT_RIGHT_HEADLINE_LEFT)
    val selectedComposition: StateFlow<CompositionPreset> = _selectedComposition.asStateFlow()

    private val _referenceImages = MutableStateFlow<List<ReferenceImageItem>>(emptyList())
    val referenceImages: StateFlow<List<ReferenceImageItem>> = _referenceImages.asStateFlow()

    private val _additionalInstructions = MutableStateFlow(
        "Place a serious-looking student on the right third, a dimly lit coaching classroom in the background, subtle cool blue and warm amber directional lighting, realistic skin texture, and clean dark negative space on the left for the headline."
    )
    val additionalInstructions: StateFlow<String> = _additionalInstructions.asStateFlow()

    private val _selectedAiModel = MutableStateFlow(AiImageModel.NANO_BANANA_2_1)
    val selectedAiModel: StateFlow<AiImageModel> = _selectedAiModel.asStateFlow()

    private val _suggestedHeadlines = MutableStateFlow<List<String>>(
        listOf("EDUCATION SCAM", "THE COACHING TRAP", "SOLD A DREAM", "INSIDE KOTA'S FACTORY")
    )
    val suggestedHeadlines: StateFlow<List<String>> = _suggestedHeadlines.asStateFlow()

    private val _isSuggestingHeadlines = MutableStateFlow(false)
    val isSuggestingHeadlines: StateFlow<Boolean> = _isSuggestingHeadlines.asStateFlow()

    private val _artDirectionPlan = MutableStateFlow<ArtDirectionPlan?>(null)
    val artDirectionPlan: StateFlow<ArtDirectionPlan?> = _artDirectionPlan.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgressStage = MutableStateFlow<String?>(null)
    val generationProgressStage: StateFlow<String?> = _generationProgressStage.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // --- Professional Thumbnail Editor State ---
    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _projectTitle = MutableStateFlow("Coaching Industry Documentary")
    val projectTitle: StateFlow<String> = _projectTitle.asStateFlow()

    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap.asStateFlow()

    private val _localImagePath = MutableStateFlow("")
    val localImagePath: StateFlow<String> = _localImagePath.asStateFlow()

    private val _exposure = MutableStateFlow(0)
    val exposure: StateFlow<Int> = _exposure.asStateFlow()

    private val _contrast = MutableStateFlow(12)
    val contrast: StateFlow<Int> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(-5)
    val saturation: StateFlow<Int> = _saturation.asStateFlow()

    private val _temperature = MutableStateFlow(-4)
    val temperature: StateFlow<Int> = _temperature.asStateFlow()

    private val _vignette = MutableStateFlow(35)
    val vignette: StateFlow<Int> = _vignette.asStateFlow()

    private val _backgroundBlur = MutableStateFlow(0)
    val backgroundBlur: StateFlow<Int> = _backgroundBlur.asStateFlow()

    private val _showRuleOfThirdsGrid = MutableStateFlow(false)
    val showRuleOfThirdsGrid: StateFlow<Boolean> = _showRuleOfThirdsGrid.asStateFlow()

    private val _canvasZoom = MutableStateFlow(1f)
    val canvasZoom: StateFlow<Float> = _canvasZoom.asStateFlow()

    private val _layers = MutableStateFlow<List<CanvasLayer>>(defaultInitialLayers())
    val layers: StateFlow<List<CanvasLayer>> = _layers.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(_layers.value.firstOrNull()?.id)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // AI Editing Assistant State
    private val _aiEditCommand = MutableStateFlow("")
    val aiEditCommand: StateFlow<String> = _aiEditCommand.asStateFlow()

    private val _isExecutingAiEdit = MutableStateFlow(false)
    val isExecutingAiEdit: StateFlow<Boolean> = _isExecutingAiEdit.asStateFlow()

    private val _aiEditFeedback = MutableStateFlow<String?>(null)
    val aiEditFeedback: StateFlow<String?> = _aiEditFeedback.asStateFlow()

    private fun defaultInitialLayers(): List<CanvasLayer> {
        return listOf(
            CanvasLayer(
                id = "layer_badge",
                name = "Category Badge",
                type = LayerType.TEXT,
                xPercent = 7f,
                yPercent = 14f,
                widthPercent = 36f,
                heightPercent = 10f,
                zIndex = 1,
                text = "INVESTIGATION",
                fontName = StudioFont.MONTSERRAT.displayName,
                fontSizeSp = 20f,
                fontWeightBold = true,
                textColorHex = "#FFFFFF",
                outlineColorHex = "#000000",
                outlineWidth = 0f,
                shadowRadius = 6f,
                backgroundBadgeHex = "#EF4444",
                uppercase = true,
                letterSpacing = 3f
            ),
            CanvasLayer(
                id = "layer_headline",
                name = "Main Headline",
                type = LayerType.TEXT,
                xPercent = 7f,
                yPercent = 26f,
                widthPercent = 56f,
                heightPercent = 34f,
                zIndex = 2,
                text = "EDUCATION\nSCAM",
                fontName = StudioFont.BEBAS_NEUE.displayName,
                fontSizeSp = 74f,
                fontWeightBold = true,
                textColorHex = "#FFFFFF",
                outlineColorHex = "#000000",
                outlineWidth = 5f,
                shadowRadius = 16f,
                backgroundBadgeHex = "",
                uppercase = true,
                letterSpacing = 1.5f,
                lineHeightMultiplier = 0.92f
            ),
            CanvasLayer(
                id = "layer_subheadline",
                name = "Subheadline",
                type = LayerType.TEXT,
                xPercent = 7f,
                yPercent = 64f,
                widthPercent = 52f,
                heightPercent = 14f,
                zIndex = 3,
                text = "THE BILLION DOLLAR TRAP",
                fontName = StudioFont.SPACE_GROTESK.displayName,
                fontSizeSp = 24f,
                fontWeightBold = true,
                textColorHex = "#FBBF24",
                outlineColorHex = "#000000",
                outlineWidth = 3f,
                shadowRadius = 10f,
                backgroundBadgeHex = "",
                uppercase = true,
                letterSpacing = 1.2f
            )
        )
    }

    // Navigation & Basic Setters
    fun navigateTo(destination: StudioDestination) {
        _currentDestination.value = destination
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearGenerationError() {
        _generationError.value = null
    }

    fun updateVideoTopic(topic: String) {
        _videoTopic.value = topic
    }

    fun updateHeadlineText(text: String) {
        _headlineText.value = text
    }

    fun updateSubheadlineText(text: String) {
        _subheadlineText.value = text
    }

    fun updateIncludeTextOverlay(enabled: Boolean) {
        _includeTextOverlay.value = enabled
    }

    fun selectCategory(category: VideoCategory) {
        _selectedCategory.value = category
    }

    fun selectStyle(style: VisualStylePreset) {
        _selectedStyle.value = style
    }

    fun selectRealism(realism: ImageRealismMode) {
        _selectedRealism.value = realism
    }

    fun selectAspectRatio(ratio: ThumbnailAspectRatio) {
        _selectedAspectRatio.value = ratio
        if (ratio != ThumbnailAspectRatio.CUSTOM) {
            _customWidth.value = ratio.defaultWidth
            _customHeight.value = ratio.defaultHeight
        }
    }

    fun updateCustomDimensions(width: Int, height: Int) {
        _customWidth.value = width.coerceIn(320, 4096)
        _customHeight.value = height.coerceIn(320, 4096)
    }

    fun selectComposition(preset: CompositionPreset) {
        _selectedComposition.value = preset
        // Automatically adjust headline layer placement to match composition's negative space
        pushUndoState()
        _layers.value = _layers.value.map { layer ->
            if (layer.id == "layer_headline") {
                layer.copy(
                    xPercent = preset.defaultHeadlineX.toFloat(),
                    yPercent = preset.defaultHeadlineY.toFloat()
                )
            } else if (layer.id == "layer_badge") {
                layer.copy(
                    xPercent = preset.defaultHeadlineX.toFloat(),
                    yPercent = (preset.defaultHeadlineY - 11).coerceAtLeast(4).toFloat()
                )
            } else if (layer.id == "layer_subheadline") {
                layer.copy(
                    xPercent = preset.defaultHeadlineX.toFloat(),
                    yPercent = (preset.defaultHeadlineY + 36).coerceAtMost(86).toFloat()
                )
            } else {
                layer
            }
        }
    }

    fun updateAdditionalInstructions(instructions: String) {
        _additionalInstructions.value = instructions
    }

    fun selectAiModel(model: AiImageModel) {
        _selectedAiModel.value = model
    }

    fun addReferenceImage(bitmap: Bitmap, label: String, usage: ReferenceUsageOption) {
        val item = ReferenceImageItem(
            id = UUID.randomUUID().toString(),
            bitmap = bitmap,
            label = label,
            usageOption = usage
        )
        _referenceImages.value = _referenceImages.value + item
    }

    fun updateReferenceUsage(id: String, usage: ReferenceUsageOption) {
        _referenceImages.value = _referenceImages.value.map {
            if (it.id == id) it.copy(usageOption = usage) else it
        }
    }

    fun removeReferenceImage(id: String) {
        _referenceImages.value = _referenceImages.value.filterNot { it.id == id }
    }

    // --- AI Headline Recommendation ---
    fun requestAiHeadlines() {
        val topic = _videoTopic.value.trim()
        if (topic.isBlank()) {
            _generationError.value = "Please enter a video topic first so AI can suggest headlines."
            return
        }
        viewModelScope.launch {
            _isSuggestingHeadlines.value = true
            _generationError.value = null
            val result = aiService.suggestHeadlines(
                videoTopic = topic,
                category = _selectedCategory.value,
                style = _selectedStyle.value
            )
            _isSuggestingHeadlines.value = false
            result.onSuccess { (headlines, subs) ->
                if (headlines.isNotEmpty()) {
                    _suggestedHeadlines.value = headlines
                    _headlineText.value = headlines.first()
                }
                if (subs.isNotEmpty()) {
                    _subheadlineText.value = subs.first()
                }
                _statusBanner.value = "Generated ${headlines.size} headline recommendations"
            }.onFailure { err ->
                _generationError.value = err.localizedMessage ?: "Failed to suggest headlines."
            }
        }
    }

    // --- AI Art Direction Plan Only ---
    fun runArtDirectionAnalysisOnly() {
        val topic = _videoTopic.value.trim()
        if (topic.isBlank()) {
            _generationError.value = "Please enter a video topic to analyze."
            return
        }
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationProgressStage.value = "Analyzing your topic & crafting Art Direction Plan..."
            val planResult = aiService.analyzeTopicAndCreatePlan(
                videoTopic = topic,
                userHeadline = _headlineText.value,
                category = _selectedCategory.value,
                style = _selectedStyle.value,
                realism = _selectedRealism.value,
                aspectRatio = _selectedAspectRatio.value,
                compositionPreset = _selectedComposition.value,
                referenceImages = _referenceImages.value,
                additionalInstructions = _additionalInstructions.value
            )
            _isGenerating.value = false
            _generationProgressStage.value = null
            planResult.onSuccess { plan ->
                _artDirectionPlan.value = plan
                if (plan.suggestedHeadlines.isNotEmpty()) {
                    _suggestedHeadlines.value = plan.suggestedHeadlines
                    if (_headlineText.value.isBlank()) {
                        _headlineText.value = plan.suggestedHeadlines.first()
                    }
                }
                if (plan.recommendedSubheadline.isNotBlank() && _subheadlineText.value.isBlank()) {
                    _subheadlineText.value = plan.recommendedSubheadline
                }
                _statusBanner.value = "AI Art Direction Plan ready"
            }.onFailure { err ->
                _generationError.value = err.localizedMessage ?: "Art Direction analysis failed."
            }
        }
    }

    // --- Full AI Thumbnail Generation Pipeline ---
    fun generateFullThumbnail(context: Context) {
        val topic = _videoTopic.value.trim()
        if (topic.isBlank()) {
            _generationError.value = "Please enter what your video is about before generating."
            return
        }
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null

            // Step 1: Analyze topic & build Art Direction Plan
            _generationProgressStage.value = "Analyzing your topic & determining visual concept..."
            val planResult = aiService.analyzeTopicAndCreatePlan(
                videoTopic = topic,
                userHeadline = _headlineText.value,
                category = _selectedCategory.value,
                style = _selectedStyle.value,
                realism = _selectedRealism.value,
                aspectRatio = _selectedAspectRatio.value,
                compositionPreset = _selectedComposition.value,
                referenceImages = _referenceImages.value,
                additionalInstructions = _additionalInstructions.value
            )

            val plan = planResult.getOrElse { err ->
                _isGenerating.value = false
                _generationProgressStage.value = null
                _generationError.value = "Art Direction failed: ${err.localizedMessage ?: "Unknown error"}"
                return@launch
            }

            _artDirectionPlan.value = plan
            if (plan.suggestedHeadlines.isNotEmpty()) {
                _suggestedHeadlines.value = plan.suggestedHeadlines
                if (_headlineText.value.isBlank()) {
                    _headlineText.value = plan.suggestedHeadlines.first()
                }
            }

            // Step 2: Generate Photorealistic Scene Image
            _generationProgressStage.value = "Generating photorealistic thumbnail scene with ${_selectedAiModel.value.displayName} (may take 30–60s)..."
            val imageResult = aiService.generateThumbnailImage(
                plan = plan,
                realism = _selectedRealism.value,
                style = _selectedStyle.value,
                aspectRatio = _selectedAspectRatio.value,
                compositionPreset = _selectedComposition.value,
                referenceImages = _referenceImages.value,
                additionalInstructions = _additionalInstructions.value,
                modelChoice = _selectedAiModel.value
            )

            val generatedBitmap = imageResult.getOrElse { err ->
                _isGenerating.value = false
                _generationProgressStage.value = null
                _generationError.value = err.localizedMessage ?: "Image generation failed."
                return@launch
            }

            // Step 3: Save bitmap locally and configure smart typography layers
            _generationProgressStage.value = "Preparing composition & Smart Typography layers..."
            val savedPath = withContext(Dispatchers.IO) {
                ThumbnailCanvasRenderer.saveBitmapToInternalFile(context, generatedBitmap, "ai_bg")
            }

            pushUndoState()
            _backgroundBitmap.value = generatedBitmap
            _localImagePath.value = savedPath
            _projectTitle.value = topic.take(48)

            // Sync typography layers with user/AI headline and composition preset
            val preset = _selectedComposition.value
            val effectiveHeadline = _headlineText.value.ifBlank {
                plan.suggestedHeadlines.firstOrNull() ?: "MUST WATCH"
            }
            val effectiveSub = _subheadlineText.value.ifBlank {
                plan.recommendedSubheadline
            }
            val textCol = plan.recommendedTextColorHex.ifBlank { "#FFFFFF" }
            val accentCol = plan.recommendedAccentColorHex.ifBlank { _selectedStyle.value.accentColorHex }

            if (_includeTextOverlay.value) {
                _layers.value = listOf(
                    CanvasLayer(
                        id = "layer_badge",
                        name = "Category Tag",
                        type = LayerType.TEXT,
                        xPercent = preset.defaultHeadlineX.toFloat(),
                        yPercent = (preset.defaultHeadlineY - 10).coerceAtLeast(5).toFloat(),
                        widthPercent = 36f,
                        heightPercent = 9f,
                        zIndex = 1,
                        text = _selectedCategory.value.displayName.uppercase(),
                        fontName = StudioFont.MONTSERRAT.displayName,
                        fontSizeSp = 19f,
                        fontWeightBold = true,
                        textColorHex = "#FFFFFF",
                        outlineColorHex = "#000000",
                        outlineWidth = 0f,
                        shadowRadius = 6f,
                        backgroundBadgeHex = accentCol,
                        uppercase = true,
                        letterSpacing = 2.5f
                    ),
                    CanvasLayer(
                        id = "layer_headline",
                        name = "Main Headline",
                        type = LayerType.TEXT,
                        xPercent = preset.defaultHeadlineX.toFloat(),
                        yPercent = preset.defaultHeadlineY.toFloat(),
                        widthPercent = 54f,
                        heightPercent = 34f,
                        zIndex = 2,
                        text = effectiveHeadline.replace(" ", "\n"),
                        fontName = StudioFont.BEBAS_NEUE.displayName,
                        fontSizeSp = 74f,
                        fontWeightBold = true,
                        textColorHex = textCol,
                        outlineColorHex = "#000000",
                        outlineWidth = 5f,
                        shadowRadius = 16f,
                        backgroundBadgeHex = "",
                        uppercase = true,
                        letterSpacing = 1.5f,
                        lineHeightMultiplier = 0.92f
                    ),
                    CanvasLayer(
                        id = "layer_subheadline",
                        name = "Subheadline",
                        type = LayerType.TEXT,
                        xPercent = preset.defaultHeadlineX.toFloat(),
                        yPercent = (preset.defaultHeadlineY + 36).coerceAtMost(85).toFloat(),
                        widthPercent = 52f,
                        heightPercent = 14f,
                        zIndex = 3,
                        text = effectiveSub,
                        fontName = StudioFont.SPACE_GROTESK.displayName,
                        fontSizeSp = 24f,
                        fontWeightBold = true,
                        textColorHex = accentCol,
                        outlineColorHex = "#000000",
                        outlineWidth = 3f,
                        shadowRadius = 10f,
                        backgroundBadgeHex = "",
                        uppercase = true,
                        letterSpacing = 1.2f
                    )
                )
                _selectedLayerId.value = "layer_headline"
            } else {
                _layers.value = emptyList()
                _selectedLayerId.value = null
            }

            // Record in Firestore Generation History
            repository.addHistoryItem(
                GenerationHistoryItem(
                    userId = currentUserId,
                    videoTopic = topic,
                    suggestedHeadline = effectiveHeadline,
                    category = _selectedCategory.value.displayName,
                    visualStyle = _selectedStyle.value.displayName,
                    realism = _selectedRealism.value.displayName,
                    aspectRatio = _selectedAspectRatio.value.ratioLabel,
                    compositionPreset = _selectedComposition.value.displayName,
                    mainSubject = plan.mainSubject,
                    lightingSetup = plan.lightingSetup,
                    colorPalette = plan.colorPalette,
                    generatedPrompt = plan.masterImagePrompt.ifBlank { topic },
                    modelUsed = _selectedAiModel.value.modelId,
                    localImagePath = savedPath
                )
            )

            _isGenerating.value = false
            _generationProgressStage.value = null
            _statusBanner.value = "Thumbnail generated! Opening in Studio Editor."
            _currentDestination.value = StudioDestination.EDITOR
        }
    }

    // --- Editor Undo / Redo ---
    private fun captureSnapshot(): EditorSnapshot {
        return EditorSnapshot(
            backgroundBitmap = _backgroundBitmap.value,
            exposure = _exposure.value,
            contrast = _contrast.value,
            saturation = _saturation.value,
            temperature = _temperature.value,
            vignette = _vignette.value,
            backgroundBlur = _backgroundBlur.value,
            layers = _layers.value
        )
    }

    private fun pushUndoState() {
        undoStack.addLast(captureSnapshot())
        if (undoStack.size > 25) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(captureSnapshot())
        val prev = undoStack.removeLast()
        restoreSnapshot(prev)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(captureSnapshot())
        val next = redoStack.removeLast()
        restoreSnapshot(next)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun restoreSnapshot(snap: EditorSnapshot) {
        _backgroundBitmap.value = snap.backgroundBitmap
        _exposure.value = snap.exposure
        _contrast.value = snap.contrast
        _saturation.value = snap.saturation
        _temperature.value = snap.temperature
        _vignette.value = snap.vignette
        _backgroundBlur.value = snap.backgroundBlur
        _layers.value = snap.layers
    }

    fun resetEditorAdjustments() {
        pushUndoState()
        _exposure.value = 0
        _contrast.value = 10
        _saturation.value = 0
        _temperature.value = 0
        _vignette.value = 25
        _backgroundBlur.value = 0
        _canvasZoom.value = 1f
        _statusBanner.value = "Reset color grading and canvas adjustments"
    }

    // --- Editor Canvas & Grading Controls ---
    fun updateProjectTitle(title: String) {
        _projectTitle.value = title
    }

    fun setBackgroundBitmapFromUpload(context: Context, bitmap: Bitmap) {
        pushUndoState()
        _backgroundBitmap.value = bitmap
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = ThumbnailCanvasRenderer.saveBitmapToInternalFile(context, bitmap, "uploaded_bg")
            _localImagePath.value = savedPath
        }
    }

    fun updateColorGrading(
        exposure: Int = _exposure.value,
        contrast: Int = _contrast.value,
        saturation: Int = _saturation.value,
        temperature: Int = _temperature.value,
        vignette: Int = _vignette.value,
        blur: Int = _backgroundBlur.value
    ) {
        _exposure.value = exposure.coerceIn(-100, 100)
        _contrast.value = contrast.coerceIn(-100, 100)
        _saturation.value = saturation.coerceIn(-100, 100)
        _temperature.value = temperature.coerceIn(-100, 100)
        _vignette.value = vignette.coerceIn(0, 100)
        _backgroundBlur.value = blur.coerceIn(0, 100)
    }

    fun toggleRuleOfThirdsGrid() {
        _showRuleOfThirdsGrid.value = !_showRuleOfThirdsGrid.value
    }

    fun adjustZoom(delta: Float) {
        _canvasZoom.value = (_canvasZoom.value + delta).coerceIn(0.6f, 2.2f)
    }

    fun resetZoom() {
        _canvasZoom.value = 1f
    }

    // --- Layer System Operations ---
    fun selectLayer(layerId: String?) {
        _selectedLayerId.value = layerId
    }

    fun updateLayer(updated: CanvasLayer) {
        _layers.value = _layers.value.map { if (it.id == updated.id) updated else it }
    }

    fun moveSelectedLayerByDelta(dxPercent: Float, dyPercent: Float) {
        val selId = _selectedLayerId.value ?: return
        _layers.value = _layers.value.map { layer ->
            if (layer.id == selId && !layer.locked) {
                layer.copy(
                    xPercent = (layer.xPercent + dxPercent).coerceIn(-10f, 92f),
                    yPercent = (layer.yPercent + dyPercent).coerceIn(-10f, 92f)
                )
            } else {
                layer
            }
        }
    }

    fun addTextLayer(text: String = "NEW HEADLINE", isSubheadline: Boolean = false) {
        pushUndoState()
        val maxZ = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newLayer = CanvasLayer(
            id = "text_${UUID.randomUUID().toString().take(8)}",
            name = if (isSubheadline) "Subheadline $maxZ" else "Headline $maxZ",
            type = LayerType.TEXT,
            xPercent = 12f,
            yPercent = if (isSubheadline) 62f else 28f,
            widthPercent = 55f,
            heightPercent = if (isSubheadline) 14f else 26f,
            zIndex = maxZ,
            text = text,
            fontName = if (isSubheadline) StudioFont.MONTSERRAT.displayName else StudioFont.BEBAS_NEUE.displayName,
            fontSizeSp = if (isSubheadline) 26f else 62f,
            textColorHex = if (isSubheadline) "#FBBF24" else "#FFFFFF",
            outlineWidth = if (isSubheadline) 2f else 5f
        )
        _layers.value = _layers.value + newLayer
        _selectedLayerId.value = newLayer.id
    }

    fun addShapeLayer(kind: ShapeKind, colorHex: String = "#EF4444") {
        pushUndoState()
        val maxZ = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newLayer = CanvasLayer(
            id = "shape_${UUID.randomUUID().toString().take(8)}",
            name = kind.label,
            type = LayerType.SHAPE,
            xPercent = 25f,
            yPercent = 35f,
            widthPercent = if (kind == ShapeKind.LINE) 35f else 22f,
            heightPercent = if (kind == ShapeKind.LINE) 4f else 18f,
            zIndex = maxZ,
            shapeKind = kind,
            fillColorHex = colorHex
        )
        _layers.value = _layers.value + newLayer
        _selectedLayerId.value = newLayer.id
    }

    fun addSubjectOrOverlayLayer(bitmap: Bitmap, label: String, isOverlay: Boolean = false) {
        pushUndoState()
        val maxZ = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newLayer = CanvasLayer(
            id = "img_${UUID.randomUUID().toString().take(8)}",
            name = label,
            type = if (isOverlay) LayerType.OVERLAY else LayerType.SUBJECT,
            xPercent = if (isOverlay) 10f else 48f,
            yPercent = 12f,
            widthPercent = 44f,
            heightPercent = 76f,
            zIndex = maxZ,
            bitmap = bitmap
        )
        _layers.value = _layers.value + newLayer
        _selectedLayerId.value = newLayer.id
    }

    fun duplicateLayer(layerId: String) {
        val target = _layers.value.find { it.id == layerId } ?: return
        pushUndoState()
        val maxZ = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        val copy = target.copy(
            id = "${target.type.name.lowercase()}_${UUID.randomUUID().toString().take(8)}",
            name = "${target.name} Copy",
            xPercent = (target.xPercent + 4f).coerceAtMost(85f),
            yPercent = (target.yPercent + 4f).coerceAtMost(85f),
            zIndex = maxZ
        )
        _layers.value = _layers.value + copy
        _selectedLayerId.value = copy.id
    }

    fun deleteLayer(layerId: String) {
        pushUndoState()
        _layers.value = _layers.value.filterNot { it.id == layerId }
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = _layers.value.lastOrNull()?.id
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        pushUndoState()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(visible = !it.visible) else it
        }
    }

    fun toggleLayerLock(layerId: String) {
        pushUndoState()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(locked = !it.locked) else it
        }
    }

    fun bringLayerForward(layerId: String) {
        val target = _layers.value.find { it.id == layerId } ?: return
        pushUndoState()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(zIndex = target.zIndex + 2) else it
        }
    }

    fun sendLayerBackward(layerId: String) {
        val target = _layers.value.find { it.id == layerId } ?: return
        pushUndoState()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(zIndex = (target.zIndex - 2).coerceAtLeast(0)) else it
        }
    }

    // --- AI-Powered Editing Assistant inside Editor ---
    fun updateAiEditCommand(cmd: String) {
        _aiEditCommand.value = cmd
    }

    fun executeAiEdit(context: Context) {
        val cmd = _aiEditCommand.value.trim()
        if (cmd.isBlank()) return

        viewModelScope.launch {
            _isExecutingAiEdit.value = true
            _aiEditFeedback.value = "AI Assistant is analyzing your edit instruction..."

            val currentHeadlineLayer = _layers.value.firstOrNull { it.type == LayerType.TEXT }
            val currentHeadlineStr = currentHeadlineLayer?.text ?: _headlineText.value

            val planResult = aiService.interpretAiEditCommand(
                userCommand = cmd,
                currentHeadline = currentHeadlineStr,
                currentStyle = _selectedStyle.value.displayName
            )

            val editPlan = planResult.getOrElse { err ->
                _isExecutingAiEdit.value = false
                _aiEditFeedback.value = "AI Edit Error: ${err.localizedMessage ?: "Failed to interpret instruction"}"
                return@launch
            }

            pushUndoState()

            when (editPlan.editCategory) {
                "CANVAS_GRADING" -> {
                    _exposure.value = (_exposure.value + editPlan.exposureDelta).coerceIn(-100, 100)
                    _contrast.value = (_contrast.value + editPlan.contrastDelta).coerceIn(-100, 100)
                    _saturation.value = (_saturation.value + editPlan.saturationDelta).coerceIn(-100, 100)
                    _temperature.value = (_temperature.value + editPlan.temperatureDelta).coerceIn(-100, 100)
                    _vignette.value = (_vignette.value + editPlan.vignetteDelta).coerceIn(0, 100)
                    _backgroundBlur.value = (_backgroundBlur.value + editPlan.blurDelta).coerceIn(0, 100)
                    _aiEditFeedback.value = editPlan.explanation
                }

                "TYPOGRAPHY" -> {
                    val targetId = _selectedLayerId.value
                        ?: _layers.value.firstOrNull { it.id == "layer_headline" }?.id
                        ?: _layers.value.firstOrNull { it.type == LayerType.TEXT }?.id

                    if (targetId != null) {
                        _layers.value = _layers.value.map { layer ->
                            if (layer.id == targetId) {
                                layer.copy(
                                    text = editPlan.newHeadlineText.ifBlank { layer.text },
                                    fontSizeSp = (layer.fontSizeSp + editPlan.headlineSizeDelta).coerceIn(14f, 140f),
                                    xPercent = if (editPlan.headlineXTarget in 0..90) editPlan.headlineXTarget.toFloat() else layer.xPercent,
                                    yPercent = if (editPlan.headlineYTarget in 0..90) editPlan.headlineYTarget.toFloat() else layer.yPercent,
                                    textColorHex = editPlan.newHeadlineColorHex.ifBlank { layer.textColorHex },
                                    backgroundBadgeHex = editPlan.newAccentColorHex.ifBlank { layer.backgroundBadgeHex }
                                )
                            } else {
                                layer
                            }
                        }
                    }
                    _aiEditFeedback.value = editPlan.explanation
                }

                "IMAGE_REGENERATION" -> {
                    val currentBmp = _backgroundBitmap.value
                    val promptToRun = editPlan.imageEditInstruction.ifBlank { cmd }
                    _aiEditFeedback.value = "Regenerating scene with ${_selectedAiModel.value.displayName} (may take 30–60s)..."

                    val bmpResult = if (currentBmp != null) {
                        aiService.editBitmapWithAi(currentBmp, promptToRun, _selectedAiModel.value)
                    } else {
                        val fallbackPlan = _artDirectionPlan.value ?: ArtDirectionPlan(
                            mainSubject = promptToRun,
                            backgroundEnvironment = promptToRun,
                            lightingSetup = _selectedStyle.value.lightingGuide,
                            colorPalette = "High contrast cinematic tones",
                            mood = "Dramatic documentary"
                        )
                        aiService.generateThumbnailImage(
                            plan = fallbackPlan,
                            realism = _selectedRealism.value,
                            style = _selectedStyle.value,
                            aspectRatio = _selectedAspectRatio.value,
                            compositionPreset = _selectedComposition.value,
                            referenceImages = _referenceImages.value,
                            additionalInstructions = promptToRun,
                            modelChoice = _selectedAiModel.value
                        )
                    }

                    bmpResult.onSuccess { newBmp ->
                        _backgroundBitmap.value = newBmp
                        val savedPath = withContext(Dispatchers.IO) {
                            ThumbnailCanvasRenderer.saveBitmapToInternalFile(context, newBmp, "ai_edit")
                        }
                        _localImagePath.value = savedPath
                        _aiEditFeedback.value = editPlan.explanation
                    }.onFailure { err ->
                        _aiEditFeedback.value = "AI Image Edit failed: ${err.localizedMessage ?: "Error"}"
                    }
                }
            }

            _aiEditCommand.value = ""
            _isExecutingAiEdit.value = false
        }
    }

    // --- Project Persistence (Firestore + Local Bitmap Cache) ---
    fun saveCurrentProject(context: Context, saveAsTemplate: Boolean = false) {
        viewModelScope.launch {
            val composite = withContext(Dispatchers.Default) {
                renderCurrentComposite(context, includeGrid = false)
            }
            val savedPath = withContext(Dispatchers.IO) {
                ThumbnailCanvasRenderer.saveBitmapToInternalFile(context, composite, "proj_preview")
            }
            _localImagePath.value = savedPath

            val headlineLayer = _layers.value.find { it.id == "layer_headline" }
                ?: _layers.value.firstOrNull { it.type == LayerType.TEXT }
            val subLayer = _layers.value.find { it.id == "layer_subheadline" }

            val project = ThumbnailProject(
                id = _activeProjectId.value.orEmpty(),
                userId = currentUserId,
                title = _projectTitle.value.ifBlank { "Untitled Thumbnail" },
                videoTopic = _videoTopic.value.ifBlank { _projectTitle.value },
                headline = headlineLayer?.text?.replace("\n", " ") ?: _headlineText.value,
                subheadline = subLayer?.text ?: _subheadlineText.value,
                category = _selectedCategory.value.displayName,
                visualStyle = _selectedStyle.value.displayName,
                realism = _selectedRealism.value.displayName,
                aspectRatio = _selectedAspectRatio.value.ratioLabel,
                compositionPreset = _selectedComposition.value.displayName,
                width = _customWidth.value.toLong(),
                height = _customHeight.value.toLong(),
                aiPrompt = _artDirectionPlan.value?.masterImagePrompt ?: _additionalInstructions.value,
                artDirectionSummary = _artDirectionPlan.value?.toCompactSummary() ?: "",
                headlineFont = headlineLayer?.fontName ?: StudioFont.BEBAS_NEUE.displayName,
                headlineColorHex = headlineLayer?.textColorHex ?: "#FFFFFF",
                accentColorHex = _selectedStyle.value.accentColorHex,
                headlineSizeSp = (headlineLayer?.fontSizeSp ?: 64f).toLong(),
                headlineX = (headlineLayer?.xPercent ?: 8f).toLong(),
                headlineY = (headlineLayer?.yPercent ?: 22f).toLong(),
                exposure = _exposure.value.toLong(),
                contrast = _contrast.value.toLong(),
                saturation = _saturation.value.toLong(),
                temperature = _temperature.value.toLong(),
                vignette = _vignette.value.toLong(),
                backgroundBlur = _backgroundBlur.value.toLong(),
                localImagePath = savedPath,
                layersSummary = _layers.value.take(25).map { it.serialize() },
                isTemplate = saveAsTemplate
            )

            val existingId = _activeProjectId.value
            if (existingId.isNullOrBlank() || saveAsTemplate) {
                val res = repository.createProject(project)
                res.onSuccess { newId ->
                    if (!saveAsTemplate) _activeProjectId.value = newId
                    _statusBanner.value = if (saveAsTemplate) {
                        "Saved reusable template to Cloud Firestore"
                    } else {
                        "Project '${project.title}' saved to Cloud Firestore"
                    }
                }.onFailure { err ->
                    _statusBanner.value = "Save failed: ${err.localizedMessage}"
                }
            } else {
                val res = repository.updateProject(project.copy(id = existingId))
                res.onSuccess {
                    _statusBanner.value = "Updated '${project.title}' in Cloud Firestore"
                }.onFailure { err ->
                    _statusBanner.value = "Update failed: ${err.localizedMessage}"
                }
            }
        }
    }

    fun openProjectInEditor(project: ThumbnailProject) {
        _activeProjectId.value = if (project.isTemplate) null else project.id
        _projectTitle.value = if (project.isTemplate) "${project.title} (Copy)" else project.title
        _videoTopic.value = project.videoTopic
        _headlineText.value = project.headline
        _subheadlineText.value = project.subheadline
        _selectedCategory.value = VideoCategory.entries.find { it.displayName == project.category }
            ?: VideoCategory.DOCUMENTARY
        _selectedStyle.value = VisualStylePreset.entries.find { it.displayName == project.visualStyle }
            ?: VisualStylePreset.REALISTIC_DOCUMENTARY
        _selectedRealism.value = ImageRealismMode.entries.find { it.displayName == project.realism }
            ?: ImageRealismMode.PHOTOREALISTIC
        _selectedAspectRatio.value = ThumbnailAspectRatio.entries.find { it.ratioLabel == project.aspectRatio }
            ?: ThumbnailAspectRatio.YOUTUBE_16_9
        _selectedComposition.value = CompositionPreset.entries.find { it.displayName == project.compositionPreset }
            ?: CompositionPreset.SUBJECT_RIGHT_HEADLINE_LEFT
        _customWidth.value = project.width.toInt().coerceIn(320, 4096)
        _customHeight.value = project.height.toInt().coerceIn(320, 4096)
        _exposure.value = project.exposure.toInt()
        _contrast.value = project.contrast.toInt()
        _saturation.value = project.saturation.toInt()
        _temperature.value = project.temperature.toInt()
        _vignette.value = project.vignette.toInt()
        _backgroundBlur.value = project.backgroundBlur.toInt()
        _localImagePath.value = project.localImagePath

        val parsedLayers = project.layersSummary.mapNotNull { CanvasLayer.deserialize(it) }
        _layers.value = if (parsedLayers.isNotEmpty()) parsedLayers else defaultInitialLayers()
        _selectedLayerId.value = _layers.value.firstOrNull()?.id

        if (project.localImagePath.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val file = File(project.localImagePath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        _backgroundBitmap.value = bmp
                    }
                }
            }
        } else {
            _backgroundBitmap.value = null
        }

        _currentDestination.value = StudioDestination.EDITOR
    }

    fun startNewBlankProject() {
        _activeProjectId.value = null
        _projectTitle.value = "New Thumbnail Project"
        _backgroundBitmap.value = null
        _localImagePath.value = ""
        _exposure.value = 0
        _contrast.value = 10
        _saturation.value = 0
        _temperature.value = 0
        _vignette.value = 25
        _backgroundBlur.value = 0
        _layers.value = defaultInitialLayers()
        _selectedLayerId.value = _layers.value.firstOrNull()?.id
        _currentDestination.value = StudioDestination.GENERATOR
    }

    fun duplicateExistingProject(project: ThumbnailProject) {
        viewModelScope.launch {
            val copy = project.copy(
                id = "",
                title = "${project.title} (Copy)",
                isTemplate = false
            )
            repository.createProject(copy).onSuccess {
                _statusBanner.value = "Duplicated '${project.title}'"
            }
        }
    }

    fun renameExistingProject(project: ThumbnailProject, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateProject(project.copy(title = newTitle.trim())).onSuccess {
                _statusBanner.value = "Renamed project to '${newTitle.trim()}'"
            }
        }
    }

    fun deleteExistingProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId).onSuccess {
                if (_activeProjectId.value == projectId) {
                    _activeProjectId.value = null
                }
                _statusBanner.value = "Deleted project"
            }
        }
    }

    fun deleteHistoryRecord(historyId: String) {
        viewModelScope.launch {
            repository.deleteHistoryItem(historyId)
        }
    }

    fun saveBrandKitItem(kit: BrandKit) {
        viewModelScope.launch {
            repository.saveBrandKit(kit).onSuccess {
                _statusBanner.value = "Brand Kit '${kit.brandName}' saved"
            }
        }
    }

    fun applyBrandKitToEditor(kit: BrandKit) {
        pushUndoState()
        _selectedStyle.value = VisualStylePreset.entries.find { it.displayName == kit.defaultStyle }
            ?: _selectedStyle.value
        _layers.value = _layers.value.map { layer ->
            if (layer.id == "layer_headline") {
                layer.copy(
                    fontName = kit.primaryFont,
                    textColorHex = kit.primaryColorHex
                )
            } else if (layer.id == "layer_subheadline") {
                layer.copy(
                    fontName = kit.secondaryFont,
                    textColorHex = kit.secondaryColorHex
                )
            } else if (layer.id == "layer_badge") {
                layer.copy(
                    backgroundBadgeHex = kit.accentColorHex,
                    text = kit.channelTag.ifBlank { layer.text }
                )
            } else {
                layer
            }
        }
        _statusBanner.value = "Applied Brand Kit '${kit.brandName}' to canvas"
    }

    fun deleteBrandKitItem(kitId: String) {
        viewModelScope.launch {
            repository.deleteBrandKit(kitId)
        }
    }

    fun saveUserPreferences(
        displayName: String,
        defaultCategory: VideoCategory,
        defaultStyle: VisualStylePreset,
        defaultRealism: ImageRealismMode,
        defaultAspectRatio: ThumbnailAspectRatio,
        preferredModel: AiImageModel
    ) {
        viewModelScope.launch {
            val currentProfile = (userProfileState.value as? DataUiState.Success)?.data
            val profile = UserProfile(
                id = currentUserId,
                userId = currentUserId,
                displayName = displayName.ifBlank { "Creator" },
                email = currentProfile?.email,
                photoUrl = currentProfile?.photoUrl,
                defaultCategory = defaultCategory.displayName,
                defaultStyle = defaultStyle.displayName,
                defaultRealism = defaultRealism.displayName,
                defaultAspectRatio = defaultAspectRatio.ratioLabel,
                preferredAiModel = preferredModel.modelId
            )
            repository.saveUserProfile(profile, isCreate = currentProfile == null).onSuccess {
                _selectedCategory.value = defaultCategory
                _selectedStyle.value = defaultStyle
                _selectedRealism.value = defaultRealism
                _selectedAspectRatio.value = defaultAspectRatio
                _selectedAiModel.value = preferredModel
                _statusBanner.value = "Studio preferences saved to Cloud Firestore"
            }
        }
    }

    fun renderCurrentComposite(context: Context, includeGrid: Boolean = _showRuleOfThirdsGrid.value): Bitmap {
        val style = _selectedStyle.value
        return ThumbnailCanvasRenderer.renderCompositeBitmap(
            context = context,
            width = _customWidth.value,
            height = _customHeight.value,
            backgroundBitmap = _backgroundBitmap.value,
            primaryBgHex = style.primaryColorHex,
            secondaryBgHex = style.secondaryColorHex,
            accentHex = style.accentColorHex,
            exposure = _exposure.value,
            contrast = _contrast.value,
            saturation = _saturation.value,
            temperature = _temperature.value,
            vignette = _vignette.value,
            backgroundBlur = _backgroundBlur.value,
            layers = _layers.value,
            showRuleOfThirdsGrid = includeGrid
        )
    }

    fun exportAndShareHighResThumbnail(context: Context) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                renderCurrentComposite(context, includeGrid = false)
            }
            val result = withContext(Dispatchers.IO) {
                ThumbnailCanvasRenderer.exportToGalleryAndShare(
                    context = context,
                    bitmap = bitmap,
                    projectTitle = _projectTitle.value
                )
            }
            result.onSuccess { msg ->
                _statusBanner.value = msg
            }.onFailure { err ->
                _statusBanner.value = "Export failed: ${err.localizedMessage}"
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: StudioRepository,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StudioViewModel(
                    repository = repository,
                    aiService = ThumbnailAiDirectorService(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
