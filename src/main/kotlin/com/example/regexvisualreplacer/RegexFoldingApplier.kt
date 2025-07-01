package com.example.regexvisualreplacer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.util.TextRange
import com.intellij.util.Alarm

class RegexFoldingApplier : EditorFactoryListener {
    
    private val service = RegexReplacementService.getInstance()
    private val alarm = Alarm()
    
    // Track our custom fold regions to distinguish them from IDE's built-in folding
    private val customRegexFoldRanges = mutableSetOf<Pair<Int, Int>>()
    
    /**
     * Check if a fold region is one of our custom regex-based fold regions
     */
    private fun isCustomRegexFoldRegion(foldRegion: com.intellij.openapi.editor.FoldRegion): Boolean {
        val range = Pair(foldRegion.startOffset, foldRegion.endOffset)
        return customRegexFoldRanges.contains(range)
    }
    
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val file = editor.virtualFile
        
        println("RegexFoldingApplier: Editor created for file: ${file?.name} (${file?.extension})")
        
        // Apply initial folding with multiple attempts to ensure it sticks
        ApplicationManager.getApplication().invokeLater {
            applyRegexFoldingWithRetry(editor, 0)
        }
    }
    
    private fun applyRegexFoldingWithRetry(editor: Editor, attempt: Int) {
        val file = editor.virtualFile
        val isPythonFile = file?.extension?.lowercase() == "py"
        val maxAttempts = if (isPythonFile) 8 else 5 // More attempts for Python files
        
        if (attempt > maxAttempts) return
        
        val success = applyRegexFolding(editor)
        
        if (!success && attempt < maxAttempts) {
            // Retry after a delay (longer delays for Python files)
            val baseDelay = if (isPythonFile) 1000 else 500
            alarm.addRequest({
                applyRegexFoldingWithRetry(editor, attempt + 1)
            }, baseDelay + (attempt * 300))
        }
    }
    
    private fun applyRegexFolding(editor: Editor): Boolean {
        val document = editor.document
        val fileText = document.text
        val rules = service.getEnabledRules()
        val file = editor.virtualFile
        
        println("RegexFoldingApplier: Processing editor (attempt) for ${file?.extension} file with ${rules.size} rules")
        println("RegexFoldingApplier: Editor type: ${editor.javaClass.simpleName}")
        println("RegexFoldingApplier: Folding model type: ${editor.foldingModel.javaClass.simpleName}")
        
        if (rules.isEmpty()) return false
        
        val foldingModel = editor.foldingModel as FoldingModelEx
        var success = false
        
        // For Python files, try a different approach
        val isPythonFile = file?.extension?.lowercase() == "py"
        if (isPythonFile) {
            println("RegexFoldingApplier: Detected Python file, using alternative approach")
        }
        
        foldingModel.runBatchFoldingOperation {
            // Only clear existing regex-based custom fold regions, preserve IDE's built-in folding
            val existingCustomRegions = foldingModel.allFoldRegions.filter { 
                it.placeholderText == "..." && isCustomRegexFoldRegion(it)
            }
            println("RegexFoldingApplier: Found ${existingCustomRegions.size} existing custom regex fold regions to remove")
            for (region in existingCustomRegions) {
                try {
                    // Remove from our tracking set
                    val range = Pair(region.startOffset, region.endOffset)
                    customRegexFoldRanges.remove(range)
                    
                    foldingModel.removeFoldRegion(region)
                    println("RegexFoldingApplier: Removed existing regex fold region")
                } catch (e: Exception) {
                    println("RegexFoldingApplier: Failed to remove fold region: ${e.message}")
                }
            }
            
            // Track created regions to avoid overlaps
            val createdRegions = mutableListOf<Pair<Int, Int>>()
            
            for (rule in rules) {
                val multiLineMatches = rule.findMultiLineMatches(fileText)
                println("RegexFoldingApplier: Found ${multiLineMatches.size} matches")
                
                for (match in multiLineMatches) {
                    if (match.totalLines > 1) {
                        val matchStart = match.matchRange.first
                        val matchEnd = match.matchRange.last + 1
                        
                        println("RegexFoldingApplier: Character at matchEnd-1: '${fileText.getOrNull(matchEnd-1)}'")
                        println("RegexFoldingApplier: Character at matchEnd: '${fileText.getOrNull(matchEnd)}'")
                        
                        // Find the first newline in the match
                        val firstNewlineIndex = fileText.indexOf('\n', matchStart)
                        if (firstNewlineIndex != -1 && firstNewlineIndex < matchEnd) {
                            val foldStart = firstNewlineIndex
                            
                            // Use the full match end for all patterns
                            val foldEnd = matchEnd
                            
                            println("RegexFoldingApplier: Creating fold region from $foldStart to $foldEnd")
                            println("RegexFoldingApplier: Text to fold: '${fileText.substring(foldStart, minOf(foldEnd, foldStart + 100))}...'")
                            println("RegexFoldingApplier: Match range was: $matchStart to $matchEnd")
                            println("RegexFoldingApplier: Last chars of fold: '${fileText.substring(maxOf(foldStart, foldEnd - 20), foldEnd)}'")
                            
                            // Check for overlaps with existing regions
                            val hasOverlap = createdRegions.any { (existingStart, existingEnd) ->
                                !(foldEnd <= existingStart || foldStart >= existingEnd)
                            }
                            
                            if (foldStart < foldEnd && !hasOverlap) {
                                createdRegions.add(Pair(foldStart, foldEnd))
                                
                                try {
                                    val foldRegion = foldingModel.addFoldRegion(
                                        foldStart,
                                        foldEnd,
                                        "..."
                                    )
                                    
                                    if (foldRegion != null) {
                                        // Track this as our custom regex fold region
                                        customRegexFoldRanges.add(Pair(foldStart, foldEnd))
                                        
                                        // Set properties to make folding more stable
                                        foldRegion.isExpanded = false
                                        
                                        // Mark the region as non-navigable to avoid conflicts
                                        // with IDE's built-in folding
                                        try {
                                            val field = foldRegion.javaClass.getDeclaredField("myCanBeRemovedWhenCollapsed")
                                            field.isAccessible = true
                                            field.setBoolean(foldRegion, false)
                                        } catch (e: Exception) {
                                            // Ignore if field doesn't exist
                                        }
                                        
                                        success = true
                                        println("RegexFoldingApplier: Successfully created and collapsed custom regex fold region")
                                    } else {
                                        println("RegexFoldingApplier: Failed to create fold region")
                                    }
                                } catch (e: Exception) {
                                    println("RegexFoldingApplier: Exception creating fold region: ${e.message}")
                                }
                            } else if (hasOverlap) {
                                println("RegexFoldingApplier: Skipping fold region due to overlap with existing region")
                            }
                        }
                    }
                }
            }
        }
        
        return success
    }
}