package com.example.regexvisualreplacer

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

class RegexMultiLineFoldingBuilder : FoldingBuilderEx() {
    
    private val service = RegexReplacementService.getInstance()
    
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        println("RegexMultiLineFoldingBuilder: buildFoldRegions called for ${root.containingFile?.name}")
        
        val rules = service.getEnabledRules()
        println("RegexMultiLineFoldingBuilder: Found ${rules.size} enabled rules")
        
        if (rules.isEmpty()) return emptyArray()
        
        val fileText = document.text
        val foldingDescriptors = mutableListOf<FoldingDescriptor>()
        
        for (rule in rules) {
            println("RegexMultiLineFoldingBuilder: Processing rule with pattern: ${rule.pattern}")
            val multiLineMatches = rule.findMultiLineMatches(fileText)
            println("RegexMultiLineFoldingBuilder: Found ${multiLineMatches.size} multi-line matches")
            
            for (match in multiLineMatches) {
                println("RegexMultiLineFoldingBuilder: Match has ${match.totalLines} lines")
                if (match.totalLines > 1) {
                    val matchStart = match.matchRange.first
                    val matchEnd = match.matchRange.last + 1 // Include the end character
                    
                    // Find the first newline in the match
                    val firstNewlineIndex = fileText.indexOf('\n', matchStart)
                    if (firstNewlineIndex != -1 && firstNewlineIndex < matchEnd) {
                        val foldStart = firstNewlineIndex
                        val foldEnd = minOf(matchEnd, fileText.length)
                        
                        println("RegexMultiLineFoldingBuilder: Creating fold region from $foldStart to $foldEnd")
                        println("RegexMultiLineFoldingBuilder: Text to fold: '${fileText.substring(foldStart, foldEnd).take(50)}...'")
                        
                        if (foldStart < foldEnd && foldEnd <= fileText.length) {
                            val textRange = TextRange(foldStart, foldEnd)
                            
                            // Try to find any PSI element that covers this range
                            var elementToFold: PsiElement? = null
                            
                            // Look for elements within the fold range
                            for (offset in foldStart until minOf(foldStart + 100, foldEnd)) {
                                val element = PsiTreeUtil.findElementOfClassAtOffset(
                                    root.containingFile, 
                                    offset, 
                                    PsiElement::class.java, 
                                    false
                                )
                                if (element != null && element !is PsiWhiteSpace) {
                                    elementToFold = element
                                    break
                                }
                            }
                            
                            // If we still don't have an element, use the root
                            if (elementToFold == null) {
                                elementToFold = root
                            }
                            
                            val foldingDescriptor = FoldingDescriptor(
                                elementToFold.node,
                                textRange,
                                null
                            )
                            foldingDescriptors.add(foldingDescriptor)
                            println("RegexMultiLineFoldingBuilder: Added folding descriptor using element: ${elementToFold.javaClass.simpleName}")
                        }
                    }
                }
            }
        }
        
        println("RegexMultiLineFoldingBuilder: Returning ${foldingDescriptors.size} folding descriptors")
        return foldingDescriptors.toTypedArray()
    }
    
    override fun getPlaceholderText(node: ASTNode): String {
        println("RegexMultiLineFoldingBuilder: getPlaceholderText called")
        return "..."
    }
    
    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        println("RegexMultiLineFoldingBuilder: isCollapsedByDefault called")
        return true
    }
}