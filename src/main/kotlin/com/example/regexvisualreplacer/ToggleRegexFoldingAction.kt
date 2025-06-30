package com.example.regexvisualreplacer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.FoldingModelEx

class ToggleRegexFoldingAction : AnAction("Toggle Regex Folding") {
    
    private val service = RegexReplacementService.getInstance()
    
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        
        println("ToggleRegexFoldingAction: Action triggered")
        
        ApplicationManager.getApplication().invokeLater {
            toggleRegexFolding(editor)
        }
    }
    
    override fun update(e: AnActionEvent) {
        // Enable the action only when an editor is available
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }
    
    private fun toggleRegexFolding(editor: Editor) {
        val foldingModel = editor.foldingModel as FoldingModelEx
        val rules = service.getEnabledRules()
        
        if (rules.isEmpty()) {
            println("ToggleRegexFoldingAction: No enabled rules found")
            return
        }
        
        foldingModel.runBatchFoldingOperation {
            // Always scan for new regions first
            val newFoldRegions = findAllMatchingRegions(editor)
            println("ToggleRegexFoldingAction: Found ${newFoldRegions.size} total matching regions in file")
            
            // Find all existing custom fold regions
            val existingCustomRegions = foldingModel.allFoldRegions.filter { 
                it.placeholderText == "..." 
            }
            
            println("ToggleRegexFoldingAction: Found ${existingCustomRegions.size} existing custom fold regions")
            
            // Determine what action to take
            if (existingCustomRegions.isEmpty()) {
                // No existing regions, create and fold all new ones
                println("ToggleRegexFoldingAction: Creating and folding all new regions")
                createFoldRegions(foldingModel, newFoldRegions, false) // false = folded
            } else {
                // Check if any existing regions are expanded (visible)
                val anyExpanded = existingCustomRegions.any { it.isExpanded }
                
                if (anyExpanded) {
                    // Some are expanded, fold all (existing + new)
                    println("ToggleRegexFoldingAction: Folding all regions (existing + new)")
                    
                    // Fold existing regions
                    existingCustomRegions.forEach { region ->
                        region.isExpanded = false
                    }
                    
                    // Create and fold any new regions
                    val existingRanges = existingCustomRegions.map { Pair(it.startOffset, it.endOffset) }
                    val newRegionsOnly = newFoldRegions.filter { newRegion ->
                        !existingRanges.any { existingRange ->
                            newRegion.first == existingRange.first && newRegion.second == existingRange.second
                        }
                    }
                    createFoldRegions(foldingModel, newRegionsOnly, false) // false = folded
                    
                } else {
                    // All existing are folded, unfold all existing (but create any new ones as folded)
                    println("ToggleRegexFoldingAction: Unfolding existing regions, creating new ones as folded")
                    
                    // Unfold existing regions
                    existingCustomRegions.forEach { region ->
                        region.isExpanded = true
                    }
                    
                    // Create any new regions as folded
                    val existingRanges = existingCustomRegions.map { Pair(it.startOffset, it.endOffset) }
                    val newRegionsOnly = newFoldRegions.filter { newRegion ->
                        !existingRanges.any { existingRange ->
                            newRegion.first == existingRange.first && newRegion.second == existingRange.second
                        }
                    }
                    createFoldRegions(foldingModel, newRegionsOnly, false) // false = folded
                }
            }
        }
    }
    
    private fun findAllMatchingRegions(editor: Editor): List<Pair<Int, Int>> {
        val document = editor.document
        val fileText = document.text
        val rules = service.getEnabledRules()
        val foldRegions = mutableListOf<Pair<Int, Int>>()
        
        // Track created regions to avoid overlaps
        val createdRegions = mutableListOf<Pair<Int, Int>>()
        
        for (rule in rules) {
            val multiLineMatches = rule.findMultiLineMatches(fileText)
            
            for (match in multiLineMatches) {
                if (match.totalLines > 1) {
                    val matchStart = match.matchRange.first
                    val matchEnd = match.matchRange.last + 1
                    
                    // Find the first newline in the match
                    val firstNewlineIndex = fileText.indexOf('\n', matchStart)
                    if (firstNewlineIndex != -1 && firstNewlineIndex < matchEnd) {
                        val foldStart = firstNewlineIndex
                        val foldEnd = matchEnd
                        
                        // Check for overlaps with existing regions
                        val hasOverlap = createdRegions.any { (existingStart, existingEnd) ->
                            !(foldEnd <= existingStart || foldStart >= existingEnd)
                        }
                        
                        if (foldStart < foldEnd && !hasOverlap) {
                            foldRegions.add(Pair(foldStart, foldEnd))
                            createdRegions.add(Pair(foldStart, foldEnd))
                        }
                    }
                }
            }
        }
        
        return foldRegions
    }
    
    private fun createFoldRegions(foldingModel: FoldingModelEx, regions: List<Pair<Int, Int>>, expanded: Boolean) {
        for ((foldStart, foldEnd) in regions) {
            try {
                val foldRegion = foldingModel.addFoldRegion(
                    foldStart,
                    foldEnd,
                    "..."
                )
                
                if (foldRegion != null) {
                    foldRegion.isExpanded = expanded
                    println("ToggleRegexFoldingAction: Created fold region from $foldStart to $foldEnd (expanded: $expanded)")
                } else {
                    println("ToggleRegexFoldingAction: Failed to create fold region")
                }
            } catch (e: Exception) {
                println("ToggleRegexFoldingAction: Exception creating fold region: ${e.message}")
            }
        }
    }
    
    // Legacy method - keeping for compatibility but not used anymore
    private fun createAllFoldRegions(editor: Editor, foldingModel: FoldingModelEx) {
        val document = editor.document
        val fileText = document.text
        val rules = service.getEnabledRules()
        val file = editor.virtualFile
        
        // Track created regions to avoid overlaps
        val createdRegions = mutableListOf<Pair<Int, Int>>()
        
        for (rule in rules) {
            val multiLineMatches = rule.findMultiLineMatches(fileText)
            println("ToggleRegexFoldingAction: Found ${multiLineMatches.size} matches for rule")
            
            for (match in multiLineMatches) {
                if (match.totalLines > 1) {
                    val matchStart = match.matchRange.first
                    val matchEnd = match.matchRange.last + 1
                    
                    // Find the first newline in the match
                    val firstNewlineIndex = fileText.indexOf('\n', matchStart)
                    if (firstNewlineIndex != -1 && firstNewlineIndex < matchEnd) {
                        val foldStart = firstNewlineIndex
                        
                        // Use the full match end for all patterns
                        val foldEnd = matchEnd
                        
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
                                    foldRegion.isExpanded = false
                                    println("ToggleRegexFoldingAction: Created fold region from $foldStart to $foldEnd")
                                } else {
                                    println("ToggleRegexFoldingAction: Failed to create fold region")
                                }
                            } catch (e: Exception) {
                                println("ToggleRegexFoldingAction: Exception creating fold region: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
}