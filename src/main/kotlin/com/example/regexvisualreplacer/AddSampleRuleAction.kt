package com.example.regexvisualreplacer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class AddSampleRuleAction : AnAction("Add Sample Regex Rule") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val service = RegexReplacementService.getInstance()
        
        // Clear existing rules first
        service.clearRules()
        
        // Add both a simple rule and a complex rule
        val simpleRule = RegexReplacementRule(
            pattern = "START[\\s\\S]*?END",
            replacement = "",
            isEnabled = true
        )
        
        val complexRule = RegexReplacementRule(
            pattern = "public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*\\]\\s+\\w+\\s*\\)\\s*\\{[\\s\\S]*?\\}",
            replacement = "",
            isEnabled = true
        )
        
        service.addRule(simpleRule)
        service.addRule(complexRule)
        
        Messages.showInfoMessage(
            "Sample rules added:\n1. START...END pattern\n2. Java main method pattern\n\nBoth should work in all file types.",
            "Sample Rules Added"
        )
    }
}