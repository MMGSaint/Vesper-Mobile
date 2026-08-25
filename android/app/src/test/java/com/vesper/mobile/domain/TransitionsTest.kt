package com.vesper.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
