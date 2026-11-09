package de.luca.dungeon_master_manager.data

data class FuzzyMatchResult(
    val score: Double,
    val matchedIndices: List<Int>
)

object FuzzyMatcher {

    /**
     * Matches [query] against [target].
     * Returns a [FuzzyMatchResult] if [query] matches [target], or null if it doesn't match.
     */
    fun match(query: String, target: String): FuzzyMatchResult? {
        val q = query.trim()
        if (q.isEmpty()) {
            return FuzzyMatchResult(score = 1.0, matchedIndices = emptyList())
        }
        if (target.isEmpty()) return null

        val qLower = q.lowercase()
        val tLower = target.lowercase()

        // 1. Exact match (highest priority)
        if (tLower == qLower) {
            return FuzzyMatchResult(
                score = 1000.0,
                matchedIndices = target.indices.toList()
            )
        }

        // 2. Starts with query
        if (tLower.startsWith(qLower)) {
            val score = 800.0 + (qLower.length.toDouble() / tLower.length) * 100.0
            return FuzzyMatchResult(
                score = score,
                matchedIndices = (0 until q.length).toList()
            )
        }

        // 3. Contains as continuous substring
        val subIndex = tLower.indexOf(qLower)
        if (subIndex != -1) {
            // Higher score if matched at a word boundary (after ' ', '_', '-', '/')
            val isWordBoundary = subIndex == 0 || !tLower[subIndex - 1].isLetterOrDigit()
            val score = (if (isWordBoundary) 600.0 else 400.0) - (subIndex * 2.0)
            return FuzzyMatchResult(
                score = score,
                matchedIndices = (subIndex until subIndex + q.length).toList()
            )
        }

        // 4. Subsequence fuzzy match
        val matchedIndices = mutableListOf<Int>()
        var qIdx = 0
        var consecutiveBonus = 0
        var score = 100.0

        for (tIdx in target.indices) {
            if (qIdx < qLower.length && tLower[tIdx] == qLower[qIdx]) {
                matchedIndices.add(tIdx)

                // Word boundary bonus
                if (tIdx == 0 || !tLower[tIdx - 1].isLetterOrDigit()) {
                    score += 25.0
                }

                // CamelCase bonus
                if (tIdx > 0 && target[tIdx].isUpperCase() && target[tIdx - 1].isLowerCase()) {
                    score += 20.0
                }

                // Consecutive match bonus
                if (matchedIndices.size > 1 && matchedIndices[matchedIndices.size - 2] == tIdx - 1) {
                    consecutiveBonus += 15
                    score += consecutiveBonus
                } else {
                    consecutiveBonus = 0
                }

                qIdx++
            }
        }

        // If not all query characters were matched, no match
        if (qIdx < qLower.length) {
            return null
        }

        // Penalty for gaps and long targets
        val span = (matchedIndices.last() - matchedIndices.first() + 1)
        val gapPenalty = (span - q.length) * 2.0
        score -= gapPenalty
        score -= (target.length - q.length) * 0.5

        return FuzzyMatchResult(
            score = score.coerceAtLeast(10.0),
            matchedIndices = matchedIndices
        )
    }
}
