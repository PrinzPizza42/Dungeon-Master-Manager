package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntityPopout: (String) -> Unit = {},
    entityColors: Map<String, String> = emptyMap()
) {
    var isPreviewMode by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(content))
    }

    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            val newSelection = if (content.startsWith(textFieldValue.text)) {
                androidx.compose.ui.text.TextRange(content.length)
            } else {
                androidx.compose.ui.text.TextRange(
                    textFieldValue.selection.start.coerceAtMost(content.length),
                    textFieldValue.selection.end.coerceAtMost(content.length)
                )
            }
            textFieldValue = TextFieldValue(text = content, selection = newSelection)
        }
    }

    fun applyFormat(formatSyntax: String) {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        
        val before = text.substring(0, selection.min)
        val selected = text.substring(selection.min, selection.max)
        val after = text.substring(selection.max)

        // Case 1: The user highlighted the entire formatted block, e.g., "**bold**"
        if (selected.startsWith(formatSyntax) && selected.endsWith(formatSyntax) && selected.length >= formatSyntax.length * 2) {
            val unformatted = selected.substring(formatSyntax.length, selected.length - formatSyntax.length)
            val newContent = "$before$unformatted$after"
            val newSelection = androidx.compose.ui.text.TextRange(selection.min, selection.min + unformatted.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            return
        }

        // Case 2: The user highlighted the text inside the formatting, e.g., "bold" surrounded by "**"
        if (before.endsWith(formatSyntax) && after.startsWith(formatSyntax)) {
            val newBefore = before.substring(0, before.length - formatSyntax.length)
            val newAfter = after.substring(formatSyntax.length)
            val newContent = "$newBefore$selected$newAfter"
            val newSelection = androidx.compose.ui.text.TextRange(newBefore.length, newBefore.length + selected.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            return
        }
        
        val newContent = "$before$formatSyntax$selected$formatSyntax$after"
        
        val newSelection = if (selected.isEmpty()) {
            androidx.compose.ui.text.TextRange(selection.min + formatSyntax.length)
        } else {
            androidx.compose.ui.text.TextRange(selection.min, selection.max + formatSyntax.length * 2)
        }
        
        textFieldValue = TextFieldValue(newContent, newSelection)
        onContentChange(newContent)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { 
                    onContentChange(content + "\n# New Heading\n")
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("+ Heading")
            }

            if (!isPreviewMode) {
                Button(onClick = { applyFormat("**") }, modifier = Modifier.padding(end = 4.dp)) {
                    Text("B", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { applyFormat("*") }, modifier = Modifier.padding(end = 4.dp)) {
                    Text("I", fontStyle = FontStyle.Italic)
                }
                Button(onClick = { applyFormat("__") }, modifier = Modifier.padding(end = 4.dp)) {
                    Text("U", textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Edit / Preview toggle
            TabRow(
                selectedTabIndex = if (isPreviewMode) 1 else 0,
                modifier = Modifier.width(200.dp),
                containerColor = Color.Transparent,
            ) {
                Tab(
                    selected = !isPreviewMode,
                    onClick = { isPreviewMode = false },
                    text = { Text("Edit") }
                )
                Tab(
                    selected = isPreviewMode,
                    onClick = { isPreviewMode = true },
                    text = { Text("Preview") }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (isPreviewMode) {
                MarkdownPreview(content, entityColors, onOpenEntityPopout)
            } else {
                MarkdownEdit(
                    textFieldValue = textFieldValue,
                    entityColors = entityColors,
                    onOpenEntityPopout = onOpenEntityPopout,
                    onValueChange = { newValue ->
                        var updatedSelection = newValue.selection
                        val text = newValue.text

                        // Auto-expand selection to include styling tokens if the inner text is selected
                        if (!updatedSelection.collapsed) {
                            val before = text.substring(0, updatedSelection.min)
                            val after = text.substring(updatedSelection.max)
                            
                            if (before.endsWith("**") && after.startsWith("**")) {
                                updatedSelection = androidx.compose.ui.text.TextRange(updatedSelection.min - 2, updatedSelection.max + 2)
                            } else if (before.endsWith("__") && after.startsWith("__")) {
                                updatedSelection = androidx.compose.ui.text.TextRange(updatedSelection.min - 2, updatedSelection.max + 2)
                            } else if (before.endsWith("*") && after.startsWith("*") && !before.endsWith("**") && !after.startsWith("**")) {
                                updatedSelection = androidx.compose.ui.text.TextRange(updatedSelection.min - 1, updatedSelection.max + 1)
                            } else if (before.endsWith("@[") && after.startsWith("]")) {
                                updatedSelection = androidx.compose.ui.text.TextRange(updatedSelection.min - 2, updatedSelection.max + 1)
                            }
                        }

                        val finalValue = newValue.copy(selection = updatedSelection)
                        
                        textFieldValue = finalValue
                        if (finalValue.text != content) {
                            onContentChange(finalValue.text)
                        }
                    },
                    onFormat = { applyFormat(it) }
                )
            }
        }
    }
}

@Composable
private fun MarkdownEdit(
    textFieldValue: TextFieldValue,
    entityColors: Map<String, String>,
    onValueChange: (TextFieldValue) -> Unit,
    onFormat: (String) -> Unit,
    onOpenEntityPopout: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val markdownTransformation = remember(primaryColor, textFieldValue.selection, entityColors) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val (annotated, mapping) = parseMarkdown(text.text, primaryColor, textFieldValue.selection, entityColors)
                return TransformedText(annotated, mapping)
            }
        }
    }

    ContextMenuDataProvider(
        items = {
            val items = mutableListOf(
                ContextMenuItem("Bold") { onFormat("**") },
                ContextMenuItem("Italic") { onFormat("*") },
                ContextMenuItem("Underline") { onFormat("__") }
            )
            
            val text = textFieldValue.text
            val uniqueEntities = Regex("(?<!\\\\)@\\[(.*?)\\]").findAll(text).map { it.groups[1]!!.value }.toSet()
            
            if (uniqueEntities.isNotEmpty()) {
                uniqueEntities.forEach { entityName ->
                    items.add(0, ContextMenuItem("Open Entity '$entityName'") {
                        onOpenEntityPopout(entityName)
                    })
                }
            }
            
            items
        }
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = markdownTransformation
        )
    }
}

@Composable
private fun MarkdownPreview(content: String, entityColors: Map<String, String>, onOpenEntityPopout: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val (baseAnnotatedString, _) = remember(content, primaryColor, entityColors) {
        parseMarkdown(content, primaryColor, null, entityColors) // null selection hides all syntax
    }
    
    // Convert string annotations to LinkAnnotations for hover/click support
    val finalAnnotatedString = remember(baseAnnotatedString, entityColors, primaryColor, onOpenEntityPopout) {
        val builder = AnnotatedString.Builder(baseAnnotatedString)
        baseAnnotatedString.getStringAnnotations("entity", 0, baseAnnotatedString.length).forEach { annotation ->
            val entityName = annotation.item
            val hexColor = entityColors[entityName]
            val color = if (hexColor != null) {
                try { Color(hexColor.toULong()) } catch(e: Exception) { primaryColor }
            } else primaryColor

            builder.addLink(
                androidx.compose.ui.text.LinkAnnotation.Clickable(
                    tag = entityName,
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(
                            color = color,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = {
                        onOpenEntityPopout(entityName)
                    }
                ),
                annotation.start,
                annotation.end
            )
        }
        builder.toAnnotatedString()
    }

    Text(
        text = finalAnnotatedString,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
    )
}

private fun parseMarkdown(
    text: String,
    primaryColor: Color,
    cursorSelection: androidx.compose.ui.text.TextRange?,
    entityColors: Map<String, String> = emptyMap()
): Pair<AnnotatedString, OffsetMapping> {
    val hiddenRanges = mutableListOf<IntRange>()
    val styles = mutableListOf<Triple<SpanStyle, Int, Int>>()
    val entityAnnotations = mutableListOf<Triple<String, Int, Int>>()

    fun isCursorInside(range: IntRange): Boolean {
        if (cursorSelection == null) return false
        return cursorSelection.start <= range.last + 1 && cursorSelection.end >= range.first - 1
    }

    // Bold (**text**)
    Regex("\\*\\*(.*?)\\*\\*").findAll(text).forEach { match ->
        styles.add(Triple(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1))
        if (!isCursorInside(match.range)) {
            hiddenRanges.add(match.range.first..match.range.first + 1)
            hiddenRanges.add(match.range.last - 1..match.range.last)
        }
    }

    // Italic (*text*)
    Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)").findAll(text).forEach { match ->
        styles.add(Triple(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1))
        if (!isCursorInside(match.range)) {
            hiddenRanges.add(match.range.first..match.range.first)
            hiddenRanges.add(match.range.last..match.range.last)
        }
    }

    // Underline (__text__)
    Regex("__(.*?)__").findAll(text).forEach { match ->
        styles.add(Triple(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), match.range.first, match.range.last + 1))
        if (!isCursorInside(match.range)) {
            hiddenRanges.add(match.range.first..match.range.first + 1)
            hiddenRanges.add(match.range.last - 1..match.range.last)
        }
    }

    // Entities (@[EntityName])
    Regex("(?<!\\\\)@\\[(.*?)\\]").findAll(text).forEach { match ->
        val entityName = match.groups[1]!!.value
        val hexColor = entityColors[entityName]
        val color = if (hexColor != null) {
            try { Color(hexColor.toULong()) } catch(e: Exception) { primaryColor }
        } else primaryColor
        
        styles.add(Triple(
            SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
            match.range.first, match.range.last + 1
        ))
        entityAnnotations.add(Triple(entityName, match.range.first, match.range.last + 1))
        if (!isCursorInside(match.range)) {
            hiddenRanges.add(match.range.first..match.range.first + 1)
            hiddenRanges.add(match.range.last..match.range.last)
        }
    }

    // Headings
    Regex("^(#{1,6}) (.*)$", RegexOption.MULTILINE).findAll(text).forEach { match ->
        val hashes = match.groups[1]?.value ?: ""
        val fontSize = when (hashes.length) {
            1 -> 24.sp
            2 -> 20.sp
            else -> 18.sp
        }
        styles.add(Triple(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold, fontSize = fontSize), match.range.first, match.range.last + 1))
        
        // Only hide the "# " in Preview mode
        if (cursorSelection == null) {
            val hashRange = match.groups[1]!!.range
            val spaceIndex = hashRange.last + 1
            hiddenRanges.add(hashRange.first..spaceIndex)
        }
    }

    val simplifiedHidden = hiddenRanges.flatMap { it.toList() }.toSet()

    val origLength = text.length
    val origToTrans = IntArray(origLength + 1)
    val transToOrig = mutableListOf<Int>()
    val builder = AnnotatedString.Builder()

    for (i in 0 until origLength) {
        origToTrans[i] = transToOrig.size
        if (i !in simplifiedHidden) {
            builder.append(text[i])
            transToOrig.add(i)
        }
    }
    origToTrans[origLength] = transToOrig.size
    transToOrig.add(origLength)

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = origToTrans[offset.coerceIn(0, origLength)]
        override fun transformedToOriginal(offset: Int): Int = transToOrig[offset.coerceIn(0, transToOrig.size - 1)]
    }

    styles.forEach { (style, origStart, origEnd) ->
        val transStart = origToTrans[origStart]
        val transEnd = origToTrans[origEnd]
        if (transStart < transEnd) {
            builder.addStyle(style, transStart, transEnd)
        }
    }

    entityAnnotations.forEach { (entityName, origStart, origEnd) ->
        val transStart = origToTrans[origStart]
        val transEnd = origToTrans[origEnd]
        if (transStart < transEnd) {
            builder.addStringAnnotation("entity", entityName, transStart, transEnd)
        }
    }

    return Pair(builder.toAnnotatedString(), offsetMapping)
}
