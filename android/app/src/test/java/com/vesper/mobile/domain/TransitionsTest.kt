package com.vesper.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionsTest {

    @Test
    fun stagedCannotGoToSealed() {
        assertFalse(Transitions.canTransition("STAGED", "SEALED"))
    }

    @Test
    fun approvedCannotGoToSealed() {
        assertFalse(Transitions.canTransition("APPROVED", "SEALED"))
    }

    @Test
    fun sealedCanGoToReleaseCandidate() {
        assertTrue(Transitions.canTransition("SEALED", "RELEASE_CANDIDATE"))
    }

    @Test
    fun preparedLegalActionsIncludeScheduleSealTeaseHide() {
        val actions = Transitions.legalActions("PREPARED")
        assertTrue(actions.contains(LegalAction.SCHEDULE))
        assertTrue(actions.contains(LegalAction.SEAL))
        assertTrue(actions.contains(LegalAction.TEASE))
        assertTrue(actions.contains(LegalAction.HIDE))
        assertEquals(
            listOf(LegalAction.SCHEDULE, LegalAction.SEAL, LegalAction.TEASE, LegalAction.HIDE),
            actions,
        )
    }

    @Test
    fun confirmOpApproveIsStageApprove() {
        assertEquals("stage.approve", Transitions.confirmOp(LegalAction.APPROVE))
    }

    @Test
    fun confirmOpSealIsStageSeal() {
        assertEquals("stage.seal", Transitions.confirmOp(LegalAction.SEAL))
    }

    @Test
    fun prepareDoesNotRequireConfirm() {
        assertNull(Transitions.confirmOp(LegalAction.PREPARE))
    }

    @Test
    fun teaseDoesNotRequireConfirm() {
        assertNull(Transitions.confirmOp(LegalAction.TEASE))
    }

    @Test
    fun stagedCannotGoToReleaseCandidate() {
        assertFalse(Transitions.canTransition("STAGED", "RELEASE_CANDIDATE"))
    }

    @Test
    fun approvedCannotGoToReleaseCandidate() {
        assertFalse(Transitions.canTransition("APPROVED", "RELEASE_CANDIDATE"))
    }

    @Test
    fun stableAssignRequiresConfirm() {
        assertTrue(ChannelRules.assignRequiresConfirm(ChannelKind.STABLE))
        assertFalse(ChannelRules.assignRequiresConfirm(ChannelKind.DEV))
        assertFalse(ChannelRules.canSchedule(ChannelKind.STABLE))
        assertTrue(ChannelRules.canSchedule(ChannelKind.PREVIEW))
    }

    @Test
    fun publishPhraseIsLiteralPublish() {
        assertEquals("PUBLISH", ReleaseRules.PUBLISH_PHRASE)
        assertEquals("release.publish", ReleaseRules.PUBLISH_CONFIRM)
        assertEquals("release.import", ReleaseRules.IMPORT_CONFIRM)
        assertEquals(
            listOf(
                "EXPORT UNSIGNED",
                "SIGN ON PC",
                "RETURN SIGNED",
                "IMPORT",
                "VERIFY",
                "PUBLISH",
            ),
            ReleaseRules.handoffSteps,
        )
    }

    @Test
    fun legalCopyPreservesDistinctions() {
        assertEquals("STAGING ≠ CANON", LegalCopy.STAGING_NE_CANON)
        assertEquals("APPROVE ≠ PUBLISH", LegalCopy.APPROVE_NE_PUBLISH)
        assertEquals("TEASE ≠ REVEAL", LegalCopy.TEASE_NE_REVEAL)
        assertEquals("TRIGGER ≠ REVEAL", LegalCopy.TRIGGER_NE_REVEAL)
        assertEquals("SEAL ≠ PUBLICATION", LegalCopy.SEAL_NE_PUBLICATION)
        assertEquals("DATASET ≠ BINARY", LegalCopy.DATASET_NE_BINARY)
        assertEquals("NO BINARY RELEASE", LegalCopy.NO_BINARY_RELEASE)
        assertEquals("DM IS NOT THE PLAYER CLIENT", LegalCopy.DM_NOT_PLAYER)
        assertEquals("SIGNING KEY NEVER ON DEVICE", LegalCopy.SIGNING_KEY_OFF_DEVICE)
    }
}
