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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.ui.components.EditorFontSizeControl
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.ui.theme.LocalFantasyFontFamily
import de.luca.dungeon_master_manager.ui.theme.tiledTexture
import dungeon_master_manager.shared.generated.resources.Res
import dungeon_master_manager.shared.generated.resources.grimoire_vellum
import de.luca.dungeonmastermanager.database.Entity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import de.luca.dungeon_master_manager.data.ProjectRepository
import de.luca.dungeon_master_manager.ui.components.rememberEntityImage
import de.luca.dungeon_master_manager.ui.components.ImagePickerDialog
import java.io.File

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntityPopout: (String) -> Unit = {},
    onOpenImageInTab: (String) -> Unit = {},
    projectPath: String = "",
    entities: List<Entity> = emptyList()
) {
    val settings by AppSettingsRepository.settings.collectAsState()
    val fontSize = settings.editorFontSize
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

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

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var imageDialogInitialTitle by remember { mutableStateOf("") }
    var isImageDialogTriggeredByTyping by remember { mutableStateOf(false) }
    var typingTriggerRange by remember { mutableStateOf<TextRange?>(null) }
    var dismissedTypingTriggerPos by remember { mutableStateOf<Int?>(null) }

    fun openImagePickerFromToolbar() {
        val sel = textFieldValue.selection
        val selectedText = if (sel.collapsed) "" else textFieldValue.text.substring(sel.min, sel.max).trim()
        imageDialogInitialTitle = selectedText
        isImageDialogTriggeredByTyping = false
        typingTriggerRange = null
        showImagePickerDialog = true
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

    fun insertOrTriggerEntityLink() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val before = text.substring(0, selection.min)
        val selected = text.substring(selection.min, selection.max)
        val after = text.substring(selection.max)

        // Case 1: Already wrapped as @[id] -> unwrap back to inner text
        if (selected.startsWith("@[") && selected.endsWith("]") && selected.length >= 3) {
            val inner = selected.substring(2, selected.length - 1)
            val newContent = "$before$inner$after"
            val newSelection = TextRange(selection.min, selection.min + inner.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
            return
        }

        // Case 2: Highlighted text exactly matches an existing entity name -> link directly as @[id]
        val exactMatch = if (selected.isNotBlank()) {
            entities.find { it.name.equals(selected.trim(), ignoreCase = true) }
        } else null

        if (exactMatch != null) {
            val replacement = "@[${exactMatch.id}]"
            val newContent = "$before$replacement$after"
            val newSelection = TextRange(before.length, before.length + replacement.length)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
            return
        }

        // Case 3: No entities exist in the project -> insert the raw syntax @[entity_id] as a template
        if (entities.isEmpty()) {
            val inner = if (selected.isNotBlank()) selected else "entity_id"
            val placeholder = "@[$inner]"
            val newContent = "$before$placeholder$after"
            val selStart = before.length + 2
            val selEnd = selStart + inner.length
            val newSelection = TextRange(selStart, selEnd)
            textFieldValue = TextFieldValue(newContent, newSelection)
            onContentChange(newContent)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
            return
        }

        // Case 4: Entities exist -> insert '@' (or '@selected' if text highlighted) to trigger the autocomplete popup
        val query = selected.trim()
        val insertText = if (before.endsWith("@")) query else "@$query"
        val newContent = "$before$insertText$after"
        val newCursor = before.length + insertText.length
        val newSelection = TextRange(newCursor)
        textFieldValue = TextFieldValue(newContent, newSelection)
        onContentChange(newContent)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
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
                onClick = { insertOrTriggerEntityLink() },
                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.GoldPrimary),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 8.dp).defaultMinSize(minHeight = 30.dp)
            ) {
                Text("@ Entity", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { openImagePickerFromToolbar() },
                border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.GoldPrimary),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 8.dp).defaultMinSize(minHeight = 30.dp)
            ) {
                Text("🖼 Image", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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

            EditorFontSizeControl(
                fontSize = fontSize,
                onFontSizeChange = { newSize ->
                    coroutineScope.launch {
                        AppSettingsRepository.setEditorFontSize(newSize)
                    }
                }
            )
        }

        GrimoireDivider()

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
                fontSize = fontSize,
                onFontSizeChange = { newSize ->
                    coroutineScope.launch {
                        AppSettingsRepository.setEditorFontSize(newSize)
                    }
                },
                onOpenEntityPopout = onOpenEntityPopout,
                onOpenImageInTab = onOpenImageInTab,
                onOpenImagePicker = { openImagePickerFromToolbar() },
                projectPath = projectPath,
                focusRequester = focusRequester,
                onValueChange = { newValue ->
                    val updatedSelection = expandMarkdownTokenSelection(newValue.text, newValue.selection)
                    val finalValue = newValue.copy(selection = updatedSelection)
                    
                    textFieldValue = finalValue
                    if (finalValue.text != content) {
                        onContentChange(finalValue.text)
                    }

                    if (dismissedTypingTriggerPos != null && finalValue.selection.start != dismissedTypingTriggerPos) {
                        dismissedTypingTriggerPos = null
                    }

                    val cursor = finalValue.selection.start
                    if (finalValue.selection.collapsed && cursor >= 2) {
                        val textBefore = finalValue.text.substring(0, cursor)
                        if (textBefore.endsWith("![") && dismissedTypingTriggerPos != cursor) {
                            val isAlreadyCompleteLink = IMAGE_LINK_REGEX.findAll(finalValue.text).any { cursor in it.range }
                            if (!isAlreadyCompleteLink) {
                                isImageDialogTriggeredByTyping = true
                                typingTriggerRange = TextRange(cursor - 2, cursor)
                                imageDialogInitialTitle = ""
                                showImagePickerDialog = true
                            }
                        }
                    }
                },
                onFormat = { applyFormat(it) }
            )
        }

        if (showImagePickerDialog) {
            ImagePickerDialog(
                projectPath = projectPath,
                initialTitle = imageDialogInitialTitle,
                onDismiss = {
                    showImagePickerDialog = false
                    dismissedTypingTriggerPos = textFieldValue.selection.start
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                },
                onInsert = { title, relPath ->
                    val text = textFieldValue.text
                    val insertion = "![$title]($relPath)"
                    val (newContent, newCursor) = if (isImageDialogTriggeredByTyping && typingTriggerRange != null) {
                        val range = typingTriggerRange!!
                        val before = text.substring(0, range.min.coerceIn(0, text.length))
                        val after = text.substring(range.max.coerceIn(0, text.length))
                        "$before$insertion$after" to (before.length + insertion.length)
                    } else {
                        val sel = textFieldValue.selection
                        val before = text.substring(0, sel.min.coerceIn(0, text.length))
                        val after = text.substring(sel.max.coerceIn(0, text.length))
                        "$before$insertion$after" to (before.length + insertion.length)
                    }
                    showImagePickerDialog = false
                    isImageDialogTriggeredByTyping = false
                    typingTriggerRange = null
                    dismissedTypingTriggerPos = null
                    textFieldValue = TextFieldValue(newContent, TextRange(newCursor))
                    onContentChange(newContent)
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MarkdownEdit(
    textFieldValue: TextFieldValue,
    entities: List<Entity>,
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onFormat: (String) -> Unit,
    onOpenEntityPopout: (String) -> Unit,
    onOpenImageInTab: (String) -> Unit,
    onOpenImagePicker: () -> Unit = {},
    projectPath: String,
    focusRequester: FocusRequester
) {
    val primaryColor = GrimoireColors.GoldPrimary
    val fantasyFamily = LocalFantasyFontFamily.current

    val (annotatedText, currentOffsetMapping) = remember(
        primaryColor,
        textFieldValue.text,
        textFieldValue.selection,
        entities,
        fantasyFamily,
        fontSize
    ) {
        parseMarkdown(textFieldValue.text, primaryColor, textFieldValue.selection, entities, fantasyFamily, fontSize)
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

    val coroutineScope = rememberCoroutineScope()
    var lastMousePosition by remember { mutableStateOf<Offset?>(null) }
    var isCtrlDown by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val dispatcher = KeyEventDispatcher { event ->
            if (event.keyCode == KeyEvent.VK_CONTROL || event.keyCode == KeyEvent.VK_META) {
                isCtrlDown = (event.id == KeyEvent.KEY_PRESSED)
            }
            false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
        onDispose {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
        }
    }

    val hoveredEntity = remember(lastMousePosition, isCtrlDown, textLayoutResult, textFieldValue.text, currentOffsetMapping, entities) {
        if (!isCtrlDown || lastMousePosition == null) {
            null
        } else {
            findEntityAtPosition(lastMousePosition!!, textLayoutResult, currentOffsetMapping, textFieldValue.text, entities)
        }
    }
    val isHoveringEntityLink = hoveredEntity != null

    val currentImageHit = remember(lastMousePosition, textLayoutResult, textFieldValue.text, currentOffsetMapping, projectPath) {
        if (lastMousePosition == null) null
        else findImageLinkAtPosition(lastMousePosition!!, textLayoutResult, currentOffsetMapping, textFieldValue.text, projectPath)
    }
    val isHoveringImageLink = currentImageHit != null

    var hoveredImageLink by remember { mutableStateOf<ImageLinkHit?>(null) }
    var popupPosition by remember { mutableStateOf<Offset?>(null) }
    var isMouseOverPopup by remember { mutableStateOf(false) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(currentImageHit, isMouseOverPopup) {
        if (currentImageHit != null) {
            dismissJob?.cancel()
            val oldPath = hoveredImageLink?.relativePath
            hoveredImageLink = currentImageHit
            if (popupPosition == null || oldPath != currentImageHit.relativePath) {
                popupPosition = lastMousePosition
            }
        } else if (!isMouseOverPopup) {
            dismissJob?.cancel()
            dismissJob = launch {
                delay(250)
                if (!isMouseOverPopup) {
                    hoveredImageLink = null
                    popupPosition = null
                }
            }
        }
    }

    val shouldShowHandCursor = isHoveringEntityLink || (isHoveringImageLink && isCtrlDown)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .tiledTexture(Res.drawable.grimoire_vellum, alpha = 0.08f)
            .pointerHoverIcon(
                icon = if (shouldShowHandCursor) PointerIcon.Hand else PointerIcon.Default,
                overrideDescendants = shouldShowHandCursor
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !shouldShowHandCursor
            ) {
                focusRequester.requestFocus()
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                lastMousePosition = event.changes.firstOrNull()?.position
                isCtrlDown = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
            }
            .onPointerEvent(PointerEventType.Exit) {
                lastMousePosition = null
            }
            .onPointerEvent(PointerEventType.Scroll, pass = PointerEventPass.Initial) { event ->
                val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                if (isCtrl) {
                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (delta < 0f) {
                        onFontSizeChange(fontSize + 1)
                        event.changes.forEach { it.consume() }
                    } else if (delta > 0f) {
                        onFontSizeChange(fontSize - 1)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) { event ->
                val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                if (isCtrl && event.button == PointerButton.Primary) {
                    val clickPos = event.changes.firstOrNull()?.position
                    val layout = textLayoutResult
                    if (clickPos != null && layout != null) {
                        val clickedEntity = findEntityAtPosition(
                            pos = clickPos,
                            layout = layout,
                            offsetMapping = currentOffsetMapping,
                            text = textFieldValue.text,
                            entities = entities
                        )
                        if (clickedEntity != null) {
                            onOpenEntityPopout(clickedEntity.name)
                            event.changes.forEach { it.consume() }
                            return@onPointerEvent
                        }
                        val clickedImg = findImageLinkAtPosition(
                            pos = clickPos,
                            layout = layout,
                            offsetMapping = currentOffsetMapping,
                            text = textFieldValue.text,
                            projectPath = projectPath
                        )
                        if (clickedImg != null) {
                            onOpenImageInTab(clickedImg.relativePath)
                            event.changes.forEach { it.consume() }
                            return@onPointerEvent
                        }
                    }
                } else {
                    stickyX = null
                    lastVerticalSelection = null
                }
            }
            .onPointerEvent(PointerEventType.Release, pass = PointerEventPass.Initial) { event ->
                val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                if (isCtrl && event.button == PointerButton.Primary) {
                    val clickPos = event.changes.firstOrNull()?.position
                    val layout = textLayoutResult
                    if (clickPos != null && layout != null) {
                        val clickedEntity = findEntityAtPosition(
                            pos = clickPos,
                            layout = layout,
                            offsetMapping = currentOffsetMapping,
                            text = textFieldValue.text,
                            entities = entities
                        )
                        if (clickedEntity != null) {
                            event.changes.forEach { it.consume() }
                            return@onPointerEvent
                        }
                        val clickedImg = findImageLinkAtPosition(
                            pos = clickPos,
                            layout = layout,
                            offsetMapping = currentOffsetMapping,
                            text = textFieldValue.text,
                            projectPath = projectPath
                        )
                        if (clickedImg != null) {
                            onOpenImageInTab(clickedImg.relativePath)
                            event.changes.forEach { it.consume() }
                            return@onPointerEvent
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
                    ContextMenuItem("Underline") { onFormat("__") },
                    ContextMenuItem("Insert Image...") { onOpenImagePicker() }
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
                onValueChange = { newValue ->
                    if (shouldShowHandCursor && newValue.text == textFieldValue.text) {
                        return@BasicTextField
                    }
                    onValueChange(newValue)
                },
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        val isCtrl = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                        isCtrlDown = isCtrl

                        if (keyEvent.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        val key = keyEvent.key

                        if (isCtrl && !keyEvent.isAltPressed) {
                            when (key) {
                                Key.Plus, Key.Equals, Key.NumPadAdd -> {
                                    onFontSizeChange(fontSize + 1)
                                    return@onPreviewKeyEvent true
                                }
                                Key.Minus, Key.NumPadSubtract -> {
                                    onFontSizeChange(fontSize - 1)
                                    return@onPreviewKeyEvent true
                                }
                                Key.Zero, Key.NumPad0 -> {
                                    onFontSizeChange(AppSettingsRepository.DEFAULT_EDITOR_FONT_SIZE)
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }

                        val hasCommandModifier = isCtrl || keyEvent.isAltPressed

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
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5f).sp
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

        val activeHover = hoveredImageLink
        if (activeHover != null && popupPosition != null) {
            val density = LocalDensity.current
            val maxContainerWidthPx = with(density) { maxWidth.toPx() }
            val maxContainerHeightPx = with(density) { maxHeight.toPx() }
            val popupWidthPx = with(density) { 260.dp.toPx() }

            val anchorX = popupPosition!!.x
            val anchorY = popupPosition!!.y
            val popupX = if (anchorX + 16f + popupWidthPx <= maxContainerWidthPx) {
                anchorX + 16f
            } else {
                (anchorX - popupWidthPx - 16f).coerceAtLeast(0f)
            }
            val popupY = if (anchorY + 16f + 250f <= maxContainerHeightPx) {
                anchorY + 16f
            } else {
                (anchorY - 260f).coerceAtLeast(0f)
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(popupX.roundToInt(), popupY.roundToInt()) }
                    .width(260.dp)
                    .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(8.dp))
                    .onPointerEvent(PointerEventType.Enter) {
                        isMouseOverPopup = true
                        dismissJob?.cancel()
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        isMouseOverPopup = false
                    }
                    .clickable {
                        onOpenImageInTab(activeHover.relativePath)
                    },
                color = GrimoireColors.SurfaceCard,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val file = activeHover.resolvedFile
                    val fileExists = file != null && file.exists() && file.isFile

                    if (fileExists) {
                        val bitmap = rememberEntityImage(file)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = activeHover.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(0.5.dp, GrimoireColors.BorderBronze.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(GrimoireColors.Surface, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Laden...",
                                    color = GrimoireColors.ParchmentMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(GrimoireColors.Surface, RoundedCornerShape(6.dp))
                                .border(1.dp, GrimoireColors.CrimsonError.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Bilddatei nicht gefunden:\n${activeHover.relativePath}",
                                color = GrimoireColors.CrimsonError,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = activeHover.title,
                        color = GrimoireColors.GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = activeHover.relativePath,
                        color = GrimoireColors.ParchmentMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Klicken oder Strg+Klick zum Öffnen",
                        color = GrimoireColors.GoldPrimary.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

internal val ENTITY_MENTION_REGEX = Regex("(?<!\\\\)@\\[(.*?)\\]")

internal interface EntityHoverTextLayout {
    val textLength: Int
    fun getBoundingBox(offset: Int): Rect
}

internal fun TextLayoutResult.asEntityHoverTextLayout(): EntityHoverTextLayout = object : EntityHoverTextLayout {
    override val textLength: Int get() = this@asEntityHoverTextLayout.layoutInput.text.length
    override fun getBoundingBox(offset: Int): Rect = this@asEntityHoverTextLayout.getBoundingBox(offset)
}

internal fun findEntityAtPosition(
    pos: Offset,
    layout: EntityHoverTextLayout?,
    offsetMapping: OffsetMapping,
    text: String,
    entities: List<Entity>
): Entity? {
    if (layout == null || layout.textLength == 0) return null

    for (match in ENTITY_MENTION_REGEX.findAll(text)) {
        val entId = match.groups[1]?.value ?: continue
        val ent = entities.find { it.id == entId } ?: continue

        val transStart = offsetMapping.originalToTransformed(match.range.first)
            .coerceIn(0, layout.textLength)
        if (transStart >= layout.textLength) continue

        var transEnd = transStart
        while (transEnd < layout.textLength && offsetMapping.transformedToOriginal(transEnd) in match.range) {
            transEnd++
        }

        if (transEnd <= transStart) continue

        val firstCharBox = layout.getBoundingBox(transStart)
        val lastCharBox = layout.getBoundingBox(transEnd - 1)

        val isSingleLine = kotlin.math.abs(firstCharBox.top - lastCharBox.top) < 2f
        if (isSingleLine) {
            val linkRect = Rect(
                left = minOf(firstCharBox.left, lastCharBox.left) - 2f,
                top = minOf(firstCharBox.top, lastCharBox.top) - 2f,
                right = maxOf(firstCharBox.right, lastCharBox.right) + 2f,
                bottom = maxOf(firstCharBox.bottom, lastCharBox.bottom) + 2f
            )
            if (linkRect.contains(pos)) {
                return ent
            }
        } else {
            for (t in transStart until transEnd) {
                val charBox = layout.getBoundingBox(t).inflate(2f)
                if (charBox.contains(pos)) {
                    return ent
                }
            }
        }
    }

    return null
}

internal fun findEntityAtPosition(
    pos: Offset,
    layout: TextLayoutResult?,
    offsetMapping: OffsetMapping,
    text: String,
    entities: List<Entity>
): Entity? = findEntityAtPosition(pos, layout?.asEntityHoverTextLayout(), offsetMapping, text, entities)

internal val IMAGE_LINK_REGEX = Regex(
    "(?<!\\\\)(!?)\\[([^\\]\\n]+)\\]\\(([^)\\n]+\\.(?:png|jpg|jpeg|webp|svg|bmp|gif|ico))\\)",
    RegexOption.IGNORE_CASE
)

internal data class ImageLinkHit(
    val title: String,
    val relativePath: String,
    val resolvedFile: File?,
    val linkRect: Rect
)

internal fun resolveImageFile(relativePath: String, projectPath: String): File? {
    if (relativePath.isBlank()) return null
    val norm = relativePath.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar)
    val asFile = File(norm)
    if (asFile.isAbsolute && asFile.exists() && asFile.isFile) return asFile

    if (projectPath.isNotBlank()) {
        val inProject = File(projectPath, norm)
        if (inProject.exists() && inProject.isFile) return inProject
    }

    val inGlobalImages = File(ProjectRepository.globalImagesDir, norm)
    if (inGlobalImages.exists() && inGlobalImages.isFile) return inGlobalImages

    val inGlobal = File(ProjectRepository.globalDir, norm)
    if (inGlobal.exists() && inGlobal.isFile) return inGlobal

    return if (projectPath.isNotBlank()) File(projectPath, norm) else null
}

internal fun findImageLinkAtPosition(
    pos: Offset,
    layout: EntityHoverTextLayout?,
    offsetMapping: OffsetMapping,
    text: String,
    projectPath: String
): ImageLinkHit? {
    if (layout == null || layout.textLength == 0) return null

    for (match in IMAGE_LINK_REGEX.findAll(text)) {
        val title = match.groups[2]?.value ?: continue
        val relativePath = match.groups[3]?.value ?: continue

        val transStart = offsetMapping.originalToTransformed(match.range.first)
            .coerceIn(0, layout.textLength)
        if (transStart >= layout.textLength) continue

        var transEnd = transStart
        while (transEnd < layout.textLength && offsetMapping.transformedToOriginal(transEnd) in match.range) {
            transEnd++
        }

        if (transEnd <= transStart) continue

        val firstCharBox = layout.getBoundingBox(transStart)
        val lastCharBox = layout.getBoundingBox(transEnd - 1)

        val isSingleLine = kotlin.math.abs(firstCharBox.top - lastCharBox.top) < 2f
        val linkRect = if (isSingleLine) {
            Rect(
                left = minOf(firstCharBox.left, lastCharBox.left) - 2f,
                top = minOf(firstCharBox.top, lastCharBox.top) - 2f,
                right = maxOf(firstCharBox.right, lastCharBox.right) + 2f,
                bottom = maxOf(firstCharBox.bottom, lastCharBox.bottom) + 2f
            )
        } else {
            Rect(
                left = minOf(firstCharBox.left, lastCharBox.left) - 2f,
                top = minOf(firstCharBox.top, lastCharBox.top) - 2f,
                right = maxOf(firstCharBox.right, lastCharBox.right) + 2f,
                bottom = maxOf(firstCharBox.bottom, lastCharBox.bottom) + 2f
            )
        }

        val hit = if (isSingleLine) {
            linkRect.contains(pos)
        } else {
            (transStart until transEnd).any { layout.getBoundingBox(it).inflate(2f).contains(pos) }
        }

        if (hit) {
            val resolvedFile = resolveImageFile(relativePath, projectPath)
            return ImageLinkHit(title, relativePath, resolvedFile, linkRect)
        }
    }

    return null
}

internal fun findImageLinkAtPosition(
    pos: Offset,
    layout: TextLayoutResult?,
    offsetMapping: OffsetMapping,
    text: String,
    projectPath: String
): ImageLinkHit? = findImageLinkAtPosition(pos, layout?.asEntityHoverTextLayout(), offsetMapping, text, projectPath)

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
    fantasyFamily: FontFamily = FontFamily.Serif,
    baseFontSize: Int = 16
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
        "\\*\\*(.*?)\\*\\*|(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|__(.*?)__|(?<!\\\\)@\\[(.*?)\\]|^(#{1,6}) (.*)$|(?<!\\\\)(!?)\\[([^\\]\\n]+)\\]\\(([^)\\n]+\\.(?:png|jpg|jpeg|webp|svg|bmp|gif|ico))\\)",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
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
            val scaleFactor = baseFontSize / 16f
            val headingFontSize = when (hashes.length) {
                1 -> (26f * scaleFactor).roundToInt().sp
                2 -> (22f * scaleFactor).roundToInt().sp
                else -> (18f * scaleFactor).roundToInt().sp
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
                        fontSize = headingFontSize,
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
                        fontSize = headingFontSize,
                        fontFamily = fantasyFamily
                    ),
                    transStart,
                    builder.length
                )
            }
        } else if (match.groups[8] != null && match.groups[9] != null) {
            val title = match.groups[8]!!.value
            val path = match.groups[9]!!.value
            val isCursorIn = isEntityExpanded(match.range, cursorSelection)
            val displayText = "🖼 " + title

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
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    transStart,
                    builder.length
                )
                builder.addStringAnnotation("image_link", path, transStart, builder.length)
                builder.addStringAnnotation("image_title", title, transStart, builder.length)
            } else {
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
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    transStart,
                    builder.length
                )
                builder.addStringAnnotation("image_link", path, transStart, builder.length)
                builder.addStringAnnotation("image_title", title, transStart, builder.length)
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
