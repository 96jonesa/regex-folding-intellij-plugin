package com.example.regexvisualreplacer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import java.awt.Font

class RegexLineHiderEditorFactoryListener : EditorFactoryListener {
    
    private val service = RegexReplacementService.getInstance()
    
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        processEditor(editor)
    }
    
    private fun processEditor(editor: Editor) {
        if (editor !is EditorEx) return
        
        val document = editor.document
        val fileText = document.text
        val rules = service.getEnabledRules()
        
        println("RegexLineHiderEditorFactoryListener: Processing editor with ${rules.size} rules")
        
        for (rule in rules) {
            val multiLineMatches = rule.findMultiLineMatches(fileText)
            println("RegexLineHiderEditorFactoryListener: Found ${multiLineMatches.size} matches")
            
            for (match in multiLineMatches) {
                if (match.totalLines > 1) {
                    // Hide lines after the first line by making them very small and gray
                    val matchStart = match.matchRange.first
                    val matchEnd = match.matchRange.last
                    
                    // Find the first newline in the match
                    val firstNewlineIndex = fileText.indexOf('\n', matchStart)
                    if (firstNewlineIndex != -1 && firstNewlineIndex < matchEnd) {
                        val hideStart = firstNewlineIndex + 1
                        val hideEnd = matchEnd
                        
                        if (hideStart < hideEnd) {
                            val textRange = TextRange(hideStart, hideEnd)
                            val attributes = TextAttributes().apply {
                                foregroundColor = JBColor.GRAY
                                backgroundColor = JBColor.LIGHT_GRAY
                                fontType = Font.ITALIC
                            }
                            
                            val highlighter: RangeHighlighter = editor.markupModel.addRangeHighlighter(
                                textRange.startOffset,
                                textRange.endOffset,
                                HighlighterLayer.LAST,
                                attributes,
                                HighlighterTargetArea.EXACT_RANGE
                            )
                            
                            println("RegexLineHiderEditorFactoryListener: Added highlighter for range $hideStart-$hideEnd")
                        }
                    }
                }
            }
        }
    }
}