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

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: String
) {
    object Chords : BottomNavItem(
        route = "chords",
        label = "CHORDS",
        icon = "music_note"
    )
    object PianoRoll : BottomNavItem(
        route = "piano_roll",
        label = "ARRANGE",
        icon = "piano"
    )
}
