package com.example.regexvisualreplacer

import javax.swing.table.AbstractTableModel

class RegexRuleTableModel : AbstractTableModel() {
    
    private val rules = mutableListOf<RegexReplacementRule>()
    private var modified = false
    
    companion object {
        const val PATTERN_COLUMN = 0
        const val REPLACEMENT_COLUMN = 1
        const val ENABLED_COLUMN = 2
    }
    
    override fun getRowCount(): Int = rules.size
    
    override fun getColumnCount(): Int = 3
    
    override fun getColumnName(column: Int): String = when (column) {
        PATTERN_COLUMN -> "Pattern"
        REPLACEMENT_COLUMN -> "Replacement"
        ENABLED_COLUMN -> "Enabled"
        else -> ""
    }
    
    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        PATTERN_COLUMN -> String::class.java
        REPLACEMENT_COLUMN -> String::class.java
        ENABLED_COLUMN -> Boolean::class.java
        else -> Any::class.java
    }
    
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true
    
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex >= rules.size) return null
        val rule = rules[rowIndex]
        return when (columnIndex) {
            PATTERN_COLUMN -> rule.pattern
            REPLACEMENT_COLUMN -> rule.replacement
            ENABLED_COLUMN -> rule.isEnabled
            else -> null
        }
    }
    
    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (rowIndex >= rules.size) return
        
        val rule = rules[rowIndex]
        val newRule = when (columnIndex) {
            PATTERN_COLUMN -> rule.copy(pattern = aValue as String)
            REPLACEMENT_COLUMN -> rule.copy(replacement = aValue as String)
            ENABLED_COLUMN -> rule.copy(isEnabled = aValue as Boolean)
            else -> rule
        }
        
        rules[rowIndex] = newRule
        modified = true
        fireTableCellUpdated(rowIndex, columnIndex)
    }
    
    fun addRule(rule: RegexReplacementRule) {
        rules.add(rule)
        modified = true
        fireTableRowsInserted(rules.size - 1, rules.size - 1)
    }
    
    fun removeRule(rowIndex: Int) {
        if (rowIndex >= 0 && rowIndex < rules.size) {
            rules.removeAt(rowIndex)
            modified = true
            fireTableRowsDeleted(rowIndex, rowIndex)
        }
    }
    
    fun getRules(): List<RegexReplacementRule> = rules.toList()
    
    fun setRules(newRules: List<RegexReplacementRule>) {
        rules.clear()
        rules.addAll(newRules)
        modified = false
        fireTableDataChanged()
    }
    
    fun isModified(originalRules: List<RegexReplacementRule>): Boolean {
        return modified || rules != originalRules
    }
    
    fun resetModified() {
        modified = false
    }
}