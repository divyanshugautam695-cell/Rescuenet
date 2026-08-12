package com.rescuenet.app.ai

import com.rescuenet.app.model.EmergencyType
import com.rescuenet.app.model.Priority

/**
 * Offline baseline classifier. No API key, internet, or cloud inference is used.
 *
 * The weights are intentionally tiny and interpretable so the prototype can run on
 * any Android phone. The next AI milestone can replace this scorer with a quantized
 * TensorFlow Lite model trained on an openly licensed disaster-intent dataset.
 */
class EmergencyClassifier {
    data class Result(val type: EmergencyType, val priority: Priority, val confidence: Int)

    fun classify(text: String, injured: Boolean, people: Int): Result {
        val t = text.lowercase()
        val scores = linkedMapOf(
            EmergencyType.FIRE to score(t, listOf("fire", "smoke", "burn", "flame")),
            EmergencyType.FLOOD to score(t, listOf("flood", "water", "drowning", "river")),
            EmergencyType.EARTHQUAKE to score(t, listOf("earthquake", "quake", "shaking", "collapsed")),
            EmergencyType.MEDICAL to score(t, listOf("injured", "unconscious", "bleeding", "medical", "hurt")),
            EmergencyType.TRAPPED to score(t, listOf("trapped", "stuck", "blocked", "cannot leave")),
            EmergencyType.OTHER to 1
        )
        val best = scores.maxBy { it.value }
        val critical = injured || people >= 4 || best.value >= 6
        val priority = when {
            critical -> Priority.CRITICAL
            best.value >= 4 -> Priority.HIGH
            best.value >= 2 -> Priority.MEDIUM
            else -> Priority.LOW
        }
        return Result(best.key, priority, (55 + best.value * 7).coerceAtMost(97))
    }

    private fun score(text: String, words: List<String>): Int = words.sumOf { if (text.contains(it)) 2 else 0 }
}
