package com.example.plugins.notes

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insert(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)
}
