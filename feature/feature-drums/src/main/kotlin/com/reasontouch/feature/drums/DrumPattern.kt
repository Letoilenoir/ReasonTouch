package com.reasontouch.feature.drums

/**
 * Represents a drum pattern — 6 lanes x N steps.
 * Each cell is active (true) or inactive (false).
 * Velocity per active step stored separately for future velocity editing.
 */
data class DrumPattern(
    val steps:     Int = DrumKit.STEPS_DEFAULT,
    val grid:      List<List<Boolean>>  = List(DrumKit.lanes.size) { List(steps) { false } },
    val velocities: List<List<Int>>     = List(DrumKit.lanes.size) { List(steps) { 100 } }
) {
    fun toggle(laneIdx: Int, stepIdx: Int): DrumPattern {
        val newGrid = grid.mapIndexed { li, row ->
            if (li == laneIdx) row.mapIndexed { si, cell ->
                if (si == stepIdx) !cell else cell
            } else row
        }
        return copy(grid = newGrid)
    }

    fun isActive(laneIdx: Int, stepIdx: Int): Boolean =
        grid.getOrNull(laneIdx)?.getOrNull(stepIdx) ?: false

    fun velocity(laneIdx: Int, stepIdx: Int): Int =
        velocities.getOrNull(laneIdx)?.getOrNull(stepIdx) ?: 100

    fun setVelocity(laneIdx: Int, stepIdx: Int, vel: Int): DrumPattern {
        val newVel = velocities.mapIndexed { li, row ->
            if (li == laneIdx) row.mapIndexed { si, v ->
                if (si == stepIdx) vel.coerceIn(1, 127) else v
            } else row
        }
        return copy(velocities = newVel)
    }

    fun clear(): DrumPattern = copy(
        grid       = List(DrumKit.lanes.size) { List(steps) { false } },
        velocities = List(DrumKit.lanes.size) { List(steps) { 100 } }
    )

    fun extendTo32(): DrumPattern {
        if (steps >= 32) return this
        return copy(
            steps      = 32,
            grid       = grid.map { row -> row + List(16) { false } },
            velocities = velocities.map { row -> row + List(16) { 100 } }
        )
    }

    fun trimTo16(): DrumPattern {
        if (steps <= 16) return this
        return copy(
            steps      = 16,
            grid       = grid.map { row -> row.take(16) },
            velocities = velocities.map { row -> row.take(16) }
        )
    }
}

// Preset patterns
object DrumPresets {
    val FOUR_FOUR = DrumPattern().let { p ->
        // Kick on 1 & 3, Snare on 2 & 4, HH on every 8th
        var result = p
        listOf(0, 8).forEach          { result = result.toggle(0, it) }  // Kick
        listOf(4, 12).forEach         { result = result.toggle(1, it) }  // Snare
        (0 until 16 step 2).forEach   { result = result.toggle(3, it) }  // HH
        result
    }

    val ROCK = DrumPattern().let { p ->
        var result = p
        listOf(0, 6, 8, 14).forEach  { result = result.toggle(0, it) }  // Kick
        listOf(4, 12).forEach        { result = result.toggle(1, it) }  // Snare
        (0 until 16 step 2).forEach  { result = result.toggle(3, it) }  // HH
        result
    }

    val FUNK = DrumPattern().let { p ->
        var result = p
        listOf(0, 3, 8, 11, 14).forEach { result = result.toggle(0, it) }  // Kick
        listOf(4, 12).forEach           { result = result.toggle(1, it) }  // Snare
        listOf(2, 6, 10, 14).forEach    { result = result.toggle(2, it) }  // Clap
        (0 until 16).forEach            { result = result.toggle(3, it) }  // HH 16ths
        result
    }

    val REGGAE = DrumPattern().let { p ->
        var result = p
        listOf(0, 12).forEach          { result = result.toggle(0, it) }  // Kick
        listOf(6, 14).forEach          { result = result.toggle(1, it) }  // Snare
        listOf(2, 6, 10, 14).forEach   { result = result.toggle(3, it) }  // HH offbeats
        result
    }

    val BOSSA = DrumPattern().let { p ->
        var result = p
        listOf(0, 9).forEach           { result = result.toggle(0, it) }  // Kick
        listOf(4, 13).forEach          { result = result.toggle(1, it) }  // Snare
        listOf(0,2,3,5,6,8,9,11,12,14,15).forEach { result = result.toggle(5, it) }  // Ride
        result
    }

    val all = mapOf(
        "4/4 Basic" to FOUR_FOUR,
        "Rock"      to ROCK,
        "Funk"      to FUNK,
        "Reggae"    to REGGAE,
        "Bossa Nova" to BOSSA
    )

    /**
     * Phase 6c of the Bass/Drum Arrangement Roadmap: suggests a Drum
     * preset by matching a chord strum pattern's name against known
     * preset genre names, e.g. "Reggae Skank" -> "Reggae", "Bossa Nova"
     * -> "Bossa Nova". Substring match, case-insensitive.
     *
     * "4/4 Basic" is deliberately excluded from matching -- it has no
     * genre-name overlap with any chord pattern and would otherwise
     * never realistically match anyway.
     *
     * Returns null if chordPatternName is null (no matched pattern --
     * see CompositionContext.matchedPatternName) or if no preset's
     * genre name appears in it. This is advisory only, per
     * docs/design/Drum_Arrangement_Specification.md Section 9 -- the
     * caller decides whether/how to surface it, this never forces a
     * selection.
     */
    fun suggestForChordPatternName(chordPatternName: String?): String? {
        if (chordPatternName == null) return null
        return all.keys.firstOrNull { presetName ->
            presetName != "4/4 Basic" &&
                    chordPatternName.contains(presetName, ignoreCase = true)
        }
    }
}