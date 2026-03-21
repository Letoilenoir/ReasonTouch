package com.reasontouch.app

sealed class Screen(val route: String) {
    object SessionList : Screen("session_list")
    object Chords : Screen("chords/{sessionId}") {
        fun createRoute(sessionId: String) = "chords/$sessionId"
    }
    object PianoRoll : Screen("piano_roll/{sessionId}") {
        fun createRoute(sessionId: String) = "piano_roll/$sessionId"
    }
}
