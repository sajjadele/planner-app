package com.example.plugins.planner.data

/**
 * ActivityMessageModel — UI-independent read model for activity messages.
 *
 * Phase 4.12: Activity Message Domain Refactor
 * Phase 4.14: Identity & Interaction Foundation
 *
 * Architecture:
 * - Decouples UI from database entities
 * - Decouples UI from raw event types (NOTE_ADDED, IMAGE_ADDED, etc.)
 * - Never exposes system events (STEP_CREATED, STEP_COMPLETED, etc.)
 * - Provides clean, typed access to user-created message content
 * - Fully prepared for Edit/Delete/Reply interactions (Phase 5+)
 *
 * Flow:
 * ```
 * ActivityEventEntity
 *       ↓  (ActivityMessageMapper.toMessage() — returns null for system events)
 * ActivityMessageModel  ← identity preserved: entity.id → model.id
 *       ↓
 * ActivityMessageCapability  ← what can this message do?
 *       ↓
 * ActivityMessageAction  ← what should happen when user interacts?
 *       ↓
 * StepCard / TimelineBottomSheet / ActivityMessageCard
 * ```
 *
 * Storage:
 * - id: comes from ActivityEventEntity.id (database identity, unique)
 * - taskId: comes from ActivityEventEntity.taskId (for AI context in future phases)
 * - stepId: comes from ActivityEventEntity.stepId (nullable, null for task-level messages)
 * - text: decoded from ActivityPayload.text or legacy format
 * - attachments: decoded from ActivityPayload.attachments
 * - durationMinutes: decoded from ActivityPayload.durationMinutes or legacy format
 * - createdAt: when the message was recorded
 * - canEdit: whether the user can edit this message
 * - canDelete: whether the user can delete this message
 * - isDeleted: soft-delete state flag
 * - replyToMessageId: for threading (Phase 6+), null for top-level messages
 */
data class ActivityMessageModel(
    /** Unique database identity from ActivityEventEntity.id. Maps 1:1 with entity. */
    val id: Long,

    /** Task this message belongs to. For AI context in future phases. */
    val taskId: Long,

    /** Step this message belongs to. Null for task-level messages. */
    val stepId: Long?,

    /** Text content of the message. Null if no text (e.g., image-only message). */
    val text: String?,

    /** List of attached files/images. Empty if no attachments. */
    val attachments: List<ActivityAttachment> = emptyList(),

    /** Duration in minutes. Null if no duration was recorded. */
    val durationMinutes: Int?,

    /** When the message was created (same as source entity timestamp). */
    val createdAt: Long,

    /** Whether the user can edit this message. */
    val canEdit: Boolean = true,

    /** Whether the user can delete this message. */
    val canDelete: Boolean = false,

    /** Soft-delete state flag. */
    val isDeleted: Boolean = false,

    /** ID of the message this replies to. Null for top-level messages. */
    val replyToMessageId: Long? = null,

    /** Whether this message has been edited after creation. */
    val isEdited: Boolean = false
) {
    /** @deprecated Use createdAt instead. Kept for backward compatibility. */
    @Deprecated(
        message = "Use createdAt instead",
        replaceWith = ReplaceWith("createdAt"),
        level = DeprecationLevel.HIDDEN
    )
    val timestamp: Long
        get() = createdAt

    /**
     * Derives ActivityMessageCapability from this message's current state.
     *
     * Calculation logic:
     * - canEdit: [canEdit] field (future: may also check ownership/role)
     * - canDelete: [canDelete] field (future: may also check message age)
     * - canReply: always true for non-deleted messages
     *
     * @return ActivityMessageCapability reflecting what interactions are allowed
     */
    fun capability(): ActivityMessageCapability {
        return ActivityMessageCapability(
            canEdit = canEdit && !isDeleted,
            canDelete = canDelete && !isDeleted,
            canReply = !isDeleted
        )
    }
}
