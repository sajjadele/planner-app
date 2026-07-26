package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * StepDraftTest — Tests for StepDraft domain model.
 *
 * Phase 4.10.1: Domain Separation
 */
class StepDraftTest {

    @Test
    fun `empty step draft has empty title`() {
        val draft = StepDraft.EMPTY
        assertEquals("", draft.title)
        assertTrue(draft.initialActivities.isEmpty())
    }

    @Test
    fun `step draft with title only`() {
        val draft = StepDraft(title = "طراحی صفحه اصلی")
        assertEquals("طراحی صفحه اصلی", draft.title)
        assertTrue(draft.initialActivities.isEmpty())
        assertFalse(draft.hasInitialContent())
    }

    @Test
    fun `step draft with initial note`() {
        val draft = StepDraft(
            title = "طراحی صفحه اصلی",
            initialActivities = listOf(
                ActivityDraft(text = "نمونه اولیه آماده شد")
            )
        )
        assertEquals("طراحی صفحه اصلی", draft.title)
        assertEquals(1, draft.initialActivities.size)
        assertTrue(draft.hasInitialContent())
    }

    @Test
    fun `step draft with initial image`() {
        val draft = StepDraft(
            title = "طراحی صفحه اصلی",
            initialActivities = listOf(
                ActivityDraft(
                    attachments = listOf(
                        ActivityAttachment.Image(uri = "content://media/picker/0/test.jpg")
                    )
                )
            )
        )
        assertEquals("طراحی صفحه اصلی", draft.title)
        assertEquals(1, draft.initialActivities.size)
        assertTrue(draft.hasInitialContent())
        assertEquals(1, draft.getTotalAttachmentCount())
    }

    @Test
    fun `step draft with multiple attachments`() {
        val draft = StepDraft(
            title = "طراحی صفحه اصلی",
            initialActivities = listOf(
                ActivityDraft(
                    text = "تصاویر اولیه",
                    attachments = listOf(
                        ActivityAttachment.Image(uri = "content://media/picker/0/img1.jpg"),
                        ActivityAttachment.Image(uri = "content://media/picker/0/img2.jpg")
                    )
                )
            )
        )
        assertEquals(2, draft.getTotalAttachmentCount())
    }

    @Test
    fun `step draft with multiple initial activities`() {
        val draft = StepDraft(
            title = "طراحی صفحه اصلی",
            initialActivities = listOf(
                ActivityDraft(text = "یادداشت اول"),
                ActivityDraft(
                    attachments = listOf(
                        ActivityAttachment.Image(uri = "content://media/picker/0/test.jpg")
                    )
                )
            )
        )
        assertEquals(2, draft.initialActivities.size)
        assertEquals(1, draft.getTotalAttachmentCount())
    }

    @Test
    fun `step draft title is required`() {
        val draft = StepDraft(title = "")
        assertEquals("", draft.title)
    }
}
