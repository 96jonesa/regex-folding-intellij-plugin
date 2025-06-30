package com.example.regexvisualreplacer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection

@Service(Service.Level.APP)
@State(
    name = "RegexReplacementService",
    storages = [Storage("regexReplacementRules.xml")]
)
class RegexReplacementService : PersistentStateComponent<RegexReplacementService.State> {
    
    class State {
        @XCollection(elementName = "rule", valueAttributeName = "", style = XCollection.Style.v2)
        var rules: MutableList<RegexReplacementRule> = mutableListOf()
    }
    
    private var state = State()
    
    companion object {
        fun getInstance(): RegexReplacementService =
            ApplicationManager.getApplication().getService(RegexReplacementService::class.java)
    }
    
    override fun getState(): State = state
    
    override fun loadState(newState: State) {
        XmlSerializerUtil.copyBean(newState, this.state)
    }
    
    fun getRules(): List<RegexReplacementRule> = state.rules.toList()
    
    fun addRule(rule: RegexReplacementRule) {
        state.rules.add(rule)
    }
    
    fun removeRule(rule: RegexReplacementRule) {
        state.rules.remove(rule)
    }
    
    fun updateRule(oldRule: RegexReplacementRule, newRule: RegexReplacementRule) {
        val index = state.rules.indexOf(oldRule)
        if (index != -1) {
            state.rules[index] = newRule
        }
    }
    
    fun clearRules() {
        state.rules.clear()
    }
    
    fun getEnabledRules(): List<RegexReplacementRule> = 
        state.rules.filter { it.isEnabled && it.isValid() }
}