package com.reasontouch.core.data

import javax.inject.Inject
import javax.inject.Singleton
import java.util.Stack

data class SessionSnapshot(
    val chords: List<ChordEvent>,
    val notesMap: Map<String, List<NoteEvent>>
)

@Singleton
class UndoHistoryManager @Inject constructor() {
    private val stacks = mutableMapOf<String, Stack<SessionSnapshot>>()

    fun push(sessionId: String, snapshot: SessionSnapshot) {
        val stack = stacks.getOrPut(sessionId) { Stack() }
        stack.push(snapshot)
    }

    fun pop(sessionId: String): SessionSnapshot? {
        val stack = stacks[sessionId] ?: return null
        return if (stack.isNotEmpty()) stack.pop() else null
    }

    fun canUndo(sessionId: String): Boolean {
        val stack = stacks[sessionId]
        return stack != null && stack.isNotEmpty()
    }
}
