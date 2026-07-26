package com.example.plugins.planner.data

/**
 * ActivityPayloadParser — Factory for creating ActivityDraft instances.
 *
 * Architecture (Phase 4.7.1):
 * - Creates rich drafts with attachments and intent
 * - Decoupled from UI and ViewModel
 * - Future-ready for unified composer
 *
 * Usage:
 * val draft = ActivityPayloadParser.step("My step title")
 * val draft = ActivityPayloadParser.image(uri, "Description")
 * viewModel.createActivity(draft)
 */
object ActivityPayloadParser {

    /**
     * Create a STEP draft.
     * Maps to: STEP_CREATED event
     */
    fun step(title: String): ActivityDraft = ActivityDraft(
        text = title,
        intent = ActivityIntent.STEP
    )

    /**
     * Create a NOTE draft.
     * Maps to: NOTE_ADDED event
     */
    fun note(text: String): ActivityDraft = ActivityDraft(
        text = text,
        intent = ActivityIntent.ACTIVITY
    )

    /**
     * Create a MANUAL_ACTIVITY draft.
     * Maps to: MANUAL_ACTIVITY event
     */
    fun manualActivity(
        title: String,
        durationMinutes: Int? = null
    ): ActivityDraft = ActivityDraft(
        text = title,
        durationMinutes = durationMinutes,
        intent = ActivityIntent.ACTIVITY
    )

    /**
     * Create an IMAGE draft.
     * Maps to: NOTE_ADDED or MANUAL_ACTIVITY (depending on metadata)
     * Attachments are part of the activity payload, not separate events.
     */
    fun image(
        uri: String,
        description: String? = null
    ): ActivityDraft = ActivityDraft(
        text = description,
        attachments = listOf(ActivityAttachment.Image(uri)),
        intent = ActivityIntent.ACTIVITY
    )
}
