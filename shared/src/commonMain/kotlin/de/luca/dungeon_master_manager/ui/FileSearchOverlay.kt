package de.luca.dungeon_master_manager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.*
import de.luca.dungeon_master_manager.ui.components.EntityImageLoader
import de.luca.dungeon_master_manager.ui.components.GrimoireKeyBadge
import de.luca.dungeon_master_manager.ui.theme.GrimoireCard
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import kotlinx.coroutines.delay
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Composable
fun FileSearchOverlay(
    searchService: UniversalSearchService,
    openTabPaths: List<String>,
    onClose: () -> Unit,
    onOpenFile: (FileNode) -> Unit,
    onOpenHeading: (filePath: String, headingIndex: Int) -> Unit,
    onOpenEntity: (entityName: String) -> Unit,
    onCreateNewNote: (noteName: String) -> Unit,
    onPopOutFile: (FileNode) -> Unit,
    onExecuteCommand: (commandId: String) -> Unit,
    onInsertLinkToActiveEditor: ((title: String, relativePath: String, isImage: Boolean) -> Unit)? = null,
    onOpenMapMarker: (mapFilePath: String, markerId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf<SearchScope?>(null) }
    var results by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    // Dialog scale/drop entrance animation (snappy 150ms)
    val dialogAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        dialogAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
        )
    }

    // Trigger search with debounce
    LaunchedEffect(query, selectedScope, openTabPaths) {
        isSearching = true
        delay(40)
        results = searchService.search(
            rawQuery = query,
            openTabPaths = openTabPaths,
            scopeFilterOverride = selectedScope
        )
        selectedIndex = 0
        isSearching = false
    }

    // Scroll into view when selection moves
    LaunchedEffect(selectedIndex) {
        if (results.isNotEmpty() && selectedIndex in results.indices) {
            lazyListState.animateScrollToItem(selectedIndex)
        }
    }

    // Auto-focus input
    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
    }

    val selectedItem = results.getOrNull(selectedIndex)

    // Action handlers
    fun handlePrimaryAction(item: SearchResultItem) {
        when (item.type) {
            SearchItemType.FILE -> {
                item.filePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        onOpenFile(FileNode(name = file.name, path = file.absolutePath, isDirectory = false))
                        onClose()
                    }
                }
            }
            SearchItemType.HEADING -> {
                if (item.filePath != null && item.headingIndex != null) {
                    onOpenHeading(item.filePath, item.headingIndex)
                    onClose()
                }
            }
            SearchItemType.ENTITY -> {
                onOpenEntity(item.title)
                onClose()
            }
            SearchItemType.CREATE_NOTE -> {
                val name = item.filePath?.let { File(it).name } ?: query
                onCreateNewNote(name)
                onClose()
            }
            SearchItemType.COMMAND -> {
                item.commandActionId?.let { cmd ->
                    onExecuteCommand(cmd)
                    onClose()
                }
            }
            SearchItemType.MAP_MARKER -> {
                if (item.filePath != null && item.markerId != null) {
                    onOpenMapMarker(item.filePath, item.markerId)
                    onClose()
                }
            }
        }
    }

    fun handleOpenInOsExplorer(item: SearchResultItem) {
        item.filePath?.let { path ->
            val file = File(path)
            try {
                if (Desktop.isDesktopSupported()) {
                    val parent = file.parentFile ?: file
                    Desktop.getDesktop().open(parent)
                    onClose()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handleOpenInOsDefaultApp(item: SearchResultItem) {
        item.filePath?.let { path ->
            val file = File(path)
            try {
                if (Desktop.isDesktopSupported() && file.exists()) {
                    Desktop.getDesktop().open(file)
                    onClose()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handlePopOut(item: SearchResultItem) {
        item.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                onPopOutFile(FileNode(name = file.name, path = file.absolutePath, isDirectory = false))
                onClose()
            }
        }
    }

    fun handleCopyPath(item: SearchResultItem) {
        val path = item.relativePath ?: item.filePath ?: return
        try {
            val selection = StringSelection(path)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            onClose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleInsertLink(item: SearchResultItem) {
        val relPath = item.relativePath ?: item.filePath ?: return
        val title = item.title.substringBeforeLast('.')
        val isImg = item.isImage
        val linkText = if (item.type == SearchItemType.HEADING) {
            "[${item.title}]($relPath#${item.title})"
        } else if (isImg) {
            "![$title]($relPath)"
        } else {
            "[$title]($relPath)"
        }

        try {
            val selection = StringSelection(linkText)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            onInsertLinkToActiveEditor?.invoke(title, relPath, isImg)
            onClose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Modal Scrim & Container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GrimoireColors.Background.copy(alpha = 0.72f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                // Navigation
                when (event.key) {
                    Key.Escape -> {
                        onClose()
                        return@onPreviewKeyEvent true
                    }
                    Key.DirectionDown -> {
                        if (results.isNotEmpty()) {
                            selectedIndex = (selectedIndex + 1).coerceAtMost(results.lastIndex)
                        }
                        return@onPreviewKeyEvent true
                    }
                    Key.DirectionUp -> {
                        if (results.isNotEmpty()) {
                            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        }
                        return@onPreviewKeyEvent true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        if (event.isShiftPressed) {
                            // Instant note creation
                            val createItem = results.find { it.type == SearchItemType.CREATE_NOTE }
                            if (createItem != null) {
                                handlePrimaryAction(createItem)
                                return@onPreviewKeyEvent true
                            }
                        }
                        selectedItem?.let {
                            handlePrimaryAction(it)
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                // Alt Shortcuts for Actions
                if (event.isAltPressed && selectedItem != null) {
                    when (event.key) {
                        Key.E -> {
                            handleOpenInOsExplorer(selectedItem)
                            return@onPreviewKeyEvent true
                        }
                        Key.O -> {
                            handleOpenInOsDefaultApp(selectedItem)
                            return@onPreviewKeyEvent true
                        }
                        Key.P -> {
                            handlePopOut(selectedItem)
                            return@onPreviewKeyEvent true
                        }
                        Key.C -> {
                            handleCopyPath(selectedItem)
                            return@onPreviewKeyEvent true
                        }
                        Key.L -> {
                            handleInsertLink(selectedItem)
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                false
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Floating Dialog
        val cardScale = 0.96f + 0.04f * dialogAnim.value
        val cardOffsetY = -18.dp * (1f - dialogAnim.value)
        val cardAlpha = dialogAnim.value

        GrimoireCard(
            modifier = Modifier
                .padding(top = 70.dp, bottom = 40.dp)
                .widthIn(min = 680.dp, max = 860.dp)
                .fillMaxWidth(0.72f)
                .heightIn(min = 440.dp, max = 560.dp)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    translationY = cardOffsetY.toPx()
                    alpha = cardAlpha
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* intercept clicks inside dialog */ },
            shape = RoundedCornerShape(10.dp),
            borderColor = GrimoireColors.BorderBronze,
            containerColor = GrimoireColors.SurfaceCard
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP BAR: Search Input & Scope Pills
                var isInputFocused by remember { mutableStateOf(false) }
                val searchIconScale by animateFloatAsState(
                    targetValue = if (isSearching) 1.18f else if (isInputFocused) 1.08f else 1.0f,
                    animationSpec = tween(130, easing = FastOutSlowInEasing),
                    label = "SearchIconScale"
                )
                val searchIconRotation by animateFloatAsState(
                    targetValue = if (isSearching) 12f else 0f,
                    animationSpec = tween(130, easing = FastOutSlowInEasing),
                    label = "SearchIconRotation"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .graphicsLayer {
                                scaleX = searchIconScale
                                scaleY = searchIconScale
                                rotationZ = searchIconRotation
                            }
                    )

                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isInputFocused = it.isFocused },
                        textStyle = TextStyle(
                            color = GrimoireColors.ParchmentLight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(GrimoireColors.GoldPrimary),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search files, entities, markers... (e.g. 'e:' entities, 'pin:' markers, 'm:' maps, '@'/'#' headings, '>' commands)",
                                        style = TextStyle(
                                            color = GrimoireColors.ParchmentDim,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(100)) + scaleIn(initialScale = 0.7f, animationSpec = tween(100)),
                        exit = fadeOut(animationSpec = tween(80)) + scaleOut(targetScale = 0.7f, animationSpec = tween(80))
                    ) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(24.dp).padding(end = 6.dp)
                        ) {
                            Text("✕", fontSize = 12.sp, color = GrimoireColors.ParchmentMuted)
                        }
                    }

                    // Scope Pills: All | Project | Global
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ScopePill(
                            label = "All",
                            isSelected = selectedScope == null,
                            onClick = { selectedScope = null }
                        )
                        ScopePill(
                            label = "Project",
                            isSelected = selectedScope == SearchScope.PROJECT,
                            onClick = { selectedScope = SearchScope.PROJECT }
                        )
                        ScopePill(
                            label = "Global",
                            isSelected = selectedScope == SearchScope.GLOBAL,
                            onClick = { selectedScope = SearchScope.GLOBAL }
                        )
                    }
                }

                SearchLoadingBar(isSearching = isSearching)

                // 2. BODY: Result List + Preview Pane (Split View)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left Column: Result List
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        if (results.isEmpty() && !isSearching) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Keine Treffer gefunden",
                                    color = GrimoireColors.ParchmentDim,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                itemsIndexed(results, key = { _, it -> it.id }) { index, item ->
                                    SearchResultRow(
                                        item = item,
                                        isSelected = index == selectedIndex,
                                        onClick = {
                                            selectedIndex = index
                                        },
                                        onDoubleClick = {
                                            selectedIndex = index
                                            handlePrimaryAction(item)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(GrimoireColors.BorderBronzeSubtle)
                    )

                    // Right Column: Preview Pane
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .background(GrimoireColors.SurfaceLow.copy(alpha = 0.5f))
                    ) {
                        AnimatedContent(
                            targetState = Pair(selectedIndex, selectedItem),
                            transitionSpec = {
                                val isMovingDown = targetState.first >= initialState.first
                                val slideInOffset = if (isMovingDown) { height: Int -> height / 24 } else { height: Int -> -height / 24 }
                                val slideOutOffset = if (isMovingDown) { height: Int -> -height / 28 } else { height: Int -> height / 28 }

                                (fadeIn(animationSpec = tween(130, easing = FastOutSlowInEasing)) +
                                 slideInVertically(animationSpec = tween(130, easing = FastOutSlowInEasing), initialOffsetY = slideInOffset))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(70, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(animationSpec = tween(70, easing = FastOutSlowInEasing), targetOffsetY = slideOutOffset)
                                    )
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "PreviewTransition"
                        ) { (_, currentItem) ->
                            SearchPreviewPane(
                                item = currentItem,
                                modifier = Modifier.fillMaxSize().padding(14.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = GrimoireColors.BorderBronzeSubtle)

                // 3. ACTION FOOTER (Bottom bar with shortcuts)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrimoireColors.SurfaceLow)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Enter: Primary Action
                        val primaryLabel = when (selectedItem?.type) {
                            SearchItemType.CREATE_NOTE -> "Create note"
                            SearchItemType.ENTITY -> "Open entity"
                            SearchItemType.COMMAND -> "Execute"
                            SearchItemType.MAP_MARKER -> "Zoom to marker"
                            SearchItemType.HEADING -> "Jump to heading"
                            else -> "Open in tab"
                        }
                        ActionBadgeButton(
                            key = "Enter",
                            label = primaryLabel,
                            onClick = { selectedItem?.let { handlePrimaryAction(it) } }
                        )

                        // File & marker & entity specific actions
                        val hasValidFilePath = !selectedItem?.filePath.isNullOrBlank()

                        if (selectedItem?.type == SearchItemType.ENTITY) {
                            ActionBadgeButton(
                                key = "Alt+P",
                                label = "Popout",
                                onClick = { handlePopOut(selectedItem) }
                            )

                            if (hasValidFilePath) {
                                ActionBadgeButton(
                                    key = "Alt+E",
                                    label = "File manager",
                                    onClick = { handleOpenInOsExplorer(selectedItem) }
                                )
                                ActionBadgeButton(
                                    key = "Alt+C",
                                    label = "Copy path",
                                    onClick = { handleCopyPath(selectedItem) }
                                )
                                if (onInsertLinkToActiveEditor != null) {
                                    ActionBadgeButton(
                                        key = "Alt+L",
                                        label = "Insert link",
                                        onClick = { handleInsertLink(selectedItem) }
                                    )
                                }
                            }
                        } else if (hasValidFilePath && selectedItem.type != SearchItemType.COMMAND) {
                            ActionBadgeButton(
                                key = "Alt+E",
                                label = "File manager",
                                onClick = { handleOpenInOsExplorer(selectedItem) }
                            )

                            if (selectedItem.type != SearchItemType.MAP_MARKER && !FileExtensions.isMap(selectedItem.extension)) {
                                ActionBadgeButton(
                                    key = "Alt+O",
                                    label = "Default app",
                                    onClick = { handleOpenInOsDefaultApp(selectedItem) }
                                )
                            }

                            ActionBadgeButton(
                                key = "Alt+P",
                                label = "Popout",
                                onClick = { handlePopOut(selectedItem) }
                            )

                            ActionBadgeButton(
                                key = "Alt+C",
                                label = "Copy path",
                                onClick = { handleCopyPath(selectedItem) }
                            )

                            if (onInsertLinkToActiveEditor != null) {
                                ActionBadgeButton(
                                    key = "Alt+L",
                                    label = "Insert link",
                                    onClick = { handleInsertLink(selectedItem) }
                                )
                            }
                        }
                    }

                    // Esc button / label on the right side
                    val escInteractionSource = remember { MutableInteractionSource() }
                    val isEscHovered by escInteractionSource.collectIsHoveredAsState()
                    val escBg by animateColorAsState(
                        targetValue = if (isEscHovered) GrimoireColors.SurfaceHover else Color.Transparent,
                        animationSpec = tween(100),
                        label = "EscBg"
                    )

                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(escBg)
                            .hoverable(escInteractionSource)
                            .clickable(interactionSource = escInteractionSource, indication = null) { onClose() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GrimoireKeyBadge(keyLabel = "Esc", fontSize = 10)
                        Text(
                            text = "Close",
                            fontSize = 11.sp,
                            color = if (isEscHovered) GrimoireColors.ParchmentLight else GrimoireColors.ParchmentDim,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val animBg by animateColorAsState(
        targetValue = if (isSelected) GrimoireColors.GoldContainer.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "RowBg"
    )
    val animBorder by animateColorAsState(
        targetValue = if (isSelected) GrimoireColors.GoldPrimary else Color.Transparent,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "RowBorder"
    )
    val paddingStart by animateDpAsState(
        targetValue = if (isSelected) 10.dp else 8.dp,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "RowPadding"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(animBg)
            .border(1.dp, animBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(start = paddingStart, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon / Thumbnail
        SearchItemLeadingIcon(item = item, modifier = Modifier.size(24.dp))

        Spacer(Modifier.width(8.dp))

        // Title and Subtitle with Highlighting
        Column(modifier = Modifier.weight(1f)) {
            HighlightedText(
                text = item.title,
                highlightIndices = item.matchedIndices,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.ParchmentDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Trailing Type / Scope Badge
        if (item.type == SearchItemType.FILE && item.extension.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .background(GrimoireColors.SurfaceLow, RoundedCornerShape(4.dp))
                    .border(1.dp, GrimoireColors.BorderBronzeSubtle, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ".${item.extension}",
                    fontSize = 10.sp,
                    color = GrimoireColors.GoldMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (item.type == SearchItemType.HEADING) {
            val level = (item.headingLevel ?: 1).coerceIn(1, 3)
            Box(
                modifier = Modifier
                    .background(GrimoireColors.SurfaceLow, RoundedCornerShape(4.dp))
                    .border(1.dp, GrimoireColors.BorderBronzeSubtle, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "H$level",
                    fontSize = 10.sp,
                    color = GrimoireColors.GoldMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (item.type == SearchItemType.MAP_MARKER) {
            Box(
                modifier = Modifier
                    .background(GrimoireColors.SurfaceLow, RoundedCornerShape(4.dp))
                    .border(1.dp, GrimoireColors.BorderBronzeSubtle, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Marker",
                    fontSize = 10.sp,
                    color = GrimoireColors.GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (item.type == SearchItemType.ENTITY) {
            val entityColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
            Box(
                modifier = Modifier
                    .background(GrimoireColors.SurfaceLow, RoundedCornerShape(4.dp))
                    .border(1.dp, entityColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.entityType ?: "Entity",
                    fontSize = 10.sp,
                    color = entityColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (item.type == SearchItemType.CREATE_NOTE) {
            GrimoireKeyBadge(keyLabel = "Shift+Enter", fontSize = 10)
        }
    }
}

private fun parseEntityColor(raw: String?): Color {
    if (raw.isNullOrBlank()) return GrimoireColors.GoldPrimary
    return try {
        Color(raw.toULong())
    } catch (_: Exception) {
        GrimoireColors.GoldPrimary
    }
}

@Composable
private fun SearchItemLeadingIcon(
    item: SearchResultItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (item.isImage && item.thumbnailFile != null) {
            var bitmap by remember(item.thumbnailFile) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(item.thumbnailFile) {
                bitmap = EntityImageLoader.loadImage(item.thumbnailFile, maxDimension = 48)
            }
            if (bitmap != null) {
                if (item.type == SearchItemType.ENTITY) {
                    val entityColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, entityColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(3.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Text("🖼️", fontSize = 14.sp)
            }
        } else if (item.type == SearchItemType.ENTITY) {
            val entityColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
            val letter = remember(item.title) {
                item.title.trim().take(1).uppercase().ifBlank { "?" }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(GrimoireColors.SurfaceLow)
                    .border(1.5.dp, entityColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(entityColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = entityColor
                    )
                }
            }
        } else if (item.type == SearchItemType.MAP_MARKER && item.entityColor != null) {
            val markerColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
            val letter = remember(item.title) {
                item.title.trim().take(1).uppercase().ifBlank { "?" }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(GrimoireColors.SurfaceLow)
                    .border(1.5.dp, markerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(markerColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = markerColor
                    )
                }
            }
        } else if (item.type == SearchItemType.HEADING) {
            val level = (item.headingLevel ?: 1).coerceIn(1, 3)
            val hashText = "#".repeat(level)
            val (fontSize, letterSpacing) = when (level) {
                3 -> 10.sp to (-0.5).sp
                2 -> 12.sp to (-0.3).sp
                else -> 14.sp to 0.sp
            }
            Text(
                text = hashText,
                fontSize = fontSize,
                letterSpacing = letterSpacing,
                color = GrimoireColors.GoldPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        } else {
            val icon = when (item.type) {
                SearchItemType.HEADING -> "#"
                SearchItemType.COMMAND -> "⚡"
                SearchItemType.CREATE_NOTE -> "➕"
                SearchItemType.MAP_MARKER -> "📍"
                SearchItemType.ENTITY -> "👤"
                SearchItemType.FILE -> {
                    when {
                        FileExtensions.isMarkdown(item.extension) -> "📝"
                        FileExtensions.isMap(item.extension) -> "🗺️"
                        else -> "📄"
                    }
                }
            }
            Text(
                text = icon,
                fontSize = 13.sp,
                color = GrimoireColors.ParchmentLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SearchPreviewPane(
    item: SearchResultItem?,
    modifier: Modifier = Modifier
) {
    if (item == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No preview", fontSize = 12.sp, color = GrimoireColors.ParchmentDim)
        }
        return
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        if (item.type == SearchItemType.ENTITY) {
            val entityColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = entityColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!item.entityType.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(entityColor.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                            .border(1.dp, entityColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.entityType.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = entityColor
                        )
                    }
                }
            }
        } else if (item.type == SearchItemType.HEADING) {
            val level = (item.headingLevel ?: 1).coerceIn(1, 3)
            val hashes = "#".repeat(level)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GrimoireColors.GoldBright,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .background(GrimoireColors.SurfaceLow, RoundedCornerShape(4.dp))
                        .border(1.dp, GrimoireColors.GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$hashes H$level",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrimoireColors.GoldPrimary
                    )
                }
            }
        } else {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = GrimoireColors.GoldBright,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = GrimoireColors.ParchmentDim,
            fontSize = 11.sp
        )

        GrimoireDivider(showDiamond = false, color = GrimoireColors.BorderBronzeSubtle)

        // Image or Entity Avatar Preview
        if (item.isImage && item.thumbnailFile != null) {
            var largeBitmap by remember(item.thumbnailFile) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(item.thumbnailFile) {
                largeBitmap = EntityImageLoader.loadImage(item.thumbnailFile, maxDimension = 240)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .background(GrimoireColors.SurfaceCard, RoundedCornerShape(6.dp))
                    .border(1.dp, GrimoireColors.BorderBronzeSubtle, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (largeBitmap != null) {
                    if (item.type == SearchItemType.ENTITY) {
                        val entityColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
                        Image(
                            bitmap = largeBitmap!!,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, entityColor, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            bitmap = largeBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GrimoireColors.GoldPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else if (item.type == SearchItemType.ENTITY) {
            val entityColor = remember(item.entityColor) { parseEntityColor(item.entityColor) }
            val letter = remember(item.title) {
                item.title.trim().take(1).uppercase().ifBlank { "?" }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 130.dp)
                    .background(GrimoireColors.SurfaceCard, RoundedCornerShape(6.dp))
                    .border(1.dp, GrimoireColors.BorderBronzeSubtle, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(GrimoireColors.SurfaceLow)
                        .border(2.dp, entityColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(entityColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = entityColor
                        )
                    }
                }
            }
        }

        // Markdown / Text Preview Snippet
        if (item.previewSnippet != null) {
            Text(
                text = if (item.type == SearchItemType.HEADING) "Section preview:" else "Preview:",
                style = MaterialTheme.typography.labelSmall,
                color = GrimoireColors.ParchmentMuted,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GrimoireColors.SurfaceCard, RoundedCornerShape(6.dp))
                    .border(1.dp, GrimoireColors.BorderBronzeSubtle, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = item.previewSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentLight,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Entity Information
        if (item.type == SearchItemType.ENTITY) {
            Text(
                text = "Entity type: ${item.entityType ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.ParchmentLight,
                fontSize = 12.sp
            )
            if (!item.filePath.isNullOrBlank()) {
                Text(
                    text = "Linked note: ${item.filePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentDim,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "No notes file linked to this entity",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrimoireColors.ParchmentDim,
                    fontSize = 11.sp
                )
            }
        }

        // Heading Information
        if (item.type == SearchItemType.HEADING) {
            val level = (item.headingLevel ?: 1).coerceIn(1, 3)
            val hashes = "#".repeat(level)
            val fileName = item.filePath?.let { File(it).name } ?: "note"
            Text(
                text = "Heading level $level ($hashes) in $fileName",
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.ParchmentLight,
                fontSize = 12.sp
            )
            Text(
                text = "Press Enter to open file at this heading.",
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.ParchmentDim,
                fontSize = 11.sp
            )
        }

        // Map Information
        if (FileExtensions.isMap(item.extension) && item.type != SearchItemType.MAP_MARKER) {
            Text(
                text = "Interactive map with pins and markers",
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.ParchmentLight,
                fontSize = 12.sp
            )
        }

        // Map Marker Information
        if (item.type == SearchItemType.MAP_MARKER) {
            Text(
                text = "📍 Map Marker",
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(
                text = "Press Enter to open map and zoom to this marker.",
                style = MaterialTheme.typography.bodySmall,
                color = GrimoireColors.ParchmentDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    highlightIndices: List<Int>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    if (highlightIndices.isEmpty()) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = GrimoireColors.ParchmentLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    val highlightSet = highlightIndices.toSet()
    val annotated = buildAnnotatedString {
        for (i in text.indices) {
            if (i in highlightSet) {
                withStyle(
                    SpanStyle(
                        color = GrimoireColors.GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text[i])
                }
            } else {
                withStyle(SpanStyle(color = GrimoireColors.ParchmentLight)) {
                    append(text[i])
                }
            }
        }
    }

    Text(
        text = annotated,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SearchLoadingBar(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(GrimoireColors.BorderBronzeSubtle)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "SearchLoadingTransition")
        val progress by infiniteTransition.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SearchLoadingProgress"
        )

        AnimatedVisibility(
            visible = isSearching,
            enter = fadeIn(animationSpec = tween(80)),
            exit = fadeOut(animationSpec = tween(120))
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val barWidth = size.width * 0.35f
                        val startX = (size.width + barWidth) * progress - barWidth
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GrimoireColors.GoldPrimary.copy(alpha = 0.4f),
                                    GrimoireColors.GoldBright,
                                    GrimoireColors.GoldPrimary.copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                startX = startX,
                                endX = startX + barWidth
                            )
                        )
                    }
            )
        }
    }
}

@Composable
private fun ScopePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetBg = when {
        isSelected -> GrimoireColors.GoldContainer
        isHovered -> GrimoireColors.SurfaceHover
        else -> GrimoireColors.SurfaceLow
    }
    val targetBorder = when {
        isSelected -> GrimoireColors.GoldPrimary
        isHovered -> GrimoireColors.BorderBronze
        else -> GrimoireColors.BorderBronzeSubtle
    }
    val targetText = when {
        isSelected -> GrimoireColors.GoldBright
        isHovered -> GrimoireColors.ParchmentLight
        else -> GrimoireColors.ParchmentMuted
    }

    val animatedBg by animateColorAsState(targetBg, animationSpec = tween(120), label = "PillBg")
    val animatedBorder by animateColorAsState(targetBorder, animationSpec = tween(120), label = "PillBorder")
    val animatedText by animateColorAsState(targetText, animationSpec = tween(120), label = "PillText")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(animatedBg)
            .border(
                1.dp,
                animatedBorder,
                RoundedCornerShape(4.dp)
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = animatedText
        )
    }
}

@Composable
private fun ActionBadgeButton(
    key: String,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        targetValue = if (isHovered) GrimoireColors.SurfaceHover else Color.Transparent,
        animationSpec = tween(100),
        label = "ActionBadgeBg"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GrimoireKeyBadge(keyLabel = key, fontSize = 10)
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isHovered) GrimoireColors.GoldBright else GrimoireColors.ParchmentLight
        )
    }
}
