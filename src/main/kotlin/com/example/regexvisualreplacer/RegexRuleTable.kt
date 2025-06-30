package com.example.regexvisualreplacer

import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.JPanel

class RegexRuleTable(private val tableModel: RegexRuleTableModel) : JPanel() {
    
    private val table = JBTable(tableModel)
    
    init {
        layout = BorderLayout()
        
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { addRule() }
            .setRemoveAction { removeRule() }
            .createPanel()
        
        add(decorator, BorderLayout.CENTER)
        
        table.setShowGrid(true)
        table.columnModel.getColumn(0).preferredWidth = 200
        table.columnModel.getColumn(1).preferredWidth = 200
        table.columnModel.getColumn(2).preferredWidth = 80
    }
    
    private fun addRule() {
        val newRule = RegexReplacementRule("", "", true)
        tableModel.addRule(newRule)
        val rowIndex = tableModel.rowCount - 1
        table.setRowSelectionInterval(rowIndex, rowIndex)
        table.editCellAt(rowIndex, 0)
    }
    
    private fun removeRule() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            tableModel.removeRule(selectedRow)
        }
    }
}