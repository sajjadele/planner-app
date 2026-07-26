package com.example.plugins.planner.data

/**
 * ActivityPayloadParser — Factory for creating ActivityDraft instances.
 *
 * This abstraction:
 * - Decouples UI from activity creation logic
 * - Provides a single entry point for creating drafts
 * - Will evolve to handle unified composer drafts in the future
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
        type = ActivityDraftType.STEP,
        text = title
    )

    /**
     * Create a NOTE draft.
     * Maps to: NOTE_ADDED event
     */
    fun note(text: String): ActivityDraft = ActivityDraft(
        type = ActivityDraftType.NOTE,
        text = text
    )

    /**
     * Create a MANUAL_ACTIVITY draft.
     * Maps to: MANUAL_ACTIVITY event
     */
    fun manualActivity(
        title: String,
        durationMinutes: Int? = null
    ): ActivityDraft = ActivityDraft(
        type = ActivityDraftType.MANUAL_ACTIVITY,
        text = title,
        durationMinutes = durationMinutes
    )

    /**
     * Create an IMAGE draft.
     * Maps to: IMAGE_ADDED event
     */
    fun image(
        uri: String,
        description: String? = null
    ): ActivityDraft = ActivityDraft(
        type = ActivityDraftType.IMAGE,
        imageUri = uri,
        imageDescription = description
    )
}
