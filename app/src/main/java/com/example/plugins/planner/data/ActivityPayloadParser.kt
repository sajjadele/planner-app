package com.example.plugins.planner.data

/**
 * ActivityPayloadParser — Factory for creating ActivityDraft instances.
 *
 * Phase 4.7.1 + 4.10.1 (Domain Separation):
 * - Creates rich drafts with attachments
 * - Decoupled from UI and ViewModel
 * - Future-ready for unified composer
 *
 * Usage:
 * // For activities
 * val draft = ActivityPayloadParser.note("My note")
 * val draft = ActivityPayloadParser.image(uri, "Description")
 * viewModel.createActivity(draft)
 *
 * // For steps
 * val stepDraft = StepDraft(title = "My step")
 * viewModel.createStep(stepDraft)
 */
object ActivityPayloadParser {

    /**
     * Create a NOTE draft.
     * Maps to: NOTE_ADDED event
     */
    fun note(text: String): ActivityDraft = ActivityDraft(
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
        text = title,
        durationMinutes = durationMinutes
    )

    /**
     * Create an IMAGE draft.
     * Maps to: NOTE_ADDED event
     * Attachments are part of the activity payload, not separate events.
     */
    fun image(
        uri: String,
        description: String? = null
    ): ActivityDraft = ActivityDraft(
        text = description,
        attachments = listOf(ActivityAttachment.Image(uri))
    )
}
