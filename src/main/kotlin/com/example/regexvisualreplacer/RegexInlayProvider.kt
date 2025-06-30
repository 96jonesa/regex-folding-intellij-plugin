package com.example.regexvisualreplacer

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class RegexInlayCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    
    private val service = RegexReplacementService.getInstance()
    private var processedFile = false
    
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        // Process the entire file content once at the root level
        if (!processedFile && element is PsiFile) {
            processedFile = true
            processFileContent(element, editor, sink)
        }
        
        return true
    }
    
    private fun processFileContent(file: PsiFile, editor: Editor, sink: InlayHintsSink) {
        val rules = service.getEnabledRules()
        if (rules.isEmpty()) return
        
        val fileText = file.text
        val document = editor.document
        
        for (rule in rules) {
            val multiLineMatches = rule.findMultiLineMatches(fileText)
            
            for (match in multiLineMatches) {
                if (match.totalLines > 1) {
                    // Minimize lines after the first line
                    for (lineNum in (match.startLine + 1)..match.endLine) {
                        if (lineNum < document.lineCount) {
                            val lineStartOffset = document.getLineStartOffset(lineNum)
                            val lineEndOffset = document.getLineEndOffset(lineNum)
                            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
                            
                            // Only minimize non-empty lines
                            if (lineText.trim().isNotEmpty()) {
                                val presentation = createMinimizedLinePresentation(
                                    lineText.trim(), 
                                    factory,
                                    lineNum - match.startLine // Line number within the match
                                )
                                sink.addBlockElement(lineStartOffset, true, true, 0, presentation)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun createMinimizedLinePresentation(
        originalText: String,
        factory: PresentationFactory,
        lineIndex: Int
    ): InlayPresentation {
        val minimizedText = if (originalText.length > 50) {
            "${originalText.substring(0, 47)}..."
        } else {
            originalText
        }
        
        return factory.roundWithBackground(
            factory.smallText("[$lineIndex] $minimizedText")
        )
    }
}