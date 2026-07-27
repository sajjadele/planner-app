package com.example.plugins.planner.data

/**
 * ActivityMessageCapability — Describes what interactions are allowed on a message.
 *
 * Phase 4.14: Identity & Interaction Foundation
 *
 * Separates capability (what CAN be done) from state (what IS the current condition).
 * This abstraction allows the UI layer to decide which actions to show without
 * coupling to the message model.
 *
 * Example:
 * ```kotlin
 * val capability = message.capability()
 * if (capability.canEdit) { showEditButton() }
 * if (capability.canDelete) { showDeleteButton() }
 * ```
 *
 * Future: capability calculation can be based on:
 * - User role (admin vs member)
 * - Message ownership
 * - Message age
 * - Task status
 * - Organization policies
 */
data class ActivityMessageCapability(
    /** Whether the user can edit this message. */
    val canEdit: Boolean = true,

    /** Whether the user can delete this message. */
    val canDelete: Boolean = false,

    /** Whether the user can reply to this message. */
    val canReply: Boolean = true
) {
    companion object {
        /** Full capabilities — owner or admin. */
        val FULL = ActivityMessageCapability(
            canEdit = true,
            canDelete = true,
            canReply = true
        )

        /** Read-only capability — viewer or archived message. */
        val READ_ONLY = ActivityMessageCapability(
            canEdit = false,
            canDelete = false,
            canReply = false
        )

        /** Reply-only capability — can't edit or delete. */
        val REPLY_ONLY = ActivityMessageCapability(
            canEdit = false,
            canDelete = false,
            canReply = true
        )

        /** Default capability for active messages. */
        val DEFAULT = ActivityMessageCapability(
            canEdit = true,
            canDelete = true,
            canReply = true
        )
    }
}
