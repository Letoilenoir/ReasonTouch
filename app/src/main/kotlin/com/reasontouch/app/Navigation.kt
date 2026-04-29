package com.reasontouch.app

sealed class Screen(val route: String) {
    object SessionList : Screen("session_list")
    object Chords : Screen("chords/{sessionId}") {
        fun createRoute(sessionId: String) = "chords/$sessionId"
    }
    object PianoRoll : Screen("piano_roll/{sessionId}") {
        fun createRoute(sessionId: String) = "piano_roll/$sessionId"
    }
    object Drums : Screen("drums/{sessionId}") {
        fun createRoute(sessionId: String) = "drums/$sessionId"
    }
    object SessionSettings : Screen("session_settings/{sessionId}") {
        fun createRoute(sessionId: String) = "session_settings/$sessionId"
    }
}