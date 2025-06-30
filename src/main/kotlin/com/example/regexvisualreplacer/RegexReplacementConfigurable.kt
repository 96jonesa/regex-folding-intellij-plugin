package com.example.regexvisualreplacer

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class RegexReplacementConfigurable : SearchableConfigurable {
    
    private val service = RegexReplacementService.getInstance()
    private lateinit var panel: DialogPanel
    private val tableModel = RegexRuleTableModel()
    
    override fun getId(): String = "regex.visual.replacer.settings"
    
    override fun getDisplayName(): String = "Regex Visual Replacer"
    
    override fun createComponent(): JComponent {
        panel = panel {
            group("Regex Replacement Rules") {
                row {
                    cell(RegexRuleTable(tableModel))
                        .align(AlignX.FILL)
                        .resizableColumn()
                }
            }
        }
        
        reset()
        return panel
    }
    
    override fun isModified(): Boolean {
        return tableModel.isModified(service.getRules())
    }
    
    override fun apply() {
        service.clearRules()
        tableModel.getRules().forEach { service.addRule(it) }
        tableModel.resetModified()
    }
    
    override fun reset() {
        tableModel.setRules(service.getRules())
    }
}