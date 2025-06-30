package com.example.regexvisualreplacer

import com.intellij.util.xmlb.annotations.Attribute

data class RegexReplacementRule(
    @Attribute("pattern")
    var pattern: String = "",
    @Attribute("replacement") 
    var replacement: String = "",
    @Attribute("enabled")
    var isEnabled: Boolean = true
) {
    // No-arg constructor for XML serialization
    constructor() : this("", "", true)
    
    private val compiledRegex: Regex? by lazy {
        try {
            if (pattern.isNotEmpty()) {
                println("RegexReplacementRule: Compiling pattern: ${pattern.take(100)}...")
                val regex = Regex(pattern, setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
                println("RegexReplacementRule: Successfully compiled regex")
                regex
            } else null
        } catch (e: Exception) {
            println("RegexReplacementRule: Failed to compile regex: ${e.message}")
            null
        }
    }
    
    fun isValid(): Boolean = pattern.isNotEmpty() && compiledRegex != null
    
    fun matches(text: String): Boolean = compiledRegex?.containsMatchIn(text) ?: false
    
    fun findMatches(text: String): List<MatchResult> {
        println("RegexReplacementRule: Searching for matches in text of length ${text.length}")
        val matches = compiledRegex?.findAll(text)?.toList() ?: emptyList()
        println("RegexReplacementRule: Found ${matches.size} raw matches")
        return matches
    }
    
    fun findMultiLineMatches(text: String): List<MultiLineMatch> {
        println("RegexReplacementRule: Finding matches for pattern: $pattern")
        val matches = findMatches(text)
        println("RegexReplacementRule: Found ${matches.size} total matches")
        
        return matches.map { match ->
            val matchText = match.value
            val lines = text.substring(0, match.range.first).count { it == '\n' }
            val matchLines = matchText.split('\n')
            val multiLineMatch = MultiLineMatch(
                startLine = lines,
                endLine = lines + matchLines.size - 1,
                firstLineText = matchLines.first(),
                totalLines = matchLines.size,
                matchRange = match.range
            )
            println("RegexReplacementRule: Match has ${multiLineMatch.totalLines} lines")
            println("RegexReplacementRule: Match text: '${matchText}'")
            println("RegexReplacementRule: Match ends with: '${matchText.takeLast(10)}'")
            println("RegexReplacementRule: Match range: ${match.range.first} to ${match.range.last}")
            multiLineMatch
        }
    }
}

data class MultiLineMatch(
    val startLine: Int,
    val endLine: Int,
    val firstLineText: String,
    val totalLines: Int,
    val matchRange: IntRange
)