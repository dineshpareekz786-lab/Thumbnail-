package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveProject_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val sampleProject = ThumbnailProject(
            userId = aliceUid,
            title = "AI Documentary Thumbnail",
            videoTopic = "How Quantum Computers Break Encryption",
            headline = "QUANTUM THREAT",
            subheadline = "THE END OF PRIVACY",
            category = VideoCategory.SCIENCE.displayName,
            visualStyle = VisualStylePreset.SCIENTIFIC_VISUALIZATION.displayName,
            realism = ImageRealismMode.PHOTOREALISTIC.displayName,
            aspectRatio = ThumbnailAspectRatio.YOUTUBE_16_9.ratioLabel,
            compositionPreset = CompositionPreset.SUBJECT_RIGHT_HEADLINE_LEFT.displayName,
            width = 1280L,
            height = 720L,
            layersSummary = listOf("TEXT|Headline|8|22|55|20|0|100|true|false|0|QUANTUM THREAT")
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(sampleProject)
        }
        assertTrue("Create project should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val projectId = createResult.getOrThrow()
        assertTrue(projectId.isNotBlank())

        val emittedProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(aliceUid).first { list -> list.any { it.id == projectId } }
        }
        assertTrue(emittedProjects.any { it.id == projectId && it.headline == "QUANTUM THREAT" })
    }

    @Test
    fun getProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioRepository(firestore)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProject(
                ThumbnailProject(
                    userId = aliceUid,
                    title = "Alice Private Thumbnail",
                    videoTopic = "Secret Investigation"
                )
            ).getOrThrow()
        }

        // Bob signs in and tries to read Alice's project
        signInTestUser(BOB_EMAIL)
        val bobRepo = StudioRepository(firestore)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProjectById(aliceUid, projectId)
        }
        assertTrue("Bob reading Alice's project must fail", bobReadResult.isFailure)
        val ex = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = StudioRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProjects(aliceUid).first()
            }
            fail("Expected unauthenticated observeProjects to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun brandKitAndHistory_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val kitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveBrandKit(
                BrandKit(
                    userId = aliceUid,
                    brandName = "Apex Docs",
                    primaryFont = StudioFont.BEBAS_NEUE.displayName,
                    secondaryFont = StudioFont.MONTSERRAT.displayName
                )
            )
        }
        assertTrue("Save brand kit should succeed", kitResult.isSuccess)

        val histResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addHistoryItem(
                GenerationHistoryItem(
                    userId = aliceUid,
                    videoTopic = "The Fall of Rome",
                    suggestedHeadline = "EMPIRE COLLAPSE",
                    generatedPrompt = "Cinematic historical scene of Roman forum at dusk"
                )
            )
        }
        assertTrue("Add history item should succeed", histResult.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@thumbnailstudio.test"
        const val BOB_EMAIL = "bob@thumbnailstudio.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
