package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Projects ---

    fun observeProjects(userId: String): Flow<List<ThumbnailProject>> {
        val path = "users/$userId/projects"
        return db.collection("users").document(userId).collection("projects")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ThumbnailProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createProject(project: ThumbnailProject, customId: String? = null): Result<String> {
        val uid = requireUserId()
        val projectId = customId ?: "proj_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        val payload = mapOf(
            "userId" to uid,
            "title" to project.title.ifBlank { "Untitled Thumbnail" }.take(150),
            "videoTopic" to project.videoTopic.ifBlank { "General Video Topic" }.take(500),
            "headline" to project.headline.take(120),
            "subheadline" to project.subheadline.take(150),
            "category" to project.category.take(60),
            "visualStyle" to project.visualStyle.take(60),
            "realism" to project.realism.take(60),
            "aspectRatio" to project.aspectRatio.take(30),
            "compositionPreset" to project.compositionPreset.take(80),
            "width" to project.width.coerceIn(100L, 4096L),
            "height" to project.height.coerceIn(100L, 4096L),
            "aiPrompt" to project.aiPrompt.take(2500),
            "artDirectionSummary" to project.artDirectionSummary.take(2000),
            "headlineFont" to project.headlineFont.take(60),
            "headlineColorHex" to project.headlineColorHex.take(12),
            "accentColorHex" to project.accentColorHex.take(12),
            "headlineSizeSp" to project.headlineSizeSp.coerceIn(12L, 160L),
            "headlineX" to project.headlineX.coerceIn(0L, 100L),
            "headlineY" to project.headlineY.coerceIn(0L, 100L),
            "exposure" to project.exposure.coerceIn(-100L, 100L),
            "contrast" to project.contrast.coerceIn(-100L, 100L),
            "saturation" to project.saturation.coerceIn(-100L, 100L),
            "temperature" to project.temperature.coerceIn(-100L, 100L),
            "vignette" to project.vignette.coerceIn(0L, 100L),
            "backgroundBlur" to project.backgroundBlur.coerceIn(0L, 100L),
            "localImagePath" to project.localImagePath.take(500),
            "layersSummary" to project.layersSummary.take(25).map { it.take(500) },
            "isTemplate" to project.isTemplate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProject(project: ThumbnailProject): Result<Unit> {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
        val updates = mapOf(
            "title" to project.title.ifBlank { "Untitled Thumbnail" }.take(150),
            "videoTopic" to project.videoTopic.ifBlank { "General Video Topic" }.take(500),
            "headline" to project.headline.take(120),
            "subheadline" to project.subheadline.take(150),
            "category" to project.category.take(60),
            "visualStyle" to project.visualStyle.take(60),
            "realism" to project.realism.take(60),
            "aspectRatio" to project.aspectRatio.take(30),
            "compositionPreset" to project.compositionPreset.take(80),
            "width" to project.width.coerceIn(100L, 4096L),
            "height" to project.height.coerceIn(100L, 4096L),
            "aiPrompt" to project.aiPrompt.take(2500),
            "artDirectionSummary" to project.artDirectionSummary.take(2000),
            "headlineFont" to project.headlineFont.take(60),
            "headlineColorHex" to project.headlineColorHex.take(12),
            "accentColorHex" to project.accentColorHex.take(12),
            "headlineSizeSp" to project.headlineSizeSp.coerceIn(12L, 160L),
            "headlineX" to project.headlineX.coerceIn(0L, 100L),
            "headlineY" to project.headlineY.coerceIn(0L, 100L),
            "exposure" to project.exposure.coerceIn(-100L, 100L),
            "contrast" to project.contrast.coerceIn(-100L, 100L),
            "saturation" to project.saturation.coerceIn(-100L, 100L),
            "temperature" to project.temperature.coerceIn(-100L, 100L),
            "vignette" to project.vignette.coerceIn(0L, 100L),
            "backgroundBlur" to project.backgroundBlur.coerceIn(0L, 100L),
            "localImagePath" to project.localImagePath.take(500),
            "layersSummary" to project.layersSummary.take(25).map { it.take(500) },
            "isTemplate" to project.isTemplate,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getProjectById(ownerUid: String, projectId: String): Result<ThumbnailProject?> {
        val docRef = db.collection("users").document(ownerUid).collection("projects").document(projectId)
        return try {
            val snap = docRef.get().await()
            val proj = snap.toObject(ThumbnailProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(proj)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Generation History ---

    fun observeHistory(userId: String): Flow<List<GenerationHistoryItem>> {
        val path = "users/$userId/history"
        return db.collection("users").document(userId).collection("history")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GenerationHistoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addHistoryItem(item: GenerationHistoryItem): Result<String> {
        val uid = requireUserId()
        val historyId = "hist_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("users").document(uid).collection("history").document(historyId)
        val payload = mapOf(
            "userId" to uid,
            "videoTopic" to item.videoTopic.ifBlank { "Untitled Topic" }.take(500),
            "suggestedHeadline" to item.suggestedHeadline.take(120),
            "category" to item.category.take(60),
            "visualStyle" to item.visualStyle.take(60),
            "realism" to item.realism.take(60),
            "aspectRatio" to item.aspectRatio.take(30),
            "compositionPreset" to item.compositionPreset.take(80),
            "mainSubject" to item.mainSubject.take(500),
            "lightingSetup" to item.lightingSetup.take(500),
            "colorPalette" to item.colorPalette.take(500),
            "generatedPrompt" to item.generatedPrompt.ifBlank { "Photorealistic thumbnail" }.take(2500),
            "modelUsed" to item.modelUsed.take(80),
            "localImagePath" to item.localImagePath.take(500),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(historyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHistoryItem(historyId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("history").document(historyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Brand Kits ---

    fun observeBrandKits(userId: String): Flow<List<BrandKit>> {
        val path = "users/$userId/brand_kits"
        return db.collection("users").document(userId).collection("brand_kits")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BrandKit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveBrandKit(kit: BrandKit): Result<String> {
        val uid = requireUserId()
        val kitId = kit.id.ifBlank { "kit_${UUID.randomUUID().toString().replace("-", "").take(20)}" }
        val docRef = db.collection("users").document(uid).collection("brand_kits").document(kitId)
        val payload = mapOf(
            "userId" to uid,
            "brandName" to kit.brandName.ifBlank { "Channel Brand" }.take(100),
            "primaryFont" to kit.primaryFont.take(60),
            "secondaryFont" to kit.secondaryFont.take(60),
            "primaryColorHex" to kit.primaryColorHex.take(12),
            "secondaryColorHex" to kit.secondaryColorHex.take(12),
            "accentColorHex" to kit.accentColorHex.take(12),
            "defaultStyle" to kit.defaultStyle.take(60),
            "channelTag" to kit.channelTag.take(80),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(kitId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBrandKit(kitId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("brand_kits").document(kitId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- User Profile & Preferences ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun saveUserProfile(profile: UserProfile, isCreate: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            if (isCreate) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "Creator" }.take(100),
                    "defaultCategory" to profile.defaultCategory.take(60),
                    "defaultStyle" to profile.defaultStyle.take(60),
                    "defaultRealism" to profile.defaultRealism.take(60),
                    "defaultAspectRatio" to profile.defaultAspectRatio.take(30),
                    "preferredAiModel" to profile.preferredAiModel.take(80),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profile.email?.takeIf { it.isNotBlank() }?.let { payload["email"] = it.take(200) }
                profile.photoUrl?.takeIf { it.isNotBlank() }?.let { payload["photoUrl"] = it.take(500) }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to profile.displayName.ifBlank { "Creator" }.take(100),
                    "defaultCategory" to profile.defaultCategory.take(60),
                    "defaultStyle" to profile.defaultStyle.take(60),
                    "defaultRealism" to profile.defaultRealism.take(60),
                    "defaultAspectRatio" to profile.defaultAspectRatio.take(30),
                    "preferredAiModel" to profile.preferredAiModel.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profile.email?.takeIf { it.isNotBlank() }?.let { updates["email"] = it.take(200) }
                profile.photoUrl?.takeIf { it.isNotBlank() }?.let { updates["photoUrl"] = it.take(500) }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
