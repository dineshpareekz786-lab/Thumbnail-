package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.AiImageModel
import com.example.data.ArtDirectionPlan
import com.example.data.CompositionPreset
import com.example.data.ImageRealismMode
import com.example.data.ReferenceImageItem
import com.example.data.ThumbnailAspectRatio
import com.example.data.VideoCategory
import com.example.data.VisualStylePreset
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ThumbnailAiDirector"

data class AiEditPlan(
    val editCategory: String, // "CANVAS_GRADING", "TYPOGRAPHY", "IMAGE_REGENERATION"
    val explanation: String,
    val exposureDelta: Int = 0,
    val contrastDelta: Int = 0,
    val saturationDelta: Int = 0,
    val temperatureDelta: Int = 0,
    val vignetteDelta: Int = 0,
    val blurDelta: Int = 0,
    val newHeadlineText: String = "",
    val headlineSizeDelta: Int = 0,
    val headlineXTarget: Int = -1,
    val headlineYTarget: Int = -1,
    val newHeadlineColorHex: String = "",
    val newAccentColorHex: String = "",
    val imageEditInstruction: String = ""
)

class ThumbnailAiDirectorService {

    private val artDirectionSchema = Schema.obj(
        properties = mapOf(
            "mainSubject" to Schema.string("Detailed description of the primary subject, expression, posture, wardrobe, and texture"),
            "supportingSubjects" to Schema.string("Secondary visual storytelling elements or subjects"),
            "backgroundEnvironment" to Schema.string("Believable physical environment with architectural and material details"),
            "foregroundElements" to Schema.string("Subtle depth-enhancing foreground elements"),
            "cameraAngle" to Schema.string("Camera perspective and lens focal length (e.g., eye-level 50mm prime, low-angle 35mm)"),
            "shotType" to Schema.string("Framing shot type (e.g., medium close-up, environmental portrait)"),
            "lightingSetup" to Schema.string("Professional cinema lighting setup with key, fill, and rim light"),
            "colorPalette" to Schema.string("Specific color harmony and contrast plan"),
            "mood" to Schema.string("Atmospheric mood of the scene"),
            "emotionalTone" to Schema.string("Viewer emotional response and facial expression tone"),
            "composition" to Schema.string("Rule-of-thirds and spatial placement strategy"),
            "textPlacement" to Schema.string("Optimal zone for headline placement without obscuring subject"),
            "negativeSpace" to Schema.string("Where uncluttered negative space is reserved for mobile readability"),
            "depthOfField" to Schema.string("Aperture and background bokeh/separation strategy"),
            "visualHierarchy" to Schema.string("1st, 2nd, 3rd focal priority in the frame"),
            "realismRequirements" to Schema.string("Photographic texture, skin pores, contact shadows, optical realism rules"),
            "thingsToAvoid" to Schema.string("Elements to exclude (e.g., plastic skin, clutter, fake text, watermarks)"),
            "suggestedHeadlines" to Schema.array(
                items = Schema.string(),
                description = "4 concise, punchy, factual 2-4 word thumbnail headlines"
            ),
            "recommendedSubheadline" to Schema.string("Short 2-5 word supporting subheadline"),
            "recommendedCompositionPreset" to Schema.string("Best matching composition preset name"),
            "recommendedTextColorHex" to Schema.string("High-contrast hex color for headline text, e.g. #FFFFFF"),
            "recommendedAccentColorHex" to Schema.string("High-contrast hex color for accent/badge, e.g. #F59E0B or #EF4444"),
            "masterImagePrompt" to Schema.string("Complete, cohesive photographic prompt for generating the background/subject scene WITHOUT rendered text")
        )
    )

    private val headlineSuggestionsSchema = Schema.obj(
        properties = mapOf(
            "headlines" to Schema.array(
                items = Schema.string(),
                description = "5 short, powerful, readable 2-4 word thumbnail headlines without clickbait lies"
            ),
            "subheadlines" to Schema.array(
                items = Schema.string(),
                description = "3 short supporting subheadlines"
            ),
            "rationale" to Schema.string("Brief explanation of why these headlines work on mobile feeds")
        )
    )

    private val aiEditSchema = Schema.obj(
        properties = mapOf(
            "editCategory" to Schema.enumeration(
                values = listOf("CANVAS_GRADING", "TYPOGRAPHY", "IMAGE_REGENERATION"),
                description = "Whether the request adjusts grading/transform, typography/text, or requires AI image regeneration/editing"
            ),
            "explanation" to Schema.string("Brief confirmation of the changes applied"),
            "exposureDelta" to Schema.integer("Change to exposure (-100 to 100), 0 if unchanged"),
            "contrastDelta" to Schema.integer("Change to contrast (-100 to 100), 0 if unchanged"),
            "saturationDelta" to Schema.integer("Change to saturation (-100 to 100), 0 if unchanged"),
            "temperatureDelta" to Schema.integer("Change to color temperature (-100 cool to 100 warm), 0 if unchanged"),
            "vignetteDelta" to Schema.integer("Change to vignette (0 to 100), 0 if unchanged"),
            "blurDelta" to Schema.integer("Change to background blur (0 to 100), 0 if unchanged"),
            "newHeadlineText" to Schema.string("New headline text if requested, or empty string if unchanged"),
            "headlineSizeDelta" to Schema.integer("Change to headline font size (-40 to 40), 0 if unchanged"),
            "headlineXTarget" to Schema.integer("New normalized X position (0-80) if moved, or -1 if unchanged"),
            "headlineYTarget" to Schema.integer("New normalized Y position (0-85) if moved, or -1 if unchanged"),
            "newHeadlineColorHex" to Schema.string("New hex color for headline if requested, or empty string"),
            "newAccentColorHex" to Schema.string("New hex color for accent/badge if requested, or empty string"),
            "imageEditInstruction" to Schema.string("Detailed photographic prompt if editCategory is IMAGE_REGENERATION, else empty string")
        )
    )

    private val artDirectorModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = artDirectionSchema
            }
        )
    }

    private val headlineModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = headlineSuggestionsSchema
            }
        )
    }

    private val editAssistantModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = aiEditSchema
            }
        )
    }

    private fun getImageModel(modelChoice: AiImageModel): GenerativeModel {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        return Firebase.ai.generativeModel(
            modelName = modelChoice.modelId,
            generationConfig = config
        )
    }

    suspend fun suggestHeadlines(
        videoTopic: String,
        category: VideoCategory,
        style: VisualStylePreset
    ): Result<Pair<List<String>, List<String>>> {
        return try {
            val prompt = """
                You are a senior YouTube thumbnail strategist for high-retention ${category.displayName} channels in the "${style.displayName}" style.
                Video Topic: "$videoTopic"
                
                Rules:
                - Prefer 2 to 4 impactful words per headline for instant mobile readability.
                - Preserve the factual meaning of the topic without inventing fake statistics or unsupported claims.
                - Avoid copying the entire video title verbatim.
                - Avoid excessive punctuation.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                headlineModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty response from headline AI.")
            val json = JSONObject(jsonText)
            val headlinesArr = json.optJSONArray("headlines")
            val subArr = json.optJSONArray("subheadlines")
            val headlines = mutableListOf<String>()
            val subs = mutableListOf<String>()
            if (headlinesArr != null) {
                for (i in 0 until headlinesArr.length()) {
                    headlines.add(headlinesArr.getString(i))
                }
            }
            if (subArr != null) {
                for (i in 0 until subArr.length()) {
                    subs.add(subArr.getString(i))
                }
            }
            Result.success(headlines to subs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "suggestHeadlines failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeTopicAndCreatePlan(
        videoTopic: String,
        userHeadline: String,
        category: VideoCategory,
        style: VisualStylePreset,
        realism: ImageRealismMode,
        aspectRatio: ThumbnailAspectRatio,
        compositionPreset: CompositionPreset,
        referenceImages: List<ReferenceImageItem>,
        additionalInstructions: String
    ): Result<ArtDirectionPlan> {
        return try {
            val referencesDesc = if (referenceImages.isEmpty()) {
                "No uploaded reference images."
            } else {
                referenceImages.mapIndexed { idx, item ->
                    "Reference #${idx + 1} (${item.label}): ${item.usageOption.displayName} — ${item.usageOption.instructionPrefix}"
                }.joinToString("\n")
            }

            val promptText = """
                Act as a world-class YouTube Thumbnail Art Director and Director of Photography.
                Analyze this video project and produce a complete, production-ready Art Direction Plan.
                
                Video Topic: "$videoTopic"
                Draft Headline: "${userHeadline.ifBlank { "Auto-suggest" }}"
                Video Category: ${category.displayName} (${category.artDirectionHint})
                Visual Style Preset: ${style.displayName} — ${style.shortDescription}
                Lighting Guide: ${style.lightingGuide}
                Realism Mode: ${realism.displayName} (${realism.promptDirective})
                Target Aspect Ratio: ${aspectRatio.displayName} (${aspectRatio.ratioLabel}, ${aspectRatio.defaultWidth}x${aspectRatio.defaultHeight})
                Selected Composition Layout: ${compositionPreset.displayName} — ${compositionPreset.promptLayoutDirective}
                Reference Images Provided:
                $referencesDesc
                Additional User Instructions: "${additionalInstructions.ifBlank { "None" }}"
                
                CRITICAL ART DIRECTION RULES:
                1. The generated image must look professionally photographed, lit, and composited by an experienced human art director, NOT generic AI art.
                2. Prioritize natural facial proportions, realistic skin pores/texture, accurate anatomy, natural eyes, realistic contact shadows, and authentic environmental textures.
                3. Reserve clear, unobstructed negative space for the typography engine according to the composition layout (${compositionPreset.displayName}).
                4. Do NOT ask the image generator to render text, logos, watermarks, or UI overlays into the image pixels; our Smart Typography Engine renders crisp vector text on top.
                5. Do NOT fabricate fake documents, news chyrons, or unverifiable claims about real people.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                if (referenceImages.isEmpty()) {
                    artDirectorModel.generateContent(promptText)
                } else {
                    artDirectorModel.generateContent(
                        content {
                            referenceImages.take(3).forEach { ref ->
                                image(ref.bitmap)
                            }
                            text(promptText)
                        }
                    )
                }
            }

            val jsonString = response.text ?: throw IllegalStateException("Art Director returned empty response.")
            val json = JSONObject(jsonString)
            val suggestedList = mutableListOf<String>()
            val arr = json.optJSONArray("suggestedHeadlines")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    suggestedList.add(arr.getString(i))
                }
            }

            val plan = ArtDirectionPlan(
                mainSubject = json.optString("mainSubject", ""),
                supportingSubjects = json.optString("supportingSubjects", ""),
                backgroundEnvironment = json.optString("backgroundEnvironment", ""),
                foregroundElements = json.optString("foregroundElements", ""),
                cameraAngle = json.optString("cameraAngle", ""),
                shotType = json.optString("shotType", ""),
                lightingSetup = json.optString("lightingSetup", ""),
                colorPalette = json.optString("colorPalette", ""),
                mood = json.optString("mood", ""),
                emotionalTone = json.optString("emotionalTone", ""),
                composition = json.optString("composition", ""),
                textPlacement = json.optString("textPlacement", ""),
                negativeSpace = json.optString("negativeSpace", ""),
                depthOfField = json.optString("depthOfField", ""),
                visualHierarchy = json.optString("visualHierarchy", ""),
                realismRequirements = json.optString("realismRequirements", ""),
                thingsToAvoid = json.optString("thingsToAvoid", ""),
                suggestedHeadlines = suggestedList,
                recommendedSubheadline = json.optString("recommendedSubheadline", ""),
                recommendedCompositionPreset = json.optString(
                    "recommendedCompositionPreset",
                    compositionPreset.displayName
                ),
                recommendedTextColorHex = json.optString("recommendedTextColorHex", "#FFFFFF"),
                recommendedAccentColorHex = json.optString(
                    "recommendedAccentColorHex",
                    style.accentColorHex
                ),
                masterImagePrompt = json.optString("masterImagePrompt", "")
            )
            Result.success(plan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "analyzeTopicAndCreatePlan failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateThumbnailImage(
        plan: ArtDirectionPlan,
        realism: ImageRealismMode,
        style: VisualStylePreset,
        aspectRatio: ThumbnailAspectRatio,
        compositionPreset: CompositionPreset,
        referenceImages: List<ReferenceImageItem>,
        additionalInstructions: String,
        modelChoice: AiImageModel
    ): Result<Bitmap> {
        return try {
            val imageModel = getImageModel(modelChoice)
            val fullPhotographicPrompt = buildString {
                append("Create a professional, art-directed YouTube thumbnail photograph (NO TEXT, NO LETTERS, NO WORDS, NO LOGOS, NO WATERMARK in the image). ")
                append("Aspect Ratio Framing: ${aspectRatio.ratioLabel} (${aspectRatio.displayName}). ")
                append("Scene & Subject: ${plan.mainSubject}. ")
                if (plan.supportingSubjects.isNotBlank()) {
                    append("Supporting Elements: ${plan.supportingSubjects}. ")
                }
                append("Environment: ${plan.backgroundEnvironment}. ")
                append("Composition & Negative Space: ${compositionPreset.promptLayoutDirective} ${plan.negativeSpace}. ")
                append("Camera & Optics: ${plan.cameraAngle}, ${plan.shotType}, ${plan.depthOfField}. ")
                append("Lighting & Color Palette: ${plan.lightingSetup}. Colors: ${plan.colorPalette}. Mood: ${plan.mood}. ")
                append("Photographic Realism Directive: ${realism.promptDirective}. ${plan.realismRequirements}. ")
                if (additionalInstructions.isNotBlank()) {
                    append("Specific User Direction: $additionalInstructions. ")
                }
                append("Strictly Avoid: ${plan.thingsToAvoid}, any rendered text, typography, letters, numbers, watermarks, plastic skin, distorted hands, or cluttered focal points.")
            }

            val response = withTimeout(60_000L) {
                if (referenceImages.isEmpty()) {
                    imageModel.generateContent(fullPhotographicPrompt)
                } else {
                    imageModel.generateContent(
                        content {
                            referenceImages.take(3).forEach { ref ->
                                image(ref.bitmap)
                                text("Reference image (${ref.label}): ${ref.usageOption.instructionPrefix}.")
                            }
                            text(fullPhotographicPrompt)
                        }
                    )
                }
            }

            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("The image generation model did not return an image part. Ensure your Firebase project supports image generation for ${modelChoice.modelId}."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateThumbnailImage failed", e)
            Result.failure(e)
        }
    }

    suspend fun interpretAiEditCommand(
        userCommand: String,
        currentHeadline: String,
        currentStyle: String
    ): Result<AiEditPlan> {
        return try {
            val prompt = """
                You are the AI Editing Assistant inside Thumbnail AI Studio.
                The user wants to modify their current thumbnail design with this instruction:
                "$userCommand"
                
                Current Headline: "$currentHeadline"
                Current Style: "$currentStyle"
                
                Classify this instruction into one of:
                1. "CANVAS_GRADING": If the user asks to make the background darker/brighter, increase contrast, adjust saturation, make tones warmer/cooler/muted, add vignette, or blur the background.
                2. "TYPOGRAPHY": If the user asks to make the headline larger/smaller, move the text left/right/top/bottom, change the headline wording, or change text/accent colors.
                3. "IMAGE_REGENERATION": If the user asks to replace the background environment, move or change the photographed person/subject in the image, remove background objects, or re-light the scene with AI image-to-image editing.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                editAssistantModel.generateContent(prompt)
            }
            val jsonString = response.text ?: throw IllegalStateException("AI Edit Assistant returned empty response.")
            val json = JSONObject(jsonString)
            val plan = AiEditPlan(
                editCategory = json.optString("editCategory", "CANVAS_GRADING"),
                explanation = json.optString("explanation", "Applied requested adjustments."),
                exposureDelta = json.optInt("exposureDelta", 0),
                contrastDelta = json.optInt("contrastDelta", 0),
                saturationDelta = json.optInt("saturationDelta", 0),
                temperatureDelta = json.optInt("temperatureDelta", 0),
                vignetteDelta = json.optInt("vignetteDelta", 0),
                blurDelta = json.optInt("blurDelta", 0),
                newHeadlineText = json.optString("newHeadlineText", ""),
                headlineSizeDelta = json.optInt("headlineSizeDelta", 0),
                headlineXTarget = json.optInt("headlineXTarget", -1),
                headlineYTarget = json.optInt("headlineYTarget", -1),
                newHeadlineColorHex = json.optString("newHeadlineColorHex", ""),
                newAccentColorHex = json.optString("newAccentColorHex", ""),
                imageEditInstruction = json.optString("imageEditInstruction", "")
            )
            Result.success(plan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "interpretAiEditCommand failed", e)
            Result.failure(e)
        }
    }

    suspend fun editBitmapWithAi(
        currentBitmap: Bitmap,
        editInstruction: String,
        modelChoice: AiImageModel
    ): Result<Bitmap> {
        return try {
            val imageModel = getImageModel(modelChoice)
            val fullInstruction = "$editInstruction. Maintain photorealistic documentary quality, natural textures, realistic lighting, and do NOT render any text or watermarks onto the image."
            val response = withTimeout(60_000L) {
                imageModel.generateContent(
                    content {
                        image(currentBitmap)
                        text(fullInstruction)
                    }
                )
            }
            val editedBitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
            if (editedBitmap != null) {
                Result.success(editedBitmap)
            } else {
                Result.failure(IllegalStateException("AI image edit returned no image output."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "editBitmapWithAi failed", e)
            Result.failure(e)
        }
    }
}
