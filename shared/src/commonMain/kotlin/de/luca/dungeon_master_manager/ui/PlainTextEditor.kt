package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.ui.components.EditorFontSizeControl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plain text editor featuring monospace typography, word wrapping,
 * clipboard copy, adjustable font size, and debounced auto-save support.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlainTextEditor(
    content: String,
    onContentChange: (String) -> Unit,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    val settings by AppSettingsRepository.settings.collectAsState()
    val fontSize = settings.editorFontSize

    var textFieldValue by remember { mutableStateOf(TextFieldValue(content)) }
    var justCopied by remember { mutableStateOf(false) }

    val verticalScrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val editorInteractionSource = remember { MutableInteractionSource() }

    // Sync external changes (e.g. initial load or file reloaded)
    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            val newSelection = if (content.startsWith(textFieldValue.text)) {
                TextRange(content.length)
            } else {
                TextRange(
                    textFieldValue.selection.start.coerceAtMost(content.length),
                    textFieldValue.selection.end.coerceAtMost(content.length)
                )
            }
            textFieldValue = TextFieldValue(text = content, selection = newSelection)
        }
    }

    val lineCount = remember(textFieldValue.text) {
        if (textFieldValue.text.isEmpty()) 1 else textFieldValue.text.count { it == '\n' } + 1
    }
    val charCount = textFieldValue.text.length

    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.5f).sp,
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📄 $fileName",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.width(16.dp))

                Text(
                    "$lineCount ${if (lineCount == 1) "line" else "lines"}, $charCount chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.weight(1f))

                // Font size controls
                EditorFontSizeControl(
                    fontSize = fontSize,
                    onFontSizeChange = { newSize ->
                        coroutineScope.launch {
                            AppSettingsRepository.setEditorFontSize(newSize)
                        }
                    }
                )

                Spacer(Modifier.width(12.dp))

                // Copy All button
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(textFieldValue.text))
                        justCopied = true
                        coroutineScope.launch {
                            delay(2000)
                            justCopied = false
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(if (justCopied) "Copied!" else "Copy All", fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Editor layout: Full-width wrapped text field with vertical scroll
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val minHeight = maxHeight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .defaultMinSize(minHeight = minHeight)
                    .clickable(
                        interactionSource = editorInteractionSource,
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                        val targetLen = textFieldValue.text.length
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(targetLen)
                        )
                    }
                    .onPointerEvent(PointerEventType.Scroll, pass = PointerEventPass.Initial) { event ->
                        val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                        if (isCtrl) {
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (delta < 0f) {
                                coroutineScope.launch {
                                    AppSettingsRepository.setEditorFontSize(fontSize + 1)
                                }
                                event.changes.forEach { it.consume() }
                            } else if (delta > 0f) {
                                coroutineScope.launch {
                                    AppSettingsRepository.setEditorFontSize(fontSize - 1)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        if (newValue.text != content) {
                            onContentChange(newValue.text)
                        }
                    },
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            val isCtrl = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                            if (isCtrl && !keyEvent.isAltPressed) {
                                when (keyEvent.key) {
                                    Key.Plus, Key.Equals, Key.NumPadAdd -> {
                                        coroutineScope.launch {
                                            AppSettingsRepository.setEditorFontSize(fontSize + 1)
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.Minus, Key.NumPadSubtract -> {
                                        coroutineScope.launch {
                                            AppSettingsRepository.setEditorFontSize(fontSize - 1)
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.Zero, Key.NumPad0 -> {
                                        coroutineScope.launch {
                                            AppSettingsRepository.setEditorFontSize(AppSettingsRepository.DEFAULT_EDITOR_FONT_SIZE)
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                }
                            }
                            false
                        }
                )
            }
        }
    }
}
