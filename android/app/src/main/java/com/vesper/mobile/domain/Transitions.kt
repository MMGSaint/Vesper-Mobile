package com.vesper.mobile.domain

/**
 * Legal staging-state transitions and UI actions.
 * Client displays and offers only these. Never invent extra paths.
 *
 * Distinctions preserved:
 * staging ≠ canon, approve ≠ publish, tease ≠ reveal, trigger ≠ reveal,
 * seal ≠ publication, dataset ≠ binary, DM ≠ player client.
 * Windows = NO BINARY RELEASE. Android player = NO BINARY RELEASE.
 * DM = NOT PLAYER CLIENT. Signing key NEVER on device.
 */
object Transitions {

    val states: List<String> = listOf(
        "INGESTED",
        "STAGED",
        "UNDER_REVIEW",
        "APPROVED",
        "PREPARED",
        "SCHEDULED",
        "HIDDEN",
        "SEALED",
        "REJECTED",
        "RELEASE_CANDIDATE",
    )

    private val graph: Map<String, Set<String>> = mapOf(
        "INGESTED" to setOf("STAGED", "UNDER_REVIEW", "REJECTED", "HIDDEN"),
        "STAGED" to setOf("UNDER_REVIEW", "APPROVED", "REJECTED", "HIDDEN"),
        "UNDER_REVIEW" to setOf("APPROVED", "REJECTED", "HIDDEN", "STAGED"),
        "APPROVED" to setOf("PREPARED", "HIDDEN", "REJECTED", "UNDER_REVIEW"),
        "PREPARED" to setOf("SCHEDULED", "SEALED", "HIDDEN", "APPROVED"),
        "SCHEDULED" to setOf("SEALED", "PREPARED"),
        "HIDDEN" to setOf("STAGED", "UNDER_REVIEW", "APPROVED", "PREPARED"),
        "SEALED" to setOf("RELEASE_CANDIDATE"),
        "REJECTED" to emptySet(),
        "RELEASE_CANDIDATE" to emptySet(),
    )

    private val actions: Map<String, List<LegalAction>> = mapOf(
        "INGESTED" to listOf(
            LegalAction.REVIEW, LegalAction.EDIT, LegalAction.APPROVE,
            LegalAction.REJECT, LegalAction.HIDE,
        ),
        "STAGED" to listOf(
            LegalAction.REVIEW, LegalAction.EDIT, LegalAction.APPROVE,
            LegalAction.REJECT, LegalAction.HIDE,
        ),
        "UNDER_REVIEW" to listOf(
            LegalAction.APPROVE, LegalAction.REJECT, LegalAction.HIDE,
            LegalAction.EDIT, LegalAction.TEASE,
        ),
        "APPROVED" to listOf(
            LegalAction.PREPARE, LegalAction.TEASE, LegalAction.HIDE,
            LegalAction.REJECT, LegalAction.EDIT,
        ),
        "PREPARED" to listOf(
            LegalAction.SCHEDULE, LegalAction.SEAL, LegalAction.TEASE, LegalAction.HIDE,
        ),
        "SCHEDULED" to listOf(LegalAction.SEAL),
        "HIDDEN" to listOf(LegalAction.UNHIDE),
        "SEALED" to listOf(LegalAction.TEASE),
        "REJECTED" to emptyList(),
        "RELEASE_CANDIDATE" to emptyList(),
    )

    fun legalNext(state: String): Set<String> =
        graph[normalize(state)].orEmpty()

    fun legalActions(state: String): List<LegalAction> =
        actions[normalize(state)].orEmpty()

    fun canTransition(from: String, to: String): Boolean =
        legalNext(from).contains(normalize(to))

    fun normalize(state: String): String = state.trim().uppercase().replace(' ', '_')

    /** Confirm (step-up) op required for these staging verbs. */
    fun confirmOp(action: LegalAction): String? = when (action) {
        LegalAction.APPROVE -> "stage.approve"
        LegalAction.REJECT -> "stage.reject"
        LegalAction.HIDE -> "stage.hide"
        LegalAction.SEAL -> "stage.seal"
        else -> null
    }
}

enum class LegalAction(val path: String, val label: String, val privileged: Boolean) {
    REVIEW("review", "REVIEW", false),
    EDIT("edit", "EDIT", false),
    APPROVE("approve", "APPROVE", true),
    REJECT("reject", "REJECT", true),
    HIDE("hide", "HIDE", true),
    UNHIDE("unhide", "UNHIDE", false),
    PREPARE("prepare", "PREPARE", false),
    TEASE("tease", "TEASE", false),
    SCHEDULE("schedule", "SCHEDULE", false),
    SEAL("seal", "SEAL", true),
}

enum class ChannelKind {
    DEV, TEST, PREVIEW, STABLE;

    companion object {
        fun parse(raw: String): ChannelKind? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}

object ChannelRules {
    /** STABLE assign requires confirm channel.assign */
    const val ASSIGN_CONFIRM = "channel.assign"

    /** Promote always requires confirm channel.promote */
    const val PROMOTE_CONFIRM = "channel.promote"

    fun canSchedule(kind: ChannelKind): Boolean = kind != ChannelKind.STABLE

    fun assignRequiresConfirm(kind: ChannelKind): Boolean = kind == ChannelKind.STABLE
}

object ReleaseRules {
    const val IMPORT_CONFIRM = "release.import"
    const val PUBLISH_CONFIRM = "release.publish"
    const val PUBLISH_PHRASE = "PUBLISH"

    val handoffSteps: List<String> = listOf(
        "EXPORT UNSIGNED",
        "SIGN ON PC",
        "RETURN SIGNED",
        "IMPORT",
        "VERIFY",
        "PUBLISH",
    )
}

object LegalCopy {
    const val NO_BINARY_RELEASE = "NO BINARY RELEASE"
    const val DM_NOT_PLAYER = "DM IS NOT THE PLAYER CLIENT"
    const val SIGNING_KEY_OFF_DEVICE = "SIGNING KEY NEVER ON DEVICE"
    const val WINDOWS_NO_BINARY = "WINDOWS = NO BINARY RELEASE"
    const val ANDROID_PLAYER_NO_BINARY = "ANDROID PLAYER = NO BINARY RELEASE"
    const val STAGING_NE_CANON = "STAGING ≠ CANON"
    const val APPROVE_NE_PUBLISH = "APPROVE ≠ PUBLISH"
    const val TEASE_NE_REVEAL = "TEASE ≠ REVEAL"
    const val TRIGGER_NE_REVEAL = "TRIGGER ≠ REVEAL"
    const val SEAL_NE_PUBLICATION = "SEAL ≠ PUBLICATION"
    const val DATASET_NE_BINARY = "DATASET ≠ BINARY"
}
