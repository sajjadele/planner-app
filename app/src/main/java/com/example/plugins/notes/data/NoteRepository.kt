package com.example.plugins.notes.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByTaskId(taskId: Int): Flow<List<NoteEntity>> = noteDao.getNotesByTaskId(taskId)

    suspend fun insert(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)
}
