package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.ui.theme.LocalFantasyFontFamily
import de.luca.dungeon_master_manager.ui.theme.tiledTexture
import dungeon_master_manager.shared.generated.resources.Res
import dungeon_master_manager.shared.generated.resources.grimoire_vellum
import de.luca.dungeonmastermanager.database.Entity

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntityPopout: (String) -> Unit = {},
    entities: List<Entity> = emptyList()
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(content))
    }

    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            val start = textFieldValue.selection.start.coerceIn(0, content.length)
            val end = textFieldValue.selection.end.coerceIn(0, content.length)
            textFieldValue = TextFieldValue(text = content, selection = TextRange(start, end))
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
            val newSelection = TextRange(selection.min, selection.min + unformatted.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            return
        }

        // Case 2: The user highlighted the text inside the formatting
        if (before.endsWith(formatSyntax) && after.startsWith(formatSyntax)) {
            val newBefore = before.substring(0, before.length - formatSyntax.length)
            val newAfter = after.substring(formatSyntax.length)
            val newContent = "$newBefore$selected$newAfter"
            val newSelection = TextRange(newBefore.length, newBefore.length + selected.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            return
        }
        
        val newContent = "$before$formatSyntax$selected$formatSyntax$after"
        
        val newSelection = if (selected.isEmpty()) {
            TextRange(selection.min + formatSyntax.length)
        } else {
            TextRange(selection.min, selection.max + formatSyntax.length * 2)
        }
        
        textFieldValue = TextFieldValue(newContent, newSelection)
        onContentChange(newContent)
    }

    fun toggleOrInsertHeading() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val cursor = selection.min.coerceIn(0, text.length)
        
        val lineStart = if (cursor == 0) 0 else {
            val prevNl = text.lastIndexOf('\n', cursor - 1)
            if (prevNl == -1) 0 else prevNl + 1
        }
        val nextNl = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val lineText = text.substring(lineStart, nextNl)
        
        val headingMatch = Regex("^(#{1,6}) (.*)$").find(lineText)
        val (newText, newCursor) = if (headingMatch != null) {
            val hashes = headingMatch.groups[1]!!.value
            val rest = headingMatch.groups[2]!!.value
            if (hashes.length < 6) {
                // Increase heading level (# -> ## -> ### ...)
                val updatedLine = "#$hashes $rest"
                val updated = text.substring(0, lineStart) + updatedLine + text.substring(nextNl)
                Pair(updated, (cursor + 1).coerceAtMost(updated.length))
            } else {
                // At level 6, cycling removes the heading syntax back to normal text
                val updatedLine = rest
                val updated = text.substring(0, lineStart) + updatedLine + text.substring(nextNl)
                val diff = hashes.length + 1
                Pair(updated, (cursor - diff).coerceIn(lineStart, updated.length))
            }
        } else {
            // Insert "# " at the beginning of the line
            val updatedLine = "# $lineText"
            val updated = text.substring(0, lineStart) + updatedLine + text.substring(nextNl)
            Pair(updated, (cursor + 2).coerceAtMost(updated.length))
        }
        
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        onContentChange(newText)
    }

    Column(modifier = modifier.fillMaxSize().background(GrimoireColors.Background)) {
        // Toolbar with Live-Preview actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GrimoireColors.SurfaceLow)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { toggleOrInsertHeading() },
                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.GoldPrimary),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 8.dp).defaultMinSize(minHeight = 30.dp)
            ) {
                Text("+ Heading", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { applyFormat("**") },
                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.ParchmentLight),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 4.dp).defaultMinSize(minWidth = 32.dp, minHeight = 30.dp)
            ) {
                Text("B", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { applyFormat("*") },
                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.ParchmentLight),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 4.dp).defaultMinSize(minWidth = 32.dp, minHeight = 30.dp)
            ) {
                Text("I", fontStyle = FontStyle.Italic, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { applyFormat("__") },
                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.ParchmentLight),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 4.dp).defaultMinSize(minWidth = 32.dp, minHeight = 30.dp)
            ) {
                Text("U", textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        GrimoireDivider()

        val focusRequester = remember { FocusRequester() }
        val verticalScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                }
                .padding(16.dp)
        ) {
            MarkdownEdit(
                textFieldValue = textFieldValue,
                entities = entities,
                onOpenEntityPopout = onOpenEntityPopout,
                focusRequester = focusRequester,
                onValueChange = { newValue ->
                    val updatedSelection = expandMarkdownTokenSelection(newValue.text, newValue.selection)
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MarkdownEdit(
    textFieldValue: TextFieldValue,
    entities: List<Entity>,
    onValueChange: (TextFieldValue) -> Unit,
    onFormat: (String) -> Unit,
    onOpenEntityPopout: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val primaryColor = GrimoireColors.GoldPrimary
    val fantasyFamily = LocalFantasyFontFamily.current

    val (annotatedText, currentOffsetMapping) = remember(
        primaryColor,
        textFieldValue.text,
        textFieldValue.selection,
        entities,
        fantasyFamily
    ) {
        parseMarkdown(textFieldValue.text, primaryColor, textFieldValue.selection, entities, fantasyFamily)
    }

    val markdownTransformation = remember(annotatedText, currentOffsetMapping) {
        VisualTransformation {
            TransformedText(annotatedText, currentOffsetMapping)
        }
    }

    var dropdownQuery by remember { mutableStateOf<String?>(null) }
    var dropdownDismissed by remember { mutableStateOf(false) }
    var selectedDropdownIndex by remember { mutableIntStateOf(0) }
    var lastInsideEntityRange by remember { mutableStateOf<IntRange?>(null) }
    val dropdownListState = rememberLazyListState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val displayEntities = remember(dropdownQuery, entities) {
        val q = dropdownQuery
        if (q == null) {
            emptyList()
        } else if (q.isBlank()) {
            entities.take(10)
        } else {
            entities.filter { it.name.contains(q, ignoreCase = true) }.take(10)
        }
    }

    val isDropdownActive = dropdownQuery != null && !dropdownDismissed && displayEntities.isNotEmpty()
    val clampedSelectedIndex = if (displayEntities.isEmpty()) 0 else selectedDropdownIndex.coerceIn(0, displayEntities.lastIndex)

    LaunchedEffect(clampedSelectedIndex, isDropdownActive) {
        if (isDropdownActive && displayEntities.isNotEmpty()) {
            dropdownListState.animateScrollToItem(clampedSelectedIndex)
        }
    }

    // Sticky-X state for reliable vertical arrow navigation across lines of varying lengths
    var stickyX by remember { mutableStateOf<Float?>(null) }
    var lastVerticalText by remember { mutableStateOf<String?>(null) }
    var lastVerticalSelection by remember { mutableStateOf<TextRange?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var scrollToCursorTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollToCursorTrigger) {
        if (scrollToCursorTrigger > 0) {
            // Wait one frame so onTextLayout reflects any expanded/collapsed syntax on the target line
            withFrameNanos { }
            val layout = textLayoutResult ?: return@LaunchedEffect
            val activeOrig = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
            val activeTrans = currentOffsetMapping
                .originalToTransformed(activeOrig)
                .coerceIn(0, annotatedText.length)
            val cursorRect = layout.getCursorRect(activeTrans)
            val paddedRect = Rect(
                left = cursorRect.left,
                top = (cursorRect.top - 12f).coerceAtLeast(0f),
                right = cursorRect.right + 4f,
                bottom = cursorRect.bottom + 12f
            )
            bringIntoViewRequester.bringIntoView(paddedRect)
        }
    }
    
    LaunchedEffect(textFieldValue.text, textFieldValue.selection) {
        val cursor = textFieldValue.selection.start
        if (cursor > 0 && textFieldValue.selection.collapsed) {
            val text = textFieldValue.text
            val insideEntityMatch = ENTITY_MENTION_REGEX.findAll(text).find { match ->
                cursor in (match.range.first + 1)..match.range.last
            }

            if (insideEntityMatch != null) {
                if (dropdownQuery != "" || insideEntityMatch.range != lastInsideEntityRange) {
                    lastInsideEntityRange = insideEntityMatch.range
                    dropdownQuery = ""
                    val currentId = insideEntityMatch.groups[1]?.value
                    val matchIdx = displayEntities.indexOfFirst { it.id == currentId }
                    selectedDropdownIndex = if (matchIdx >= 0) matchIdx else 0
                    dropdownDismissed = false
                }
            } else {
                lastInsideEntityRange = null
                val textBeforeCursor = text.substring(0, cursor)
                val match = Regex(".*@([\\w\\s]*)$").find(textBeforeCursor)
                if (match != null) {
                    val newQuery = match.groups[1]?.value ?: ""
                    if (newQuery != dropdownQuery) {
                        dropdownQuery = newQuery
                        selectedDropdownIndex = 0
                        dropdownDismissed = false
                    }
                } else {
                    dropdownQuery = null
                    dropdownDismissed = false
                    selectedDropdownIndex = 0
                }
            }
        } else {
            lastInsideEntityRange = null
            dropdownQuery = null
            dropdownDismissed = false
            selectedDropdownIndex = 0
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .tiledTexture(Res.drawable.grimoire_vellum, alpha = 0.08f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                stickyX = null
                lastVerticalSelection = null
                val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                if (isCtrl && event.button == PointerButton.Primary) {
                    val clickPos = event.changes.firstOrNull()?.position
                    val layout = textLayoutResult
                    if (clickPos != null && layout != null) {
                        val transOffset = layout.getOffsetForPosition(clickPos)
                        val origOffset = currentOffsetMapping.transformedToOriginal(transOffset)
                        val entityMatch = ENTITY_MENTION_REGEX.findAll(textFieldValue.text)
                            .find { origOffset in it.range.first..(it.range.last + 1) }
                        if (entityMatch != null) {
                            val entId = entityMatch.groups[1]!!.value
                            val ent = entities.find { it.id == entId }
                            if (ent != null) {
                                onOpenEntityPopout(ent.name)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        ContextMenuDataProvider(
            items = {
                val items = mutableListOf(
                    ContextMenuItem("Bold") { onFormat("**") },
                    ContextMenuItem("Italic") { onFormat("*") },
                    ContextMenuItem("Underline") { onFormat("__") }
                )
                
                val text = textFieldValue.text
                val curOffset = textFieldValue.selection.start
                
                // If cursor is on a specific entity, put it first in the context menu
                val activeEntityMatch = ENTITY_MENTION_REGEX.findAll(text)
                    .find { curOffset in it.range.first..(it.range.last + 1) }
                if (activeEntityMatch != null) {
                    val entId = activeEntityMatch.groups[1]!!.value
                    val ent = entities.find { it.id == entId }
                    if (ent != null) {
                        items.add(0, ContextMenuItem("Open Entity '${ent.name}'") {
                            onOpenEntityPopout(ent.name)
                        })
                    }
                } else {
                    val uniqueEntities = ENTITY_MENTION_REGEX.findAll(text).map { it.groups[1]!!.value }.toSet()
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
                }
                
                items
            }
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        val key = keyEvent.key
                        val hasCommandModifier = keyEvent.isCtrlPressed || keyEvent.isAltPressed || keyEvent.isMetaPressed

                        if (isDropdownActive && !hasCommandModifier) {
                            when (key) {
                                Key.DirectionDown -> {
                                    selectedDropdownIndex = (clampedSelectedIndex + 1) % displayEntities.size
                                    return@onPreviewKeyEvent true
                                }
                                Key.DirectionUp -> {
                                    selectedDropdownIndex = (clampedSelectedIndex - 1 + displayEntities.size) % displayEntities.size
                                    return@onPreviewKeyEvent true
                                }
                                Key.Enter, Key.NumPadEnter, Key.Tab -> {
                                    val selectedEntity = displayEntities.getOrNull(clampedSelectedIndex)
                                    if (selectedEntity != null) {
                                        val (newContent, newSel) = replaceEntityQueryWithLink(
                                            text = textFieldValue.text,
                                            cursor = textFieldValue.selection.start,
                                            query = dropdownQuery ?: "",
                                            entityId = selectedEntity.id
                                        )
                                        dropdownQuery = null
                                        dropdownDismissed = false
                                        onValueChange(TextFieldValue(newContent, newSel))
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                Key.Escape -> {
                                    dropdownDismissed = true
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }

                        if (!hasCommandModifier && (key == Key.DirectionUp || key == Key.DirectionDown)) {
                            val layout = textLayoutResult ?: return@onPreviewKeyEvent false
                            val direction = if (key == Key.DirectionUp) -1 else 1
                            val validStickyX = if (
                                textFieldValue.text == lastVerticalText &&
                                textFieldValue.selection == lastVerticalSelection
                            ) {
                                stickyX
                            } else {
                                null
                            }

                            val moveResult = calculateVerticalMove(
                                originalText = textFieldValue.text,
                                transformedText = annotatedText.text,
                                currentSelection = textFieldValue.selection,
                                direction = direction,
                                extendSelection = keyEvent.isShiftPressed,
                                cachedStickyX = validStickyX,
                                layout = layout.asVerticalTextLayout(),
                                offsetMapping = currentOffsetMapping
                            )

                            val finalSelection = expandMarkdownTokenSelection(textFieldValue.text, moveResult.selection)
                            stickyX = moveResult.stickyX
                            lastVerticalText = textFieldValue.text
                            lastVerticalSelection = finalSelection
                            onValueChange(textFieldValue.copy(selection = finalSelection))
                            scrollToCursorTrigger++
                            true
                        } else {
                            val isModifierOnly = key == Key.ShiftLeft || key == Key.ShiftRight ||
                                key == Key.CtrlLeft || key == Key.CtrlRight ||
                                key == Key.AltLeft || key == Key.AltRight ||
                                key == Key.MetaLeft || key == Key.MetaRight ||
                                key == Key.CapsLock
                            if (!isModifierOnly) {
                                stickyX = null
                                lastVerticalSelection = null
                            }
                            false
                        }
                    },
                textStyle = TextStyle(
                    color = GrimoireColors.ParchmentLight,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(GrimoireColors.GoldPrimary),
                visualTransformation = markdownTransformation
            )
        }

        if (isDropdownActive) {
            val density = LocalDensity.current
            val maxContainerWidthPx = with(density) { maxWidth.toPx() }
            val menuWidthPx = with(density) { 260.dp.toPx() }

            val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
            val transCursor = currentOffsetMapping.originalToTransformed(cursor).coerceIn(0, annotatedText.length)
            val cursorRect = textLayoutResult?.getCursorRect(transCursor)

            val popupX = if (cursorRect != null) {
                cursorRect.left.coerceIn(0f, (maxContainerWidthPx - menuWidthPx).coerceAtLeast(0f))
            } else {
                0f
            }
            val popupY = if (cursorRect != null) {
                cursorRect.bottom + with(density) { 4.dp.toPx() }
            } else {
                with(density) { 24.dp.toPx() }
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(popupX.roundToInt(), popupY.roundToInt()) }
                    .width(260.dp)
                    .heightIn(max = 220.dp)
                    .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(6.dp)),
                color = GrimoireColors.SurfaceCard,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(6.dp)
            ) {
                LazyColumn(state = dropdownListState) {
                    itemsIndexed(displayEntities) { index, ent ->
                        val isSelected = index == clampedSelectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) GrimoireColors.GoldPrimary.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    val (newContent, newSel) = replaceEntityQueryWithLink(
                                        text = textFieldValue.text,
                                        cursor = textFieldValue.selection.start,
                                        query = dropdownQuery ?: "",
                                        entityId = ent.id
                                    )
                                    dropdownQuery = null
                                    dropdownDismissed = false
                                    onValueChange(TextFieldValue(newContent, newSel))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val c = try { Color(ent.color?.toULong() ?: 0xFFFFFFFFuL) } catch(e: Exception) { GrimoireColors.GoldPrimary }
                            Box(modifier = Modifier.size(10.dp).background(c, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                ent.name,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "(${ent.type})",
                                color = if (isSelected) GrimoireColors.ParchmentLight else GrimoireColors.ParchmentMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

internal val ENTITY_MENTION_REGEX = Regex("(?<!\\\\)@\\[(.*?)\\]")

internal fun replaceEntityQueryWithLink(
    text: String,
    cursor: Int,
    query: String,
    entityId: String
): Pair<String, TextRange> {
    val atIndex = (cursor - 1 - query.length).coerceAtLeast(0)

    // Check if cursor or atIndex is within or touching an existing @[entityId] mention
    val existingMatch = ENTITY_MENTION_REGEX.findAll(text).find { match ->
        cursor in match.range.first..(match.range.last + 1) || atIndex == match.range.first
    }

    val (before, after) = if (existingMatch != null) {
        Pair(
            text.substring(0, existingMatch.range.first),
            text.substring(existingMatch.range.last + 1)
        )
    } else {
        val b = text.substring(0, atIndex)
        val rawAfter = text.substring(cursor.coerceIn(0, text.length))
        val bracketMatch = Regex("^\\[[^\\]\\r\\n]*\\]").find(rawAfter)
        val a = if (bracketMatch != null) {
            rawAfter.substring(bracketMatch.range.last + 1)
        } else {
            rawAfter
        }
        Pair(b, a)
    }

    val newContent = "$before@[${entityId}]$after"
    val newCursor = before.length + entityId.length + 3
    return Pair(newContent, TextRange(newCursor))
}

internal fun isEntityExpanded(matchRange: IntRange, cursorSelection: TextRange?): Boolean {
    if (cursorSelection == null) return false
    return (cursorSelection.min > matchRange.first && cursorSelection.max <= matchRange.last) ||
        (cursorSelection.min <= matchRange.first && cursorSelection.max > matchRange.last) ||
        (!cursorSelection.collapsed && cursorSelection.min <= matchRange.last && cursorSelection.max >= matchRange.first)
}

internal fun expandMarkdownTokenSelection(text: String, selection: TextRange): TextRange {
    if (selection.collapsed) return selection
    val min = selection.min.coerceIn(0, text.length)
    val max = selection.max.coerceIn(0, text.length)
    val before = text.substring(0, min)
    val after = text.substring(max)

    fun expandPreservingDirection(leftDelta: Int, rightDelta: Int): TextRange {
        val newMin = min - leftDelta
        val newMax = max + rightDelta
        return if (selection.reversed) TextRange(newMax, newMin) else TextRange(newMin, newMax)
    }

    return when {
        before.endsWith("**") && after.startsWith("**") -> expandPreservingDirection(2, 2)
        before.endsWith("__") && after.startsWith("__") -> expandPreservingDirection(2, 2)
        before.endsWith("*") && after.startsWith("*") && !before.endsWith("**") && !after.startsWith("**") ->
            expandPreservingDirection(1, 1)
        before.endsWith("@[") && after.startsWith("]") -> expandPreservingDirection(2, 1)
        else -> selection
    }
}

internal interface VerticalTextLayout {
    val lineCount: Int
    fun getLineForOffset(offset: Int): Int
    fun getCursorHorizontal(offset: Int): Float
    fun getLineLeft(lineIndex: Int): Float
    fun getLineRight(lineIndex: Int): Float
    fun getLineTop(lineIndex: Int): Float
    fun getLineBottom(lineIndex: Int): Float
    fun getLineStart(lineIndex: Int): Int
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int
    fun getOffsetForPosition(x: Float, y: Float): Int
}

internal fun TextLayoutResult.asVerticalTextLayout(): VerticalTextLayout = object : VerticalTextLayout {
    override val lineCount: Int get() = this@asVerticalTextLayout.lineCount
    override fun getLineForOffset(offset: Int): Int = this@asVerticalTextLayout.getLineForOffset(offset)
    override fun getCursorHorizontal(offset: Int): Float = this@asVerticalTextLayout.getCursorRect(offset).left
    override fun getLineLeft(lineIndex: Int): Float = this@asVerticalTextLayout.getLineLeft(lineIndex)
    override fun getLineRight(lineIndex: Int): Float = this@asVerticalTextLayout.getLineRight(lineIndex)
    override fun getLineTop(lineIndex: Int): Float = this@asVerticalTextLayout.getLineTop(lineIndex)
    override fun getLineBottom(lineIndex: Int): Float = this@asVerticalTextLayout.getLineBottom(lineIndex)
    override fun getLineStart(lineIndex: Int): Int = this@asVerticalTextLayout.getLineStart(lineIndex)
    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int =
        this@asVerticalTextLayout.getLineEnd(lineIndex, visibleEnd)
    override fun getOffsetForPosition(x: Float, y: Float): Int =
        this@asVerticalTextLayout.getOffsetForPosition(Offset(x, y))
}

internal data class VerticalMoveResult(
    val selection: TextRange,
    val stickyX: Float
)

internal fun calculateVerticalMove(
    originalText: String,
    transformedText: String,
    currentSelection: TextRange,
    direction: Int,
    extendSelection: Boolean,
    cachedStickyX: Float?,
    layout: VerticalTextLayout,
    offsetMapping: OffsetMapping
): VerticalMoveResult {
    require(direction == -1 || direction == 1) { "direction must be -1 or +1" }

    if (originalText.isEmpty() || layout.lineCount <= 0) {
        return VerticalMoveResult(TextRange(0), cachedStickyX ?: 0f)
    }

    val activeOrigOffset = when {
        extendSelection -> currentSelection.end
        !currentSelection.collapsed -> if (direction < 0) currentSelection.min else currentSelection.max
        else -> currentSelection.end
    }.coerceIn(0, originalText.length)

    val currentTransOffset = offsetMapping.originalToTransformed(activeOrigOffset)
        .coerceIn(0, transformedText.length)

    val desiredX = cachedStickyX ?: layout.getCursorHorizontal(currentTransOffset)
    val currentLine = layout.getLineForOffset(currentTransOffset)

    // Exit a soft-wrapped expanded @[entityId] link in a single vertical press
    val activeEntityMatch = ENTITY_MENTION_REGEX.findAll(originalText).find { match ->
        isEntityExpanded(match.range, currentSelection) &&
            activeOrigOffset in match.range.first..(match.range.last + 1)
    }

    val targetLine = if (activeEntityMatch != null) {
        val transEntityStart = offsetMapping.originalToTransformed(activeEntityMatch.range.first)
            .coerceIn(0, transformedText.length)
        val transEntityLastChar = offsetMapping.originalToTransformed(activeEntityMatch.range.last)
            .coerceIn(0, transformedText.length)
        val entityStartLine = layout.getLineForOffset(transEntityStart)
        val entityEndLine = layout.getLineForOffset(transEntityLastChar)
        if (entityStartLine < entityEndLine) {
            if (direction < 0) entityStartLine - 1 else entityEndLine + 1
        } else {
            currentLine + direction
        }
    } else {
        currentLine + direction
    }

    val newOrigOffset = when {
        targetLine < 0 -> 0
        targetLine >= layout.lineCount -> originalText.length
        else -> {
            val lineLeft = layout.getLineLeft(targetLine)
            val lineRight = layout.getLineRight(targetLine)

            val targetTransOffset = when {
                desiredX >= lineRight -> resolveVisualLineEndOffset(transformedText, layout, targetLine)
                desiredX <= lineLeft -> layout.getLineStart(targetLine)
                else -> {
                    val centerY = (layout.getLineTop(targetLine) + layout.getLineBottom(targetLine)) / 2f
                    layout.getOffsetForPosition(desiredX, centerY)
                }
            }.coerceIn(0, transformedText.length)

            offsetMapping.transformedToOriginal(targetTransOffset).coerceIn(0, originalText.length)
        }
    }

    val newSelection = if (extendSelection) {
        TextRange(currentSelection.start, newOrigOffset)
    } else {
        TextRange(newOrigOffset)
    }

    return VerticalMoveResult(selection = newSelection, stickyX = desiredX)
}

private fun resolveVisualLineEndOffset(
    transformedText: String,
    layout: VerticalTextLayout,
    targetLine: Int
): Int {
    val lineStart = layout.getLineStart(targetLine)
    val rawEnd = layout.getLineEnd(targetLine, visibleEnd = false)

    if (rawEnd <= lineStart) {
        return lineStart
    }

    if (rawEnd <= transformedText.length && transformedText[rawEnd - 1] == '\n') {
        var endBeforeNewline = rawEnd - 1
        if (endBeforeNewline > lineStart && transformedText[endBeforeNewline - 1] == '\r') {
            endBeforeNewline -= 1
        }
        return endBeforeNewline
    }

    if (targetLine == layout.lineCount - 1) {
        return rawEnd.coerceAtMost(transformedText.length)
    }

    val visibleEnd = layout.getLineEnd(targetLine, visibleEnd = true)
    return if (visibleEnd < rawEnd) {
        visibleEnd.coerceAtLeast(lineStart)
    } else {
        (rawEnd - 1).coerceAtLeast(lineStart)
    }
}

internal fun parseMarkdown(
    text: String,
    primaryColor: Color,
    cursorSelection: TextRange?,
    entities: List<Entity> = emptyList(),
    fantasyFamily: FontFamily = FontFamily.Serif
): Pair<AnnotatedString, OffsetMapping> {
    val builder = AnnotatedString.Builder()
    
    val origToTrans = mutableListOf<Int>()
    val transToOrig = mutableListOf<Int>()

    var cursor = 0

    // Token-level focus: check if cursor is touching or inside inline formatting range
    fun isCursorInside(range: IntRange): Boolean {
        if (cursorSelection == null) return false
        return cursorSelection.min <= range.last && cursorSelection.max >= range.first
    }

    // Line-level focus: check if cursor is anywhere on the line of a block element (e.g. heading)
    fun isCursorInLine(range: IntRange): Boolean {
        if (cursorSelection == null) return false
        return cursorSelection.min <= range.last + 1 && cursorSelection.max >= range.first
    }

    val regex = Regex(
        "\\*\\*(.*?)\\*\\*|(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|__(.*?)__|(?<!\\\\)@\\[(.*?)\\]|^(#{1,6}) (.*)$",
        RegexOption.MULTILINE
    )
    
    val matches = regex.findAll(text)

    for (match in matches) {
        // Append text before match
        for (i in cursor until match.range.first) {
            origToTrans.add(builder.length)
            builder.append(text[i])
            transToOrig.add(i)
        }
        
        cursor = match.range.last + 1
        val origStart = match.range.first

        if (match.value.startsWith("**")) {
            val content = match.groups[1]!!.value
            val isCursorIn = isCursorInside(match.range)
            if (!isCursorIn) {
                // Hide leading **
                origToTrans.add(builder.length)
                origToTrans.add(builder.length)
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + 2 + i)
                }
                // Hide trailing **
                origToTrans.add(builder.length)
                origToTrans.add(builder.length)
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = GrimoireColors.ParchmentLight),
                    transStart,
                    builder.length
                )
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
            val isCursorIn = isCursorInside(match.range)
            if (!isCursorIn) {
                // Hide leading __
                origToTrans.add(builder.length)
                origToTrans.add(builder.length)
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + 2 + i)
                }
                // Hide trailing __
                origToTrans.add(builder.length)
                origToTrans.add(builder.length)
                builder.addStyle(
                    SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                    transStart,
                    builder.length
                )
            } else {
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(
                    SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                    transStart,
                    builder.length
                )
            }
        } else if (match.value.startsWith("*") && !match.value.startsWith("**")) {
            val content = match.groups[2]!!.value
            val isCursorIn = isCursorInside(match.range)
            if (!isCursorIn) {
                // Hide leading *
                origToTrans.add(builder.length)
                val transStart = builder.length
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + 1 + i)
                }
                // Hide trailing *
                origToTrans.add(builder.length)
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    transStart,
                    builder.length
                )
            } else {
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    transStart,
                    builder.length
                )
            }
        } else if (match.value.startsWith("@[") || match.value.startsWith("\\@[")) {
            val isEscaped = match.value.startsWith("\\")
            val idGroup = match.groups[4]
            if (idGroup != null && !isEscaped) {
                val id = idGroup.value
                val ent = entities.find { it.id == id }
                val color = if (ent?.color != null) {
                    try { Color(ent.color.toULong()) } catch (e: Exception) { primaryColor }
                } else GrimoireColors.CrimsonError
                
                val displayText = ent?.name ?: "Unknown Entity"
                // An entity reference @[id] is expanded when the cursor is strictly inside the token or selecting it
                val isCursorIn = isEntityExpanded(match.range, cursorSelection)

                if (!isCursorIn) {
                    val transStart = builder.length

                    for (i in match.range) {
                        origToTrans.add(transStart)
                    }

                    for (i in displayText.indices) {
                        val mappedOrig = if (i == 0) match.range.first else match.range.first + 1
                        builder.append(displayText[i])
                        transToOrig.add(mappedOrig)
                    }

                    builder.addStyle(
                        SpanStyle(
                            color = color,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        transStart,
                        builder.length
                    )
                    builder.addStringAnnotation("entity", id, transStart, builder.length)
                } else {
                    val transStart = builder.length
                    for (i in match.range) {
                        origToTrans.add(builder.length)
                        builder.append(text[i])
                        transToOrig.add(i)
                    }
                    builder.addStyle(
                        SpanStyle(
                            color = color,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        transStart,
                        builder.length
                    )
                    builder.addStringAnnotation("entity", id, transStart, builder.length)
                }
            } else {
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
                1 -> 26.sp
                2 -> 22.sp
                else -> 18.sp
            }
            val prefixLen = hashes.length + 1 // hashes plus space
            val isCursorOnLine = isCursorInLine(match.range)

            if (!isCursorOnLine) {
                // Hide '# ' prefix when cursor is not on the heading line
                val transStart = builder.length
                for (i in 0 until prefixLen) {
                    origToTrans.add(transStart)
                }
                for (i in content.indices) {
                    origToTrans.add(builder.length)
                    builder.append(content[i])
                    transToOrig.add(origStart + prefixLen + i)
                }
                builder.addStyle(
                    SpanStyle(
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        fontFamily = fantasyFamily
                    ),
                    transStart,
                    builder.length
                )
            } else {
                // Reveal '# ' prefix when cursor is on the heading line
                val transStart = builder.length
                for (i in match.range) {
                    origToTrans.add(builder.length)
                    builder.append(text[i])
                    transToOrig.add(i)
                }
                builder.addStyle(
                    SpanStyle(
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        fontFamily = fantasyFamily
                    ),
                    transStart,
                    builder.length
                )
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
        override fun originalToTransformed(offset: Int): Int =
            origToTrans[offset.coerceIn(0, origToTrans.size - 1)]

        override fun transformedToOriginal(offset: Int): Int =
            transToOrig[offset.coerceIn(0, transToOrig.size - 1)]
    }

    return Pair(builder.toAnnotatedString(), offsetMapping)
}
