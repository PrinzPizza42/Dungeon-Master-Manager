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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import de.luca.dungeonmastermanager.database.Entity

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntityPopout: (String) -> Unit = {},
    entities: List<Entity> = emptyList()
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

        // Case 1: The user highlighted the entire formatted block
        if (selected.startsWith(formatSyntax) && selected.endsWith(formatSyntax) && selected.length >= formatSyntax.length * 2) {
            val unformatted = selected.substring(formatSyntax.length, selected.length - formatSyntax.length)
            val newContent = "$before$unformatted$after"
            val newSelection = androidx.compose.ui.text.TextRange(selection.min, selection.min + unformatted.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            return
        }

        // Case 2: The user highlighted the text inside the formatting
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
                MarkdownPreview(content, entities, onOpenEntityPopout)
            } else {
                MarkdownEdit(
                    textFieldValue = textFieldValue,
                    entities = entities,
                    onOpenEntityPopout = onOpenEntityPopout,
                    onValueChange = { newValue ->
                        var updatedSelection = newValue.selection
                        val text = newValue.text

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
    entities: List<Entity>,
    onValueChange: (TextFieldValue) -> Unit,
    onFormat: (String) -> Unit,
    onOpenEntityPopout: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val markdownTransformation = remember(primaryColor, textFieldValue.selection, entities) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val (annotated, mapping) = parseMarkdown(text.text, primaryColor, textFieldValue.selection, entities)
                return TransformedText(annotated, mapping)
            }
        }
    }

    var dropdownQuery by remember { mutableStateOf<String?>(null) }
    var dropdownStartOffset by remember { mutableStateOf(-1) }
    
    LaunchedEffect(textFieldValue.text, textFieldValue.selection) {
        val cursor = textFieldValue.selection.start
        if (cursor > 0 && textFieldValue.selection.collapsed) {
            val textBeforeCursor = textFieldValue.text.substring(0, cursor)
            val match = Regex(".*@([\\w\\s]*)$").find(textBeforeCursor)
            if (match != null) {
                dropdownQuery = match.groups[1]?.value ?: ""
                dropdownStartOffset = match.range.last - (match.groups[1]?.value?.length ?: 0)
            } else {
                dropdownQuery = null
            }
        } else {
            dropdownQuery = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    uniqueEntities.forEach { entityId ->
                        val ent = entities.find { it.id == entityId }
                        if (ent != null) {
                            items.add(0, ContextMenuItem("Open Entity '${ent.name}'") {
                                onOpenEntityPopout(ent.name)
                            })
                        }
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

        if (dropdownQuery != null) {
            val query = dropdownQuery!!
            val filtered = if (query.isBlank()) entities else entities.filter { it.name.contains(query, ignoreCase = true) }
            
            if (filtered.isNotEmpty()) {
                Surface(
                    modifier = Modifier.padding(top = 24.dp).width(250.dp).heightIn(max = 200.dp),
                    shadowElevation = 8.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    LazyColumn {
                        items(filtered.take(10)) { ent ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val text = textFieldValue.text
                                    val before = text.substring(0, dropdownStartOffset - 1) // include @
                                    val after = text.substring(textFieldValue.selection.start)
                                    val newContent = "$before@[${ent.id}]$after"
                                    val newSelection = androidx.compose.ui.text.TextRange(before.length + ent.id.length + 3)
                                    onValueChange(TextFieldValue(newContent, newSelection))
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val c = try { Color(ent.color?.toULong() ?: 0xFFFFFFFFuL) } catch(e: Exception) { Color.Gray }
                                Box(modifier = Modifier.size(12.dp).background(c, MaterialTheme.shapes.small))
                                Spacer(Modifier.width(8.dp))
                                Text(ent.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(8.dp))
                                Text("(${ent.type})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownPreview(content: String, entities: List<Entity>, onOpenEntityPopout: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val (baseAnnotatedString, _) = remember(content, primaryColor, entities) {
        parseMarkdown(content, primaryColor, null, entities) // null selection hides all syntax
    }
    
    val finalAnnotatedString = remember(baseAnnotatedString, entities, primaryColor, onOpenEntityPopout) {
        val builder = AnnotatedString.Builder(baseAnnotatedString)
        baseAnnotatedString.getStringAnnotations("entity", 0, baseAnnotatedString.length).forEach { annotation ->
            val entityId = annotation.item
            val ent = entities.find { it.id == entityId }
            val color = if (ent?.color != null) {
                try { Color(ent.color.toULong()) } catch(e: Exception) { primaryColor }
            } else primaryColor

            builder.addLink(
                androidx.compose.ui.text.LinkAnnotation.Clickable(
                    tag = entityId,
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(
                            color = color,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = {
                        if (ent != null) {
                            onOpenEntityPopout(ent.name)
                        }
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
    entities: List<Entity> = emptyList()
): Pair<AnnotatedString, OffsetMapping> {
    val builder = AnnotatedString.Builder()
    
    val origToTrans = mutableListOf<Int>()
    val transToOrig = mutableListOf<Int>()

    var cursor = 0

    fun isCursorInside(range: IntRange): Boolean {
        if (cursorSelection == null) return false
        return cursorSelection.start <= range.last + 1 && cursorSelection.end >= range.first - 1
    }

    // A simple regex approach to find all tokens in order
    // Tokenize: Bold, Italic, Underline, Entity, Headings
    val regex = Regex("\\*\\*(.*?)\\*\\*|(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|__(.*?)__|(?<!\\\\)@\\[(.*?)\\]|^(#{1,6}) (.*)$", RegexOption.MULTILINE)
    
    val matches = regex.findAll(text)

    for (match in matches) {
        // Append text before match
        for (i in cursor until match.range.first) {
            origToTrans.add(builder.length)
            builder.append(text[i])
            transToOrig.add(i)
        }
        
        cursor = match.range.last + 1

        val isCursorIn = isCursorInside(match.range)
        val origStart = match.range.first

        if (match.value.startsWith("**")) {
            val content = match.groups[1]!!.value
            if (!isCursorIn) {
                // hide **
                origToTrans.add(builder.length) // *
                origToTrans.add(builder.length) // *
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + 2 + i)
                }
                origToTrans.add(builder.length) // *
                origToTrans.add(builder.length) // *
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), transStart, builder.length)
            } else {
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), transStart, builder.length)
            }
        } else if (match.value.startsWith("__")) {
            val content = match.groups[3]!!.value
            if (!isCursorIn) {
                origToTrans.add(builder.length)
                origToTrans.add(builder.length)
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + 2 + i)
                }
                origToTrans.add(builder.length)
                origToTrans.add(builder.length)
                builder.addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), transStart, builder.length)
            } else {
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), transStart, builder.length)
            }
        } else if (match.value.startsWith("*") && !match.value.startsWith("**")) {
            val content = match.groups[2]!!.value
            if (!isCursorIn) {
                origToTrans.add(builder.length)
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + 1 + i)
                }
                origToTrans.add(builder.length)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), transStart, builder.length)
            } else {
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), transStart, builder.length)
            }
        } else if (match.value.startsWith("@[") || match.value.startsWith("\\@[")) {
            val isEscaped = match.value.startsWith("\\")
            val idGroup = match.groups[4]
            if (idGroup != null && !isEscaped) {
                val id = idGroup.value
                val ent = entities.find { it.id == id }
                val color = if (ent?.color != null) {
                    try { Color(ent.color.toULong()) } catch(e: Exception) { primaryColor }
                } else Color.Red
                
                val displayText = ent?.name ?: "Unknown Entity"

                if (!isCursorIn) {
                    val transStart = builder.length
                    
                    // Map entire original token to the start of the display text
                    for (i in match.range) {
                        origToTrans.add(builder.length)
                    }
                    
                    for (i in displayText.indices) {
                        builder.append(displayText[i])
                        transToOrig.add(match.range.last) // all transformed chars map to the end of the token
                    }

                    builder.addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), transStart, builder.length)
                    builder.addStringAnnotation("entity", id, transStart, builder.length)
                } else {
                    val transStart = builder.length
                    for (i in match.range) {
                        origToTrans.add(builder.length)
                        builder.append(text[i])
                        transToOrig.add(i)
                    }
                    builder.addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), transStart, builder.length)
                    builder.addStringAnnotation("entity", id, transStart, builder.length)
                }
            } else {
                // Just append raw
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
            }
        } else if (match.groups[5] != null) {
            val hashes = match.groups[5]!!.value
            val content = match.groups[6]!!.value
            val fontSize = when (hashes.length) {
                1 -> 24.sp
                2 -> 20.sp
                else -> 18.sp
            }
            if (cursorSelection == null) {
                // Preview mode: hide "# "
                for (i in 0..hashes.length) {
                    origToTrans.add(builder.length)
                }
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + hashes.length + 1 + i)
                }
                builder.addStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold, fontSize = fontSize), transStart, builder.length)
            } else {
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold, fontSize = fontSize), transStart, builder.length)
            }
        }
    }

    for (i in cursor until text.length) {
        origToTrans.add(builder.length)
        builder.append(text[i])
        transToOrig.add(i)
    }
    
    origToTrans.add(builder.length)
    transToOrig.add(text.length)

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = origToTrans[offset.coerceIn(0, text.length)]
        override fun transformedToOriginal(offset: Int): Int = transToOrig[offset.coerceIn(0, transToOrig.size - 1)]
    }

    return Pair(builder.toAnnotatedString(), offsetMapping)
}
