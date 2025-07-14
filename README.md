**built entirely using Claude Code in an hour, I have not read a single line of this code or the README, it just does what I want it to well enough for personal use** - I may polish and publish this eventually

# Regex Visual Replacer Plugin

An IntelliJ IDEA plugin that allows users to visually replace regular expression patterns in the editor without modifying the actual file content.

## Features

- Define custom regex patterns with replacement text
- Visual display of replacements using inline hints
- Non-destructive - does not modify the actual file content
- Configurable through IntelliJ Settings
- Persistent storage of regex rules

## How to Use

1. **Configure Rules**: Go to `File > Settings > Editor > Regex Visual Replacer`
2. **Add Rules**: Click the "+" button to add new regex replacement rules
3. **Define Pattern**: Enter your regular expression pattern
4. **Set Replacement**: Enter the text that should be displayed instead
5. **Enable/Disable**: Use the checkbox to enable or disable individual rules

## Example

If you add a rule with:
- Pattern: `\bTODO\b`  
- Replacement: `✓ TASK`

Then anywhere "TODO" appears in your code, you'll see "✓ TASK" displayed inline while the actual file content remains unchanged.

## Installation

1. Build the plugin: `./gradlew build`
2. Install from disk in IntelliJ IDEA: `File > Settings > Plugins > Install from Disk`
3. Select the built plugin JAR file
4. Restart IntelliJ IDEA

## Development

This plugin is built using:
- Kotlin
- IntelliJ Platform SDK
- Gradle with IntelliJ Platform Plugin
