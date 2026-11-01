package de.luca.dungeon_master_manager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Toc
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider

data class MarkdownHeading(
    val level: Int,
    val title: String,
    val rawContent: String,
    val charOffset: Int,
    val lineIndex: Int
)

fun cleanHeadingTitle(raw: String): String {
    return raw
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("__(.*?)__"), "$1")
        .replace(Regex("@\\[(.*?)\\]"), "$1")
        .replace(Regex("!\\[(.*?)\\]\\(.*?\\)"), "$1")
        .trim()
}

fun extractMarkdownHeadings(text: String): List<MarkdownHeading> {
    if (text.isEmpty()) return emptyList()
    val headings = mutableListOf<MarkdownHeading>()
    val regex = Regex("^(#{1,6}) (.*)$", RegexOption.MULTILINE)

    for (match in regex.findAll(text)) {
        val hashes = match.groups[1]!!.value
        val rawContent = match.groups[2]!!.value
        val title = cleanHeadingTitle(rawContent)
        if (title.isNotEmpty()) {
            val offset = match.range.first
            val lineIndex = text.substring(0, offset).count { it == '\n' }
            headings.add(
                MarkdownHeading(
                    level = hashes.length,
                    title = title,
                    rawContent = rawContent,
                    charOffset = offset,
                    lineIndex = lineIndex
                )
            )
        }
    }
    return headings
}

@Composable
fun MarkdownTimelineSidebar(
    headings: List<MarkdownHeading>,
    activeHeadingIndex: Int,
    onHeadingClick: (MarkdownHeading) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight(),
        color = GrimoireColors.SurfaceLow
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Toc,
                        contentDescription = "Timeline",
                        tint = GrimoireColors.GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "TIMELINE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrimoireColors.GoldPrimary,
                        letterSpacing = 1.sp
                    )
                    if (headings.isNotEmpty()) {
                        Text(
                            "(${headings.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrimoireColors.ParchmentMuted
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Hide Timeline",
                        tint = GrimoireColors.ParchmentMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            GrimoireDivider()

            if (headings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Toc,
                            contentDescription = null,
                            tint = GrimoireColors.BorderBronze,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "No headings found",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = GrimoireColors.ParchmentDim,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Use '# Heading' in your notes to build an outline timeline.",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrimoireColors.ParchmentDim.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                ) {
                    itemsIndexed(headings, key = { index, heading -> "${heading.lineIndex}_${heading.charOffset}_$index" }) { index, heading ->
                        val isActive = index == activeHeadingIndex
                        val isPast = index < activeHeadingIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val density = LocalDensity.current.density

                        val indentDp = ((heading.level - 1).coerceIn(0, 3) * 8).dp

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isActive -> GrimoireColors.GoldPrimary.copy(alpha = 0.16f)
                                        isHovered -> GrimoireColors.SurfaceHover
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isActive) Modifier.border(
                                        0.5.dp,
                                        GrimoireColors.GoldPrimary.copy(alpha = 0.5f),
                                        RoundedCornerShape(6.dp)
                                    ) else Modifier
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    onHeadingClick(heading)
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (indentDp > 0.dp) {
                                Spacer(modifier = Modifier.width(indentDp))
                            }

                            // Timeline Canvas: vertical connecting track + node marker
                            Canvas(modifier = Modifier.width(20.dp).height(32.dp)) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2

                                val lineTrackColor = if (isPast || isActive) {
                                    GrimoireColors.GoldMuted.copy(alpha = 0.65f)
                                } else {
                                    GrimoireColors.BorderBronze.copy(alpha = 0.4f)
                                }

                                // Top track segment
                                if (index > 0) {
                                    drawLine(
                                        color = lineTrackColor,
                                        start = Offset(centerX, 0f),
                                        end = Offset(centerX, centerY),
                                        strokeWidth = 2f
                                    )
                                }

                                // Bottom track segment
                                if (index < headings.lastIndex) {
                                    val bottomTrackColor = if (isPast) {
                                        GrimoireColors.GoldMuted.copy(alpha = 0.65f)
                                    } else {
                                        GrimoireColors.BorderBronze.copy(alpha = 0.4f)
                                    }
                                    drawLine(
                                        color = bottomTrackColor,
                                        start = Offset(centerX, centerY),
                                        end = Offset(centerX, size.height),
                                        strokeWidth = 2f
                                    )
                                }

                                val radius = when (heading.level) {
                                    1 -> 6f * density
                                    2 -> 4.5f * density
                                    else -> 3.5f * density
                                }

                                // Glow for active node
                                if (isActive) {
                                    drawCircle(
                                        color = GrimoireColors.GoldBright.copy(alpha = 0.3f),
                                        radius = radius + 3.5f * density,
                                        center = Offset(centerX, centerY)
                                    )
                                }

                                // Fill
                                val fillColor = when {
                                    isActive -> GrimoireColors.GoldBright
                                    heading.level == 1 -> GrimoireColors.GoldPrimary
                                    heading.level == 2 -> if (isPast) GrimoireColors.GoldMuted else GrimoireColors.SurfaceCard
                                    else -> if (isPast) GrimoireColors.GoldMuted.copy(alpha = 0.8f) else GrimoireColors.BorderBronze
                                }
                                drawCircle(
                                    color = fillColor,
                                    radius = radius,
                                    center = Offset(centerX, centerY)
                                )

                                // Border
                                val strokeColor = when {
                                    isActive -> GrimoireColors.OnGoldContainer
                                    heading.level == 1 -> GrimoireColors.BorderGold
                                    heading.level == 2 -> GrimoireColors.GoldBright.copy(alpha = 0.8f)
                                    else -> GrimoireColors.BorderBronze
                                }
                                drawCircle(
                                    color = strokeColor,
                                    radius = radius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.5f * density)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            // Heading Title
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = heading.title,
                                    style = when (heading.level) {
                                        1 -> MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        2 -> MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                                        else -> MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal, fontSize = 11.sp)
                                    },
                                    color = when {
                                        isActive -> GrimoireColors.GoldBright
                                        heading.level == 1 -> GrimoireColors.ParchmentLight
                                        else -> GrimoireColors.ParchmentMuted
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Heading level indicator
                            Text(
                                text = "H${heading.level}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentDim,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
