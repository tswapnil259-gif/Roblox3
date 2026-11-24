package com.robloxai.studio.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

object LuauSyntaxHighlighter {
    private val keywordColor = Color(0xFFC792EA)
    private val stringColor = Color(0xFFC3E88D)
    private val commentColor = Color(0xFF546E7A)
    private val numberColor = Color(0xFFF78C6C)
    private val functionColor = Color(0xFF82AAFF)

    private val keywords = setOf(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
        "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return",
        "then", "true", "until", "while", "export", "type", "continue"
    )

    fun highlight(code: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("--", i) -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    withStyle(SpanStyle(color = commentColor)) { append(code.substring(i, end)) }
                    i = end
                }
                code[i] == '"' || code[i] == '\'' -> {
                    val quote = code[i]
                    val end = code.indexOf(quote, i + 1).let { if (it == -1) code.length else it + 1 }
                    withStyle(SpanStyle(color = stringColor)) { append(code.substring(i, end)) }
                    i = end
                }
                code[i].isDigit() -> {
                    var end = i
                    while (end < code.length && (code[end].isDigit() || code[end] == '.')) end++
                    withStyle(SpanStyle(color = numberColor)) { append(code.substring(i, end)) }
                    i = end
                }
                code[i].isLetter() || code[i] == '_' -> {
                    var end = i
                    while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_')) end++
                    val word = code.substring(i, end)
                    val style = when {
                        keywords.contains(word) -> SpanStyle(color = keywordColor)
                        end < code.length && code[end] == '(' -> SpanStyle(color = functionColor)
                        else -> SpanStyle(color = Color(0xFFEEFFFF))
                    }
                    withStyle(style) { append(word) }
                    i = end
                }
                else -> {
                    append(code[i])
                    i++
                }
            }
        }
    }

    val visualTransformation = VisualTransformation { text ->
        TransformedText(highlight(text.text), OffsetMapping.Identity)
    }
}
