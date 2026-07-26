package com.example.plugins.planner.data

/**
 * ActivityMessageModel — UI-independent read model for activity messages.
 *
 * Phase 4.12: Activity Message Domain Refactor
 *
 * Architecture:
 * - Decouples UI from database entities
 * - Decouples UI from raw event types (NOTE_ADDED, IMAGE_ADDED, etc.)
 * - Never exposes system events (STEP_CREATED, STEP_COMPLETED, etc.)
 * - Provides clean, typed access to user-created message content
 * - Supports future Telegram-style interactions (edit, delete, reply)
 *
 * Flow:
 * ```
 * ActivityEventEntity
 *       ↓  (ActivityMessageMapper.toMessage() — returns null for system events)
 * ActivityMessageModel
 *       ↓
 * StepCard / TimelineBottomSheet / ActivityMessageCard
 * ```
 *
 * Storage:
 * - id: comes from ActivityEventEntity.id
 * - taskId: comes from ActivityEventEntity.taskId (for AI context in future phases)
 * - stepId: comes from ActivityEventEntity.stepId (nullable, null for task-level messages)
 * - text: decoded from ActivityPayload.text or legacy format
 * - attachments: decoded from ActivityPayload.attachments
 * - durationMinutes: decoded from ActivityPayload.durationMinutes or legacy format
 * - isEditable: true for user-created messages (future: role-based)
 * - isDeleted: soft-delete flag (future)
 * - replyToMessageId: for threading (future Phase 6)
 */
data class ActivityMessageModel(
    /** Unique identifier from ActivityEventEntity. */
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

    /** Timestamp of the message event. */
    val timestamp: Long,

    /** Whether this message can be edited by the user. */
    val isEditable: Boolean = true,

    /** Whether this message has been soft-deleted. */
    val isDeleted: Boolean = false,

    /** ID of the message this replies to. Null for top-level messages. */
    val replyToMessageId: Long? = null
)
