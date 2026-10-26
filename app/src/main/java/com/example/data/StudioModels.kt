package com.example.data

import android.graphics.Bitmap
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val email: String? = null,
    val photoUrl: String? = null,
    val defaultCategory: String = VideoCategory.DOCUMENTARY.displayName,
    val defaultStyle: String = VisualStylePreset.REALISTIC_DOCUMENTARY.displayName,
    val defaultRealism: String = ImageRealismMode.PHOTOREALISTIC.displayName,
    val defaultAspectRatio: String = ThumbnailAspectRatio.YOUTUBE_16_9.ratioLabel,
    val preferredAiModel: String = AiImageModel.NANO_BANANA_2_1.modelId,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ThumbnailProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Thumbnail",
    val videoTopic: String = "",
    val headline: String = "",
    val subheadline: String = "",
    val category: String = VideoCategory.DOCUMENTARY.displayName,
    val visualStyle: String = VisualStylePreset.REALISTIC_DOCUMENTARY.displayName,
    val realism: String = ImageRealismMode.PHOTOREALISTIC.displayName,
    val aspectRatio: String = ThumbnailAspectRatio.YOUTUBE_16_9.ratioLabel,
    val compositionPreset: String = CompositionPreset.SUBJECT_RIGHT_HEADLINE_LEFT.displayName,
    val width: Long = 1280L,
    val height: Long = 720L,
    val aiPrompt: String = "",
    val artDirectionSummary: String = "",
    val headlineFont: String = StudioFont.BEBAS_NEUE.displayName,
    val headlineColorHex: String = "#FFFFFF",
    val accentColorHex: String = "#F59E0B",
    val headlineSizeSp: Long = 64L,
    val headlineX: Long = 8L,
    val headlineY: Long = 22L,
    val exposure: Long = 0L,
    val contrast: Long = 10L,
    val saturation: Long = 0L,
    val temperature: Long = 0L,
    val vignette: Long = 25L,
    val backgroundBlur: Long = 0L,
    val localImagePath: String = "",
    val layersSummary: List<String> = emptyList(),
    val isTemplate: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GenerationHistoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val videoTopic: String = "",
    val suggestedHeadline: String = "",
    val category: String = VideoCategory.DOCUMENTARY.displayName,
    val visualStyle: String = VisualStylePreset.REALISTIC_DOCUMENTARY.displayName,
    val realism: String = ImageRealismMode.PHOTOREALISTIC.displayName,
    val aspectRatio: String = ThumbnailAspectRatio.YOUTUBE_16_9.ratioLabel,
    val compositionPreset: String = CompositionPreset.SUBJECT_RIGHT_HEADLINE_LEFT.displayName,
    val mainSubject: String = "",
    val lightingSetup: String = "",
    val colorPalette: String = "",
    val generatedPrompt: String = "",
    val modelUsed: String = AiImageModel.NANO_BANANA_2_1.modelId,
    val localImagePath: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BrandKit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val brandName: String = "My Channel Brand",
    val primaryFont: String = StudioFont.BEBAS_NEUE.displayName,
    val secondaryFont: String = StudioFont.MONTSERRAT.displayName,
    val primaryColorHex: String = "#FFFFFF",
    val secondaryColorHex: String = "#E2E8F0",
    val accentColorHex: String = "#F59E0B",
    val defaultStyle: String = VisualStylePreset.REALISTIC_DOCUMENTARY.displayName,
    val channelTag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AiImageModel(val modelId: String, val displayName: String, val badge: String) {
    NANO_BANANA_2_1("gemini-nano-banana-2.1", "Gemini Nano Banana 2.1", "Default • Fast & High-Precision"),
    PRO_IMAGE("gemini-3-pro-image", "Gemini 3 Pro Image", "Studio Master • Maximum Detail"),
    FLASH_LITE_IMAGE("gemini-3.1-flash-lite-image", "Gemini 3.1 Flash-Lite Image", "Ultra-Fast • Rapid Concepting");

    companion object {
        fun fromId(id: String): AiImageModel = entries.find { it.modelId == id } ?: NANO_BANANA_2_1
    }
}

enum class VideoCategory(val displayName: String, val artDirectionHint: String) {
    DOCUMENTARY("Documentary", "Investigative depth, authentic real-world textures, natural skin tones, directional cinema lighting"),
    EDUCATION("Education", "Clear conceptual focus, high visual clarity, clean background separation, approachable subject framing"),
    SCIENCE("Science", "Laboratory precision, optical depth, authentic scientific instruments, controlled cool-warm contrast"),
    TECHNOLOGY("Technology", "Modern hardware realism, subtle screen reflections, clean industrial geometry, crisp edge lighting"),
    ARTIFICIAL_INTELLIGENCE("Artificial Intelligence", "Server architecture, neural research lab realism, deep slate blues and warm tungsten accents"),
    HISTORY("History", "Period-accurate material textures, archival warmth, chiaroscuro lighting, weathered stone and parchment tones"),
    BIOGRAPHY("Biography", "Intimate 85mm portrait lens, expressive eyes, dramatic Rembrandt lighting, contextual background blur"),
    BUSINESS("Business", "Executive architecture, glass and steel reflections, editorial magazine portrait lighting, confident posture"),
    FINANCE("Finance", "Institutional authority, macroeconomic tension, deep emerald/amber/slate tones, sharp subject isolation"),
    NEWS("News", "Photojournalistic urgency, authentic field reporting atmosphere, high-contrast documentary realism"),
    POLITICS("Politics", "Press room or institutional architecture, serious gravitas, controlled editorial contrast"),
    MYSTERY("Mystery", "Low-key noir lighting, atmospheric fog or shadow corridors, intriguing focal object, cold-warm tension"),
    CRIME("Crime", "Investigative case file atmosphere, moody sodium-vapor and cool moonlight contrast, documentary grit"),
    MOTIVATION("Motivation", "Golden hour rim lighting, determined human expression, expansive horizon or focused training space"),
    SELF_IMPROVEMENT("Self-Improvement", "Minimalist warm morning light, calm intentional workspace, clean negative space, editorial clarity"),
    GAMING("Gaming", "Dynamic perspective, rich atmospheric lighting, expressive reaction or high-detail environment"),
    ENTERTAINMENT("Entertainment", "Vibrant theatrical lighting, expressive emotion, bold foreground-background separation"),
    SPORTS("Sports", "High-shutter stadium lighting, sweat and motion texture, intense athletic focus, dramatic angle"),
    HEALTH_AND_FITNESS("Health and Fitness", "Natural daylight, anatomical realism, energetic contrast, clean modern studio environment"),
    TRAVEL("Travel", "Epic golden or blue hour landscape, authentic cultural architecture, 24mm environmental portrait"),
    CUSTOM("Custom Category", "Tailored art-directed photography with balanced negative space and high mobile readability")
}

enum class VisualStylePreset(
    val displayName: String,
    val shortDescription: String,
    val lightingGuide: String,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val accentColorHex: String
) {
    REALISTIC_DOCUMENTARY(
        "Realistic Documentary",
        "Authentic 35mm cinema photography with natural skin texture and grounded environments.",
        "Soft directional key light with natural shadows and subtle warm practicals",
        "#0F172A", "#1E293B", "#F59E0B"
    ),
    CINEMATIC_INVESTIGATIVE(
        "Cinematic Investigative",
        "High-tension investigative journalism look with moody shadows and controlled red/amber accents.",
        "Low-key side lighting with cool slate shadows and warm tungsten rim light",
        "#090D16", "#1E1B4B", "#EF4444"
    ),
    EDUCATIONAL_EXPLAINER(
        "Educational Explainer",
        "Crisp, high-clarity subject isolation with clean depth of field and inviting contrast.",
        "Bright diffused studio key light with clean background separation",
        "#0F172A", "#0369A1", "#38BDF8"
    ),
    DARK_MYSTERY(
        "Dark Mystery",
        "Atmospheric noir aesthetic with deep shadows, volumetric depth, and dramatic focal points.",
        "Chiaroscuro spotlighting with deep shadow falloff and cold cyan/amber contrast",
        "#05070B", "#111827", "#A855F7"
    ),
    HISTORICAL_DOCUMENTARY(
        "Historical Documentary",
        "Rich period textures, archival warmth, weathered materials, and painterly lighting.",
        "Warm golden directional window light with deep oil-painting shadow tones",
        "#1C1917", "#44403C", "#D97706"
    ),
    SCIENTIFIC_VISUALIZATION(
        "Scientific Visualization",
        "Optical precision, research lab environments, macro detail, and bioluminescent/cyan accents.",
        "Clean optical lab lighting with cool cyan and crisp white specular highlights",
        "#081420", "#0F2942", "#06B6D4"
    ),
    TECHNOLOGY_AND_AI(
        "Technology and AI",
        "Modern hardware and AI research aesthetics with sleek glass, silicon, and subtle neon rim light.",
        "Dual-tone cool blue and warm amber rim lighting on matte dark surfaces",
        "#0B0F19", "#1E293B", "#6366F1"
    ),
    BUSINESS_DOCUMENTARY(
        "Business Documentary",
        "Fortune-500 editorial photography with architectural depth and authoritative framing.",
        "Polished editorial key light with natural skyscraper window reflections",
        "#0F172A", "#334155", "#10B981"
    ),
    FINANCIAL_ANALYSIS(
        "Financial Analysis",
        "Institutional market tension with high-contrast subject framing and disciplined color grading.",
        "Crisp directional lighting with deep emerald and crimson market accents",
        "#091310", "#142E24", "#22C55E"
    ),
    BIOGRAPHY_PORTRAIT(
        "Biography Portrait",
        "Intimate 85mm f/1.4 character study emphasizing facial micro-expressions and eye catchlights.",
        "Classic 45-degree Rembrandt portrait lighting with soft fill and rim separation",
        "#18181B", "#27272A", "#FBBF24"
    ),
    NEWS_INVESTIGATION(
        "News Investigation",
        "Urgent photojournalistic framing with documentary realism and high-impact headline contrast.",
        "Naturalistic field lighting with high local contrast and sharp focus",
        "#111827", "#1F2937", "#F43F5E"
    ),
    CINEMATIC_STORYTELLING(
        "Cinematic Storytelling",
        "Anamorphic film look with rich teal-and-orange color harmony and emotional depth.",
        "Cinematic motivated lighting with warm skin tones against cool ambient shadows",
        "#0C131F", "#1E293B", "#FB923C"
    ),
    MINIMALIST_EDITORIAL(
        "Minimalist Editorial",
        "Generous negative space, single iconic subject, and razor-sharp typographic hierarchy.",
        "Clean softbox studio lighting with smooth gradient backdrop",
        "#18181B", "#3F3F46", "#FAFAFA"
    ),
    HIGH_CONTRAST_YOUTUBE(
        "High-Contrast YouTube",
        "Maximum mobile feed visibility with punchy subject separation and vibrant natural colors.",
        "Brightthree-point studio lighting with strong rim light for subject pop",
        "#0F172A", "#1E3A8A", "#FACC15"
    ),
    PREMIUM_PROFESSIONAL(
        "Premium Professional",
        "Luxury studio production value with refined color grading and balanced rule-of-thirds layout.",
        "Soft wrap-around key light with subtle golden hair/edge light",
        "#0B0F17", "#1E2433", "#EAB308"
    ),
    GAMING(
        "Gaming",
        "High-energy immersive scene with dramatic perspective and rich environmental lighting.",
        "Dynamic colored rim lighting balanced with natural facial illumination",
        "#0D0914", "#2E1065", "#EC4899"
    ),
    SPORTS(
        "Sports",
        "Raw athletic intensity with crisp sweat texture, stadium lights, and decisive action.",
        "Hard overhead stadium rim lighting with high-contrast facial sculpting",
        "#09101D", "#1E293B", "#84CC16"
    ),
    CUSTOM_STYLE(
        "Custom Style",
        "Fully customizable art direction driven by your specific additional instructions.",
        "Custom studio lighting tailored to topic and user instructions",
        "#0F172A", "#1E293B", "#38BDF8"
    )
}

enum class ImageRealismMode(val displayName: String, val promptDirective: String) {
    PHOTOREALISTIC(
        "Photorealistic",
        "Shot on ARRI Alexa Mini LF with 50mm Zeiss Supreme Prime lens, natural skin pores, authentic fabric weave, physically accurate shadows, zero AI plastic sheen"
    ),
    CINEMATIC_PHOTOGRAPHY(
        "Cinematic Photography",
        "35mm cinema film still, Kodak Vision3 500T color science, subtle film grain, anamorphic depth of field, motivated directional lighting"
    ),
    EDITORIAL_PHOTOGRAPHY(
        "Editorial Photography",
        "Medium format Hasselblad H6D-100c editorial portrait, razor-sharp eye focus, magazine cover lighting, refined tonal roll-off"
    ),
    DOCUMENTARY_PHOTOGRAPHY(
        "Documentary Photography",
        "Leica SL2-S 35mm reportage photography, available natural light, candid emotional authenticity, unretouched realistic textures"
    ),
    REALISTIC_3D(
        "Realistic 3D",
        "Octane / Unreal Engine 5 path-traced studio render, subsurface scattering, physically based materials, volumetric lighting"
    ),
    STYLIZED_ILLUSTRATION(
        "Stylized Illustration",
        "High-end editorial concept illustration, painterly brushwork, bold graphic shapes, sophisticated lighting"
    ),
    MIXED_MEDIA(
        "Mixed Media",
        "Photorealistic subject composited with tactile archival paper textures, subtle blueprint overlays, and studio depth"
    )
}

enum class ThumbnailAspectRatio(
    val displayName: String,
    val ratioLabel: String,
    val defaultWidth: Int,
    val defaultHeight: Int
) {
    YOUTUBE_16_9("YouTube Thumbnail", "16:9", 1280, 720),
    SHORTS_9_16("YouTube Shorts Cover", "9:16", 720, 1280),
    INSTAGRAM_1_1("Instagram Post", "1:1", 1080, 1080),
    INSTAGRAM_4_5("Instagram Portrait", "4:5", 1080, 1350),
    CUSTOM("Custom Dimensions", "Custom", 1280, 720);

    val aspectFloat: Float
        get() = defaultWidth.toFloat() / defaultHeight.toFloat().coerceAtLeast(1f)
}

enum class CompositionPreset(
    val displayName: String,
    val description: String,
    val defaultHeadlineX: Int,
    val defaultHeadlineY: Int,
    val promptLayoutDirective: String
) {
    SUBJECT_LEFT_HEADLINE_RIGHT(
        "Subject on the left, headline on the right",
        "Primary subject anchored on left rule-of-thirds line; clean negative space on right for bold typography.",
        52, 24,
        "Position the primary subject on the left third of the frame, leaving uncluttered dark/clean negative space on the right half for headline placement."
    ),
    SUBJECT_RIGHT_HEADLINE_LEFT(
        "Subject on the right, headline on the left",
        "Classic documentary layout: expressive subject on right, high-contrast negative space on left.",
        6, 24,
        "Position the primary subject on the right third of the frame looking slightly toward center, with clean, unobstructed negative space on the left half for headline typography."
    ),
    CENTERED_PORTRAIT(
        "Centered portrait",
        "Symmetrical iconic portrait with top or bottom negative space for impactful title overlay.",
        15, 72,
        "Frame the main subject dead-center with strong symmetry and leave clean atmospheric negative space in the lower and upper thirds."
    ),
    SPLIT_SCREEN_COMPARISON(
        "Split-screen comparison",
        "Dual-side contrast comparing two states, eras, or opposing forces with central visual tension.",
        18, 12,
        "Create a balanced split-screen composition contrasting two distinct environments or subjects on left and right with a clean upper zone for headline text."
    ),
    BEFORE_AND_AFTER(
        "Before-and-after composition",
        "Progressive transformation from left (before) to right (after) with clear visual storytelling.",
        14, 14,
        "Compose a clear before-and-after visual progression from left to right with high contrast and uncluttered space at the top."
    ),
    OBJECT_FOCUSED(
        "Object-focused composition",
        "Hero macro shot of a single symbolic object or technology with dramatic lighting and bokeh.",
        8, 22,
        "Focus tightly on a single hero object on the right side with shallow depth of field and clean dark negative space on the left."
    ),
    DOCUMENTARY_SCENE_OVERLAY(
        "Documentary scene with headline overlay",
        "Wide environmental storytelling shot with darkened upper/left gradient zone for headline.",
        8, 18,
        "Wide cinematic documentary scene with deep shadow falloff on the upper-left area to provide natural negative space for typography."
    ),
    LARGE_FOREGROUND_ENVIRONMENTAL(
        "Large foreground subject with environmental background",
        "Close-up subject in foreground with rich contextual environment visible in the background.",
        8, 25,
        "Place a large, sharp foreground subject on the right and a detailed contextual environment in the background on the left with controlled lighting for text contrast."
    ),
    THREE_ELEMENT_STORYTELLING(
        "Three-element storytelling composition",
        "Balances primary subject, secondary clue element, and headline in a clear triangle hierarchy.",
        8, 20,
        "Arrange the primary subject on the right, a secondary storytelling object in the mid-ground, and clean negative space on the upper-left."
    ),
    MINIMALIST_CINEMATIC(
        "Minimalist cinematic composition",
        "60% negative space with a stark, unforgettable focal subject and gallery-grade lighting.",
        8, 32,
        "Minimalist composition with 60% dark atmospheric negative space on the left and a single dramatically lit subject on the right."
    ),
    MAP_AND_LOCATION(
        "Map and location-based composition",
        "Geopolitical or historical map backdrop integrated with a foreground portrait or landmark.",
        8, 22,
        "Include a realistic tactile map or geographical terrain in the background with the main subject anchored on the right and clear text space on the left."
    ),
    TIMELINE_HISTORICAL(
        "Timeline or historical comparison composition",
        "Chronological depth connecting historical origins with modern consequences.",
        10, 16,
        "Compose a historical-to-modern visual comparison with archival warmth on one side and crisp modern lighting on the other, leaving upper negative space."
    )
}

enum class ReferenceUsageOption(val displayName: String, val instructionPrefix: String) {
    MAIN_SUBJECT("Use as the main subject", "Preserve this reference image's subject identity, facial structure, and key appearance as the main subject"),
    BACKGROUND("Use as a background", "Use the environment and spatial layout of this reference image as the background scene"),
    COMPOSITION("Use as a composition reference", "Follow the subject framing, camera angle, and spatial balance of this reference image"),
    COLOR_PALETTE("Use as a color reference", "Match the color grading, lighting temperature, and tonal contrast of this reference image"),
    STYLE_REFERENCE("Use as a style reference", "Match the photographic realism, lens depth, and atmospheric mood of this reference image")
}

data class ReferenceImageItem(
    val id: String,
    val bitmap: Bitmap,
    val label: String,
    val usageOption: ReferenceUsageOption = ReferenceUsageOption.MAIN_SUBJECT
)

enum class StudioFont(val displayName: String) {
    BEBAS_NEUE("Bebas Neue"),
    OSWALD("Oswald"),
    MONTSERRAT("Montserrat"),
    SPACE_GROTESK("Space Grotesk"),
    PLAYFAIR_DISPLAY("Playfair Display"),
    INTER("Inter")
}

data class ArtDirectionPlan(
    val mainSubject: String = "",
    val supportingSubjects: String = "",
    val backgroundEnvironment: String = "",
    val foregroundElements: String = "",
    val cameraAngle: String = "",
    val shotType: String = "",
    val lightingSetup: String = "",
    val colorPalette: String = "",
    val mood: String = "",
    val emotionalTone: String = "",
    val composition: String = "",
    val textPlacement: String = "",
    val negativeSpace: String = "",
    val depthOfField: String = "",
    val visualHierarchy: String = "",
    val realismRequirements: String = "",
    val thingsToAvoid: String = "",
    val suggestedHeadlines: List<String> = emptyList(),
    val recommendedSubheadline: String = "",
    val recommendedCompositionPreset: String = CompositionPreset.SUBJECT_RIGHT_HEADLINE_LEFT.displayName,
    val recommendedTextColorHex: String = "#FFFFFF",
    val recommendedAccentColorHex: String = "#F59E0B",
    val masterImagePrompt: String = ""
) {
    fun toCompactSummary(): String {
        return buildString {
            append("Subject: ").append(mainSubject.take(160)).append(" | ")
            append("Env: ").append(backgroundEnvironment.take(160)).append(" | ")
            append("Lighting: ").append(lightingSetup.take(160)).append(" | ")
            append("Palette: ").append(colorPalette.take(120)).append(" | ")
            append("Mood: ").append(mood.take(80))
        }.take(1950)
    }
}

enum class LayerType(val label: String) {
    BACKGROUND("Background Image"),
    SUBJECT("Subject Layer"),
    TEXT("Text Layer"),
    SHAPE("Shape / Arrow / Badge"),
    OVERLAY("Lighting / Vignette Overlay"),
    GENERATED_ELEMENT("AI Visual Element")
}

enum class ShapeKind(val label: String) {
    RECTANGLE("Highlight Box"),
    CIRCLE("Circle / Spotlight"),
    ARROW("Directional Arrow"),
    LINE("Accent Divider Line"),
    BADGE("Category Pill Label")
}

data class CanvasLayer(
    val id: String,
    val name: String,
    val type: LayerType,
    val xPercent: Float = 8f,
    val yPercent: Float = 20f,
    val widthPercent: Float = 55f,
    val heightPercent: Float = 22f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val zIndex: Int = 0,
    // Text-specific properties
    val text: String = "",
    val fontName: String = StudioFont.BEBAS_NEUE.displayName,
    val fontSizeSp: Float = 56f,
    val fontWeightBold: Boolean = true,
    val textColorHex: String = "#FFFFFF",
    val outlineColorHex: String = "#000000",
    val outlineWidth: Float = 4f,
    val shadowRadius: Float = 12f,
    val backgroundBadgeHex: String = "",
    val uppercase: Boolean = true,
    val letterSpacing: Float = 1.5f,
    val lineHeightMultiplier: Float = 1.0f,
    val textAlign: String = "LEFT", // LEFT, CENTER, RIGHT
    // Shape/Subject properties
    val shapeKind: ShapeKind = ShapeKind.RECTANGLE,
    val fillColorHex: String = "#EF4444",
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val rimLightHex: String = "",
    val bitmap: Bitmap? = null
) {
    fun serialize(): String {
        val safeName = name.replace("|", " ").take(40)
        val safeText = text.replace("|", " ").replace("\n", "\\n").take(120)
        return listOf(
            id.take(24),
            safeName,
            type.name,
            xPercent.toInt().toString(),
            yPercent.toInt().toString(),
            widthPercent.toInt().toString(),
            heightPercent.toInt().toString(),
            rotation.toInt().toString(),
            (opacity * 100).toInt().toString(),
            visible.toString(),
            locked.toString(),
            zIndex.toString(),
            safeText,
            fontName,
            fontSizeSp.toInt().toString(),
            textColorHex,
            outlineColorHex,
            backgroundBadgeHex,
            uppercase.toString(),
            shapeKind.name,
            fillColorHex,
            textAlign
        ).joinToString("|").take(480)
    }

    companion object {
        fun deserialize(encoded: String): CanvasLayer? {
            val parts = encoded.split("|")
            if (parts.size < 12) return null
            return try {
                val type = runCatching { LayerType.valueOf(parts[2]) }.getOrDefault(LayerType.TEXT)
                CanvasLayer(
                    id = parts[0],
                    name = parts[1],
                    type = type,
                    xPercent = parts.getOrNull(3)?.toFloatOrNull() ?: 8f,
                    yPercent = parts.getOrNull(4)?.toFloatOrNull() ?: 20f,
                    widthPercent = parts.getOrNull(5)?.toFloatOrNull() ?: 55f,
                    heightPercent = parts.getOrNull(6)?.toFloatOrNull() ?: 22f,
                    rotation = parts.getOrNull(7)?.toFloatOrNull() ?: 0f,
                    opacity = (parts.getOrNull(8)?.toFloatOrNull() ?: 100f) / 100f,
                    visible = parts.getOrNull(9)?.toBooleanStrictOrNull() ?: true,
                    locked = parts.getOrNull(10)?.toBooleanStrictOrNull() ?: false,
                    zIndex = parts.getOrNull(11)?.toIntOrNull() ?: 0,
                    text = parts.getOrNull(12)?.replace("\\n", "\n") ?: "",
                    fontName = parts.getOrNull(13) ?: StudioFont.BEBAS_NEUE.displayName,
                    fontSizeSp = parts.getOrNull(14)?.toFloatOrNull() ?: 56f,
                    textColorHex = parts.getOrNull(15) ?: "#FFFFFF",
                    outlineColorHex = parts.getOrNull(16) ?: "#000000",
                    backgroundBadgeHex = parts.getOrNull(17) ?: "",
                    uppercase = parts.getOrNull(18)?.toBooleanStrictOrNull() ?: true,
                    shapeKind = runCatching { ShapeKind.valueOf(parts.getOrNull(19) ?: "RECTANGLE") }
                        .getOrDefault(ShapeKind.RECTANGLE),
                    fillColorHex = parts.getOrNull(20) ?: "#EF4444",
                    textAlign = parts.getOrNull(21) ?: "LEFT"
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
