package de.luca.dungeon_master_manager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.ui.EntityHoverTextLayout
import de.luca.dungeon_master_manager.ui.VerticalTextLayout
import de.luca.dungeon_master_manager.ui.calculateVerticalMove
import de.luca.dungeon_master_manager.ui.expandMarkdownTokenSelection
import de.luca.dungeon_master_manager.ui.findEntityAtPosition
import de.luca.dungeon_master_manager.ui.findImageLinkAtPosition
import de.luca.dungeon_master_manager.ui.resolveImageFile
import de.luca.dungeon_master_manager.ui.components.ImagePickerHelper
import de.luca.dungeon_master_manager.ui.components.cleanHeadingTitle
import de.luca.dungeon_master_manager.ui.components.extractMarkdownHeadings
import de.luca.dungeon_master_manager.ui.parseMarkdown
import de.luca.dungeon_master_manager.ui.replaceEntityQueryWithLink
import de.luca.dungeonmastermanager.database.Entity
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownParserTest {

    private val primaryColor = Color(0xFFD4AF37)
    private val fantasyFamily = FontFamily.Serif

    @Test
    fun testBoldFormatting_CursorOutside_HidesSyntax() {
        val text = "Hello **world** test"
        // Cursor at offset 0 (at 'H') -> outside **world**
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(0), emptyList(), fantasyFamily)
        
        // Syntax '**' should be hidden, so transformed text is "Hello world test"
        assertEquals("Hello world test", annotated.text)
        
        // OffsetMapping tests:
        // 'H' (orig 0) -> trans 0
        assertEquals(0, mapping.originalToTransformed(0))
        // 'w' (orig 8) -> trans 6
        assertEquals(6, mapping.originalToTransformed(8))
        // Click on 'w' (trans 6) -> orig 8
        assertEquals(8, mapping.transformedToOriginal(6))
    }

    @Test
    fun testBoldFormatting_CursorInside_ShowsSyntax() {
        val text = "Hello **world** test"
        // Cursor at offset 9 (on 'o' in world) -> inside **world**
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(9), emptyList(), fantasyFamily)
        
        // Syntax '**' must be visible for editing
        assertEquals("Hello **world** test", annotated.text)
        
        // 1:1 mapping when syntax is revealed
        assertEquals(9, mapping.originalToTransformed(9))
        assertEquals(9, mapping.transformedToOriginal(9))
    }

    @Test
    fun testItalicAndUnderline_CursorOutside_HidesSyntax() {
        val text = "A *italic* and __underline__ B"
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(0), emptyList(), fantasyFamily)
        
        assertEquals("A italic and underline B", annotated.text)
    }

    @Test
    fun testHeading_CursorOnOtherLine_HidesHashPrefix() {
        val text = "# Heading 1\nSecond line"
        // Cursor is on line 2 (offset 15) -> outside heading line
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(15), emptyList(), fantasyFamily)
        
        // "# " should be hidden in transformed text
        assertEquals("Heading 1\nSecond line", annotated.text)
        
        // Click on 'H' (trans 0) maps to orig 2 ('H' after "# ")
        assertEquals(2, mapping.transformedToOriginal(0))
    }

    @Test
    fun testHeading_CursorOnHeadingLine_ShowsHashPrefix() {
        val text = "# Heading 1\nSecond line"
        // Cursor is on heading line (offset 4, at 'a')
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(4), emptyList(), fantasyFamily)
        
        // Full "# Heading 1\nSecond line" must be visible
        assertEquals("# Heading 1\nSecond line", annotated.text)
        assertEquals(4, mapping.originalToTransformed(4))
        assertEquals(4, mapping.transformedToOriginal(4))
    }

    @Test
    fun testEntityMention_CursorOutside_ShowsEntityName() {
        val text = "Talk to @[goblin_king] now."
        val mockEntity = Entity(
            id = "goblin_king",
            name = "Grom the Goblin King",
            type = "NPC",
            color = null,
            notesFilePath = null,
            imageFilePath = null
        )
        
        // Cursor at offset 0 -> outside entity
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(0), listOf(mockEntity), fantasyFamily)
        
        // Transformed text replaces @[goblin_king] with entity name
        assertEquals("Talk to Grom the Goblin King now.", annotated.text)
        
        // Offsets before entity map 1:1
        assertEquals(0, mapping.originalToTransformed(0))
        // Click inside entity name maps into @[goblin_king]
        val origForEntityClick = mapping.transformedToOriginal(10) // clicking around 'Grom'
        assertTrue(origForEntityClick in 8..21, "Clicked entity should map to original entity range")
    }

    @Test
    fun testEntityMention_CursorInside_ShowsRawSyntax() {
        val text = "Talk to @[goblin_king] now."
        val mockEntity = Entity(
            id = "goblin_king",
            name = "Grom the Goblin King",
            type = "NPC",
            color = null,
            notesFilePath = null,
            imageFilePath = null
        )
        
        // Cursor at offset 12 (inside "goblin_king")
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(12), listOf(mockEntity), fantasyFamily)
        
        // Raw syntax visible for direct editing
        assertEquals("Talk to @[goblin_king] now.", annotated.text)
        assertEquals(12, mapping.originalToTransformed(12))
    }

    @Test
    fun testOffsetMapping_MonotonicityAndBounds() {
        val text = "# Title\nSome **bold** and *italic* and @[ent] end."
        val mockEntity = Entity(
            id = "ent",
            name = "Named Entity",
            type = "NPC",
            color = null,
            notesFilePath = null,
            imageFilePath = null
        )
        
        // Test with cursor at start (everything hidden)
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(0), listOf(mockEntity), fantasyFamily)
        
        // Check bounds and non-decreasing property for originalToTransformed
        var prevTrans = -1
        for (i in 0..text.length) {
            val trans = mapping.originalToTransformed(i)
            assertTrue(trans in 0..annotated.text.length, "Transformed offset out of bounds: $trans for orig: $i")
            assertTrue(trans >= prevTrans, "originalToTransformed is not monotonic: $trans < $prevTrans at $i")
            prevTrans = trans
        }

        // Check bounds and non-decreasing property for transformedToOriginal
        var prevOrig = -1
        for (i in 0..annotated.text.length) {
            val orig = mapping.transformedToOriginal(i)
            assertTrue(orig in 0..text.length, "Original offset out of bounds: $orig for trans: $i")
            assertTrue(orig >= prevOrig, "transformedToOriginal is not monotonic: $orig < $prevOrig at $i")
            prevOrig = orig
        }
    }

    @Test
    fun testHeading_CursorAtEndOfLine_RevealsSyntax() {
        val text = "# Heading\nNext line"
        // Cursor at offset 9 (the position right after 'g', before '\n')
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(9), emptyList(), fantasyFamily)
        assertEquals("# Heading\nNext line", annotated.text)
    }

    @Test
    fun testHeadingLevels_DifferentHashes() {
        val text = "### Heading 3\nText"
        // Outside heading line
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(16), emptyList(), fantasyFamily)
        assertEquals("Heading 3\nText", annotated.text)
    }

    @Test
    fun testMultipleEntities_IndependentFocus() {
        val mockEnt1 = Entity("e1", "Hero", "NPC", null, null, null)
        val mockEnt2 = Entity("e2", "Villain", "NPC", null, null, null)
        val text = "@[e1] vs @[e2]"

        // Focus inside @[e1] (offset 2)
        val (annotated1, _) = parseMarkdown(text, primaryColor, TextRange(2), listOf(mockEnt1, mockEnt2), fantasyFamily)
        // e1 should be raw "@[e1]", while e2 should be transformed to "Villain"
        assertEquals("@[e1] vs Villain", annotated1.text)

        // Focus inside @[e2] (offset 11)
        val (annotated2, _) = parseMarkdown(text, primaryColor, TextRange(11), listOf(mockEnt1, mockEnt2), fantasyFamily)
        // e1 should be transformed to "Hero", while e2 should be raw "@[e2]"
        assertEquals("Hero vs @[e2]", annotated2.text)
    }

    @Test
    fun testRealWorldMainMd_AllOffsets() {
        val mainMdContent = """
# Kapitel 1

Dein Abenteuer beginnt...
@[a904acec-6288-433d-84b3-2b4060eb23a2]
@[d2d19c17-f139-4473-89cd-d6b4af1209d3] wdawdawdaw d  awdawdaawdawdawdawdawwdawdawd a a wdawd a aaw dsssssssssssswwdawdawdasd 
w
wwwwwwwwwww
w
w
w


w
w
w
w
w
ww
w
ww
ww
w
www
w
w
w
ww
wawdawawdawdawdawwd awd awdwd awdawd awd awd awdwd awdwdadadawdawd awd 








""".trimIndent()

        val mockEnt1 = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null, null)
        val mockEnt2 = Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null, null)
        val entities = listOf(mockEnt1, mockEnt2)

        for (cursor in 0..mainMdContent.length) {
            val (annotated, mapping) = parseMarkdown(mainMdContent, primaryColor, TextRange(cursor), entities, fantasyFamily)
            for (offset in 0..mainMdContent.length) {
                val trans = mapping.originalToTransformed(offset)
                assertTrue(trans in 0..annotated.length, "origToTrans out of bounds at cursor $cursor, offset $offset: $trans not in 0..${annotated.length}")
            }
            for (trans in 0..annotated.length) {
                val orig = mapping.transformedToOriginal(trans)
                assertTrue(orig in 0..mainMdContent.length, "transToOrig out of bounds at cursor $cursor, trans $trans: $orig not in 0..${mainMdContent.length}")
            }
        }
    }

    @Test
    fun testEntityBoundary_CursorAtFirstChar_StaysCollapsed() {
        val text = "Before @[goblin] after"
        val mockEnt = Entity("goblin", "Grom", "NPC", null, null, null)
        // Cursor at offset 7 (the position of '@')
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(7), listOf(mockEnt), fantasyFamily)
        // At the boundary, entity stays collapsed as "Grom" so arrow navigation past it doesn't explode
        assertEquals("Before Grom after", annotated.text)
    }

    @Test
    fun testEntityClick_ExpandsCorrectly() {
        val text = "Before @[goblin] after"
        val mockEnt = Entity("goblin", "Grom", "NPC", null, null, null)
        // Initially cursor is outside (offset 0)
        val (_, mapping) = parseMarkdown(text, primaryColor, TextRange(0), listOf(mockEnt), fantasyFamily)
        // User clicks on character 'r' in "Grom" (transformed offset 8: "Before G" is 8)
        val clickedOrig = mapping.transformedToOriginal(8)
        // clickedOrig should be 8 (match.range.first + 1)
        assertEquals(8, clickedOrig)

        // Now with cursor placed at clickedOrig (offset 8, inside @[goblin])
        val (annotatedAfterClick, _) = parseMarkdown(text, primaryColor, TextRange(clickedOrig), listOf(mockEnt), fantasyFamily)
        // It expands!
        assertEquals("Before @[goblin] after", annotatedAfterClick.text)
    }

    // --- Helpers for Vertical Arrow Navigation Tests ---

    private data class SimVisualLine(
        val start: Int,
        val visibleEnd: Int,
        val rawEnd: Int
    )

    private class MonospaceVerticalLayout(
        text: String,
        maxCharsPerLine: Int = 1000,
        private val charWidth: Float = 10f,
        private val lineHeight: Float = 20f
    ) : VerticalTextLayout {
        private val lines: List<SimVisualLine> = buildList {
            if (text.isEmpty()) {
                add(SimVisualLine(0, 0, 0))
            } else {
                var idx = 0
                while (idx < text.length) {
                    val nl = text.indexOf('\n', idx)
                    val pEnd = if (nl == -1) text.length else nl
                    val hasNl = nl != -1
                    if (idx == pEnd) {
                        add(SimVisualLine(idx, pEnd, if (hasNl) pEnd + 1 else pEnd))
                    } else {
                        var chunkStart = idx
                        while (chunkStart < pEnd) {
                            val chunkEnd = (chunkStart + maxCharsPerLine).coerceAtMost(pEnd)
                            val isLastChunk = chunkEnd == pEnd
                            val rawEnd = if (isLastChunk && hasNl) pEnd + 1 else chunkEnd
                            add(SimVisualLine(chunkStart, chunkEnd, rawEnd))
                            chunkStart = chunkEnd
                        }
                    }
                    idx = if (hasNl) pEnd + 1 else pEnd
                }
                if (text.endsWith('\n')) {
                    add(SimVisualLine(text.length, text.length, text.length))
                }
            }
        }

        override val lineCount: Int get() = lines.size

        override fun getLineForOffset(offset: Int): Int {
            for (i in lines.indices) {
                if (offset < lines[i].rawEnd || i == lines.lastIndex) {
                    return i
                }
            }
            return lines.lastIndex
        }

        override fun getCursorHorizontal(offset: Int): Float {
            val lineIdx = getLineForOffset(offset)
            val col = (offset - lines[lineIdx].start).coerceAtLeast(0)
            return col * charWidth
        }

        override fun getLineLeft(lineIndex: Int): Float = 0f

        override fun getLineRight(lineIndex: Int): Float =
            (lines[lineIndex].visibleEnd - lines[lineIndex].start) * charWidth

        override fun getLineTop(lineIndex: Int): Float = lineIndex * lineHeight

        override fun getLineBottom(lineIndex: Int): Float = (lineIndex + 1) * lineHeight

        override fun getLineStart(lineIndex: Int): Int = lines[lineIndex].start

        override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int =
            if (visibleEnd) lines[lineIndex].visibleEnd else lines[lineIndex].rawEnd

        override fun getOffsetForPosition(x: Float, y: Float): Int {
            val lineIdx = (y / lineHeight).toInt().coerceIn(0, lines.lastIndex)
            val line = lines[lineIdx]
            val col = (x / charWidth).roundToInt().coerceIn(0, line.visibleEnd - line.start)
            return line.start + col
        }
    }

    /**
     * Proportional layout simulator that accounts for SpanStyle font sizes (e.g. 26sp headings),
     * bold weights, narrow spaces/punctuation, and soft-wrapping at [maxWidthPx] without requiring native Skiko DLLs.
     */
    private class ProportionalAnnotatedLayout(
        private val annotated: AnnotatedString,
        maxWidthPx: Float = 1200f,
        private val lineHeight: Float = 24f
    ) : VerticalTextLayout {
        private val charWidths = FloatArray(annotated.text.length) { idx ->
            val ch = annotated.text[idx]
            val base = when {
                ch == '\n' || ch == '\r' -> 0f
                ch == ' ' -> 4.5f
                ch in "iIl.,!|[]" -> 4.0f
                ch in "mwMW@#" -> 11.5f
                else -> 8.2f
            }
            var scale = 1f
            for (span in annotated.spanStyles) {
                if (idx in span.start until span.end) {
                    if (span.item.fontSize.isSpecified) {
                        scale = maxOf(scale, span.item.fontSize.value / 16f)
                    }
                    if (span.item.fontWeight == FontWeight.Bold) {
                        scale *= 1.08f
                    }
                }
            }
            base * scale
        }

        private val lines: List<SimVisualLine> = buildList {
            val text = annotated.text
            if (text.isEmpty()) {
                add(SimVisualLine(0, 0, 0))
            } else {
                var idx = 0
                while (idx < text.length) {
                    val nl = text.indexOf('\n', idx)
                    val pEnd = if (nl == -1) text.length else nl
                    val hasNl = nl != -1
                    if (idx == pEnd) {
                        add(SimVisualLine(idx, pEnd, if (hasNl) pEnd + 1 else pEnd))
                    } else {
                        var chunkStart = idx
                        while (chunkStart < pEnd) {
                            var width = 0f
                            var chunkEnd = chunkStart
                            while (chunkEnd < pEnd) {
                                val w = charWidths[chunkEnd]
                                if (chunkEnd > chunkStart && width + w > maxWidthPx) break
                                width += w
                                chunkEnd++
                            }
                            val isLastChunk = chunkEnd == pEnd
                            val rawEnd = if (isLastChunk && hasNl) pEnd + 1 else chunkEnd
                            add(SimVisualLine(chunkStart, chunkEnd, rawEnd))
                            chunkStart = chunkEnd
                        }
                    }
                    idx = if (hasNl) pEnd + 1 else pEnd
                }
                if (text.endsWith('\n')) {
                    add(SimVisualLine(text.length, text.length, text.length))
                }
            }
        }

        override val lineCount: Int get() = lines.size

        override fun getLineForOffset(offset: Int): Int {
            for (i in lines.indices) {
                if (offset < lines[i].rawEnd || i == lines.lastIndex) return i
            }
            return lines.lastIndex
        }

        override fun getCursorHorizontal(offset: Int): Float {
            val lineIdx = getLineForOffset(offset)
            val line = lines[lineIdx]
            val clamped = offset.coerceIn(line.start, line.visibleEnd)
            var x = 0f
            for (i in line.start until clamped) x += charWidths[i]
            return x
        }

        override fun getLineLeft(lineIndex: Int): Float = 0f

        override fun getLineRight(lineIndex: Int): Float {
            val line = lines[lineIndex]
            var x = 0f
            for (i in line.start until line.visibleEnd) x += charWidths[i]
            return x
        }

        override fun getLineTop(lineIndex: Int): Float = lineIndex * lineHeight

        override fun getLineBottom(lineIndex: Int): Float = (lineIndex + 1) * lineHeight

        override fun getLineStart(lineIndex: Int): Int = lines[lineIndex].start

        override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int =
            if (visibleEnd) lines[lineIndex].visibleEnd else lines[lineIndex].rawEnd

        override fun getOffsetForPosition(x: Float, y: Float): Int {
            val lineIdx = (y / lineHeight).toInt().coerceIn(0, lines.lastIndex)
            val line = lines[lineIdx]
            var bestOffset = line.start
            var bestDist = abs(x)
            var curX = 0f
            for (i in line.start until line.visibleEnd) {
                curX += charWidths[i]
                val dist = abs(x - curX)
                if (dist < bestDist) {
                    bestDist = dist
                    bestOffset = i + 1
                }
            }
            return bestOffset
        }
    }

    private fun originalLineIndex(text: String, offset: Int): Int =
        text.substring(0, offset.coerceIn(0, text.length)).count { it == '\n' }

    @Test
    fun testVerticalMove_LongToShortToEmptyToLong_PreservesStickyX_WithHiddenMarkdownAndEntities() {
        val ent1 = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null, null)
        val ent2 = Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null, null)
        val entities = listOf(ent1, ent2)

        val lines = listOf(
            "# Kapitel 1",                                              // line 0 (hides "# ")
            "Text mit **fett**, *kursiv* und __unter__ hier",           // line 1 (hides 10 syntax chars)
            "@[a904acec-6288-433d-84b3-2b4060eb23a2]",                  // line 2 (hides ~30 chars)
            "0123456789abcdefghij",                                     // line 3 (long line, 20 chars)
            "w",                                                        // line 4 (short line, 1 char)
            "",                                                         // line 5 (empty line)
            "Hi **Welt**",                                              // line 6 (short formatted line)
            "Hi @[d2d19c17-f139-4473-89cd-d6b4af1209d3]",               // line 7 (short entity line -> "Hi Jerry")
            "0123456789ABCDEFGHIJ"                                      // line 8 (long line, 20 chars)
        )
        val doc = lines.joinToString("\n")

        fun lineStartInDoc(lineIdx: Int): Int =
            lines.take(lineIdx).sumOf { it.length + 1 }
        fun lineEndInDoc(lineIdx: Int): Int =
            lineStartInDoc(lineIdx) + lines[lineIdx].length

        // Place cursor at column 15 ('f') on long line 3
        var selection = TextRange(lineStartInDoc(3) + 15)
        var stickyX: Float? = null

        fun step(direction: Int) {
            val (annotated, mapping) = parseMarkdown(doc, primaryColor, selection, entities, fantasyFamily)
            val layout = MonospaceVerticalLayout(annotated.text)
            val res = calculateVerticalMove(
                originalText = doc,
                transformedText = annotated.text,
                currentSelection = selection,
                direction = direction,
                extendSelection = false,
                cachedStickyX = stickyX,
                layout = layout,
                offsetMapping = mapping
            )
            selection = res.selection
            stickyX = res.stickyX
        }

        // Down 1: lands at exact end of short line 4 ("w")
        step(+1)
        assertEquals(lineEndInDoc(4), selection.start)

        // Down 2: lands at exact offset of empty line 5 ("")
        step(+1)
        assertEquals(lineEndInDoc(5), selection.start)

        // Down 3: lands at exact end of "Hi **Welt**" (line 6), keeping **Welt** collapsed
        step(+1)
        assertEquals(lineEndInDoc(6), selection.start)
        val (annLine6, _) = parseMarkdown(doc, primaryColor, selection, entities, fantasyFamily)
        assertTrue(!annLine6.text.contains("**Welt**"), "Landing at line end should keep **Welt** collapsed")

        // Down 4: lands at exact end of "Hi @[uuid]" (line 7), keeping "Jerry" collapsed
        step(+1)
        assertEquals(lineEndInDoc(7), selection.start)
        val (annLine7, _) = parseMarkdown(doc, primaryColor, selection, entities, fantasyFamily)
        assertTrue(!annLine7.text.contains(ent2.id), "Landing at line end should keep entity collapsed")

        // Down 5: returns to column 15 ('F') on long line 8 via Sticky X!
        step(+1)
        assertEquals(lineStartInDoc(8) + 15, selection.start)

        // Now move all the way back UP to line 3 and verify symmetry!
        step(-1)
        assertEquals(lineEndInDoc(7), selection.start)
        step(-1)
        assertEquals(lineEndInDoc(6), selection.start)
        step(-1)
        assertEquals(lineEndInDoc(5), selection.start)
        step(-1)
        assertEquals(lineEndInDoc(4), selection.start)
        step(-1)
        assertEquals(lineStartInDoc(3) + 15, selection.start)
    }

    @Test
    fun testVerticalMove_FromInsideExpandedEntity_OntoShorterAndEmptyLine_NeverGetsStuck() {
        val ent1 = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null, null)
        val ent2 = Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null, null)
        val entities = listOf(ent1, ent2)

        val doc = listOf(
            "# Kapitel 1",
            "@[a904acec-6288-433d-84b3-2b4060eb23a2]",
            "Start @[d2d19c17-f139-4473-89cd-d6b4af1209d3] Ende",
            "",
            "w",
            "Unten angekommen"
        ).joinToString("\n")

        val ent2Index = doc.indexOf("@[d2d19c17")
        // Put cursor right after '@' inside expanded @[d2d19c17-...] (strictly inside token)
        var selection = TextRange(ent2Index + 2)
        var stickyX: Float? = null

        // Verify entity 2 is indeed expanded before pressing Down
        val (initialAnn, _) = parseMarkdown(doc, primaryColor, selection, entities, fantasyFamily)
        assertTrue(initialAnn.text.contains("@[d2d19c17-f139-4473-89cd-d6b4af1209d3]"))

        for (expectedLine in listOf(3, 4, 5)) {
            val (annotated, mapping) = parseMarkdown(doc, primaryColor, selection, entities, fantasyFamily)
            val res = calculateVerticalMove(
                originalText = doc,
                transformedText = annotated.text,
                currentSelection = selection,
                direction = +1,
                extendSelection = false,
                cachedStickyX = stickyX,
                layout = MonospaceVerticalLayout(annotated.text),
                offsetMapping = mapping
            )
            selection = res.selection
            stickyX = res.stickyX
            assertEquals(expectedLine, originalLineIndex(doc, selection.start))
        }
    }

    @Test
    fun testVerticalMove_SoftWrappedExpandedEntity_ExitsInSingleKeyPress_UpAndDown() {
        val ent = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "NPC", "NPC", null, null, null)
        val doc = listOf(
            "Obere Zeile",
            "Vor @[a904acec-6288-433d-84b3-2b4060eb23a2]",
            "Untere Zeile"
        ).joinToString("\n")

        val entityAt = doc.indexOf("@[a904acec")

        // 1) Cursor near start of expanded entity (first wrapped visual line, maxCharsPerLine = 25)
        val startInsideEntity = TextRange(entityAt + 2)
        val (annDown, mapDown) = parseMarkdown(doc, primaryColor, startInsideEntity, listOf(ent), fantasyFamily)
        val wrappedLayoutDown = MonospaceVerticalLayout(annDown.text, maxCharsPerLine = 25)
        assertTrue(wrappedLayoutDown.lineCount >= 4, "Expanded entity line should wrap across 2 visual lines")

        // Single Arrow Down must skip the second half of the wrapped UUID and land directly on "Untere Zeile" (line 2)
        val downRes = calculateVerticalMove(
            originalText = doc,
            transformedText = annDown.text,
            currentSelection = startInsideEntity,
            direction = +1,
            extendSelection = false,
            cachedStickyX = null,
            layout = wrappedLayoutDown,
            offsetMapping = mapDown
        )
        assertEquals(2, originalLineIndex(doc, downRes.selection.start))

        // 2) Cursor near end of expanded entity (second wrapped visual line)
        val endInsideEntity = TextRange(entityAt + 35)
        val (annUp, mapUp) = parseMarkdown(doc, primaryColor, endInsideEntity, listOf(ent), fantasyFamily)
        val wrappedLayoutUp = MonospaceVerticalLayout(annUp.text, maxCharsPerLine = 25)

        // Single Arrow Up must skip the first half of the wrapped UUID and land directly on "Obere Zeile" (line 0)
        val upRes = calculateVerticalMove(
            originalText = doc,
            transformedText = annUp.text,
            currentSelection = endInsideEntity,
            direction = -1,
            extendSelection = false,
            cachedStickyX = null,
            layout = wrappedLayoutUp,
            offsetMapping = mapUp
        )
        assertEquals(0, originalLineIndex(doc, upRes.selection.start))
    }

    @Test
    fun testVerticalMove_ShiftUpAndDown_ExtendsAndPreservesAnchor() {
        val doc = "# Titel\nErste **fette** Zeile\nKurz\nDritte lange Zeile"
        val line1Col10 = "# Titel\n".length + 10

        // Start collapsed at line 1, col 10 -> Shift+Down onto shorter line 2 ("Kurz")
        val initial = TextRange(line1Col10)
        val (ann1, map1) = parseMarkdown(doc, primaryColor, initial, emptyList(), fantasyFamily)
        val downSel = calculateVerticalMove(
            originalText = doc,
            transformedText = ann1.text,
            currentSelection = initial,
            direction = +1,
            extendSelection = true,
            cachedStickyX = null,
            layout = MonospaceVerticalLayout(ann1.text),
            offsetMapping = map1
        )

        assertEquals(line1Col10, downSel.selection.start, "Selection anchor must stay fixed on Shift+Down")
        val expectedKurzEnd = "# Titel\nErste **fette** Zeile\nKurz".length
        assertEquals(expectedKurzEnd, downSel.selection.end, "Selection end must clamp to original end of 'Kurz'")

        // Shift+Up twice from initial -> reversed selection (start > end) into line 0 ("# Titel")
        val upSel = calculateVerticalMove(
            originalText = doc,
            transformedText = ann1.text,
            currentSelection = initial,
            direction = -1,
            extendSelection = true,
            cachedStickyX = null,
            layout = MonospaceVerticalLayout(ann1.text),
            offsetMapping = map1
        )
        assertEquals(line1Col10, upSel.selection.start)
        assertTrue(upSel.selection.reversed, "Upward Shift selection must remain reversed (start > end)")
        assertEquals(0, originalLineIndex(doc, upSel.selection.end))

        // Also check expandMarkdownTokenSelection keeps reversed direction intact
        val sample = "abc **bold** xyz"
        val reversedInner = TextRange(10, 6) // "bold" selected backwards (end=6, start=10)
        val expanded = expandMarkdownTokenSelection(sample, reversedInner)
        assertEquals(TextRange(12, 4), expanded)
        assertTrue(expanded.reversed)
    }

    @Test
    fun testVerticalMove_ProportionalSpanLayout_MainMdEndToEnd() {
        val mainMdContent = """
# Kapitel 1

Dein Abenteuer beginnt...
@[a904acec-6288-433d-84b3-2b4060eb23a2]
@[d2d19c17-f139-4473-89cd-d6b4af1209d3] wdawdawdaw d  awdawdaawdawdawdawdawwdawdawd a a wdawd a aaw dsssssssssssswwdawdawdasd 
w
wwwwwwwwwww
w
w
w


w
w
w
ww
w
www
wawdawawdawdawdawwd awd awdwd awdawd awd awd awdwd awdwdadadawdawd awd 
""".trimIndent()

        val entities = listOf(
            Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null, null),
            Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null, null)
        )

        // Start far to the right on the long "Jerry wdawdawdaw..." line
        val jerryLineOffset = mainMdContent.indexOf("wdawdawdaw") + 25
        var selection = TextRange(jerryLineOffset)
        var stickyX: Float? = null

        // Walk all the way down to EOF across variable-width spans, soft-wraps, short lines, and empty lines
        var stepsDown = 0
        while (selection.start < mainMdContent.length && stepsDown < 45) {
            val prevLine = originalLineIndex(mainMdContent, selection.start)
            val prevOffset = selection.start
            val (annotated, mapping) = parseMarkdown(mainMdContent, primaryColor, selection, entities, fantasyFamily)
            val propLayout = ProportionalAnnotatedLayout(annotated, maxWidthPx = 480f)
            val res = calculateVerticalMove(
                originalText = mainMdContent,
                transformedText = annotated.text,
                currentSelection = selection,
                direction = +1,
                extendSelection = false,
                cachedStickyX = stickyX,
                layout = propLayout,
                offsetMapping = mapping
            )
            selection = res.selection
            stickyX = res.stickyX
            val newLine = originalLineIndex(mainMdContent, selection.start)
            assertTrue(
                newLine > prevLine || selection.start > prevOffset,
                "Arrow Down must never jump backwards (was line $prevLine offset $prevOffset, got line $newLine offset ${selection.start})"
            )
            stepsDown++
        }
        assertEquals(mainMdContent.length, selection.start, "Repeated Arrow Down must reach end of document")

        // Now walk all the way back up to BOF across variable-width spans, soft-wraps, short lines, and headings
        var stepsUp = 0
        while (selection.start > 0 && stepsUp < 45) {
            val prevLine = originalLineIndex(mainMdContent, selection.start)
            val prevOffset = selection.start
            val (annotated, mapping) = parseMarkdown(mainMdContent, primaryColor, selection, entities, fantasyFamily)
            val propLayout = ProportionalAnnotatedLayout(annotated, maxWidthPx = 480f)
            val res = calculateVerticalMove(
                originalText = mainMdContent,
                transformedText = annotated.text,
                currentSelection = selection,
                direction = -1,
                extendSelection = false,
                cachedStickyX = stickyX,
                layout = propLayout,
                offsetMapping = mapping
            )
            selection = res.selection
            stickyX = res.stickyX
            val newLine = originalLineIndex(mainMdContent, selection.start)
            assertTrue(
                newLine < prevLine || selection.start < prevOffset,
                "Arrow Up must never jump forwards (was line $prevLine offset $prevOffset, got line $newLine offset ${selection.start})"
            )
            stepsUp++
        }
        assertEquals(0, selection.start, "Repeated Arrow Up must reach start of document")
    }

    @Test
    fun testReplaceEntityQueryWithLink_VariousCursorAndQueryScenarios() {
        // Scenario 1: Just "@" at the beginning of document
        val (text1, sel1) = replaceEntityQueryWithLink(
            text = "@",
            cursor = 1,
            query = "",
            entityId = "ent-123"
        )
        assertEquals("@[ent-123]", text1)
        assertEquals(10, sel1.start) // after ']'
        assertEquals(sel1.start, sel1.end)

        // Scenario 2: Typed "@Gro" in the middle of a sentence
        val original2 = "Speak with @Gro and finish the quest."
        val cursor2 = original2.indexOf("@Gro") + 4 // cursor right after 'o'
        val (text2, sel2) = replaceEntityQueryWithLink(
            text = original2,
            cursor = cursor2,
            query = "Gro",
            entityId = "grom-the-goblin"
        )
        assertEquals("Speak with @[grom-the-goblin] and finish the quest.", text2)
        assertEquals("Speak with @[grom-the-goblin]".length, sel2.start)

        // Scenario 3: At the end of document with multi-word query
        val original3 = "Boss is @Goblin King"
        val cursor3 = original3.length
        val (text3, sel3) = replaceEntityQueryWithLink(
            text = original3,
            cursor = cursor3,
            query = "Goblin King",
            entityId = "king-id"
        )
        assertEquals("Boss is @[king-id]", text3)
        assertEquals("Boss is @[king-id]".length, sel3.start)

        // Scenario 4: Cursor right behind '@' of an existing entity link (e.g. replacing existing entity)
        val original4 = "Treffe @[d2d19c17-f139-4473-89cd-d6b4af1209d3] am Tor."
        val cursor4 = original4.indexOf("@") + 1 // cursor right after '@', before '['
        val (text4, sel4) = replaceEntityQueryWithLink(
            text = original4,
            cursor = cursor4,
            query = "",
            entityId = "new-hero-id"
        )
        // The old [d2d19c17...] must NOT remain as trailing text!
        assertEquals("Treffe @[new-hero-id] am Tor.", text4)
        assertEquals("Treffe @[new-hero-id]".length, sel4.start)

        // Scenario 5: Cursor behind '@' of an existing entity link with query typed between @ and [
        val original5 = "Treffe @Jerry[d2d19c17-f139-4473-89cd-d6b4af1209d3] am Tor."
        val cursor5 = original5.indexOf("@Jerry") + 6 // right after 'y'
        val (text5, sel5) = replaceEntityQueryWithLink(
            text = original5,
            cursor = cursor5,
            query = "Jerry",
            entityId = "jerry-uuid-123"
        )
        assertEquals("Treffe @[jerry-uuid-123] am Tor.", text5)
        assertEquals("Treffe @[jerry-uuid-123]".length, sel5.start)

        // Scenario 6: Cursor anywhere inside the UUID between @ and ] (e.g. in the middle or right before ']')
        val original6 = "Treffe @[d2d19c17-f139-4473-89cd-d6b4af1209d3] am Tor."
        val cursorMiddle = original6.indexOf("f139") // middle of UUID
        val (text6Middle, sel6Middle) = replaceEntityQueryWithLink(
            text = original6,
            cursor = cursorMiddle,
            query = "",
            entityId = "replaced-middle-id"
        )
        assertEquals("Treffe @[replaced-middle-id] am Tor.", text6Middle)
        assertEquals("Treffe @[replaced-middle-id]".length, sel6Middle.start)

        val cursorBeforeBracket = original6.indexOf("]") // right before ']'
        val (text6End, sel6End) = replaceEntityQueryWithLink(
            text = original6,
            cursor = cursorBeforeBracket,
            query = "",
            entityId = "replaced-end-id"
        )
        assertEquals("Treffe @[replaced-end-id] am Tor.", text6End)
        assertEquals("Treffe @[replaced-end-id]".length, sel6End.start)

        // Verify that the resulting text with the new selection keeps the entity collapsed when parsed!
        val mockEnt = Entity("king-id", "The King", "NPC", null, null, null)
        val (annotated, _) = parseMarkdown(text3, primaryColor, sel3, listOf(mockEnt), fantasyFamily)
        assertEquals("Boss is The King", annotated.text, "Entity must be collapsed when cursor lands right after ']'")
    }

    @Test
    fun testInsideEntityDetectionRange() {
        val text = "Vor @[entity-123] Nach"
        val atPos = text.indexOf("@")
        val bracketPos = text.indexOf("]")

        val regex = Regex("(?<!\\\\)@\\[(.*?)\\]")

        fun isInside(cursor: Int): Boolean =
            regex.findAll(text).any { cursor in (it.range.first + 1)..it.range.last }

        // Cursor before '@' -> false
        assertTrue(!isInside(atPos))

        // Cursor right after '@' -> true
        assertTrue(isInside(atPos + 1))

        // Cursor inside '[' -> true
        assertTrue(isInside(atPos + 2))

        // Cursor in middle of ID -> true
        assertTrue(isInside(text.indexOf("entity")))

        // Cursor right before ']' -> true
        assertTrue(isInside(bracketPos))

        // Cursor right after ']' -> false
        assertTrue(!isInside(bracketPos + 1))
    }

    @Test
    fun testDropdownCyclicNavigation() {
        val listSize = 5

        // Starting at 0: Arrow Down steps forward
        var index = 0
        index = (index + 1) % listSize
        assertEquals(1, index)

        index = (index + 1) % listSize
        assertEquals(2, index)

        // Moving to the end
        index = 4
        // Arrow Down wraps around to 0
        index = (index + 1) % listSize
        assertEquals(0, index)

        // From 0, Arrow Up wraps around to last item (4)
        index = (index - 1 + listSize) % listSize
        assertEquals(4, index)

        // Arrow Up moves backward
        index = (index - 1 + listSize) % listSize
        assertEquals(3, index)
    }

    @Test
    fun testMarkdownHeadingScaling_ProportionalToBaseFontSize() {
        val markdown = "# Heading 1\n## Heading 2\n### Heading 3"

        // Default baseFontSize = 16
        val (annotated16, _) = parseMarkdown(markdown, primaryColor, TextRange(100), emptyList(), fantasyFamily, baseFontSize = 16)
        val styles16 = annotated16.spanStyles.filter { it.item.fontSize.isSpecified }
        assertEquals(3, styles16.size)
        assertEquals(26.sp, styles16[0].item.fontSize)
        assertEquals(22.sp, styles16[1].item.fontSize)
        assertEquals(18.sp, styles16[2].item.fontSize)

        // Scaled up baseFontSize = 24 (1.5x)
        val (annotated24, _) = parseMarkdown(markdown, primaryColor, TextRange(100), emptyList(), fantasyFamily, baseFontSize = 24)
        val styles24 = annotated24.spanStyles.filter { it.item.fontSize.isSpecified }
        assertEquals(3, styles24.size)
        assertEquals(39.sp, styles24[0].item.fontSize) // 26 * 1.5 = 39
        assertEquals(33.sp, styles24[1].item.fontSize) // 22 * 1.5 = 33
        assertEquals(27.sp, styles24[2].item.fontSize) // 18 * 1.5 = 27

        // Scaled down baseFontSize = 12 (0.75x)
        val (annotated12, _) = parseMarkdown(markdown, primaryColor, TextRange(100), emptyList(), fantasyFamily, baseFontSize = 12)
        val styles12 = annotated12.spanStyles.filter { it.item.fontSize.isSpecified }
        assertEquals(3, styles12.size)
        assertEquals(20.sp, styles12[0].item.fontSize) // round(26 * 0.75) = 20
        assertEquals(17.sp, styles12[1].item.fontSize) // round(22 * 0.75) = 17
        assertEquals(14.sp, styles12[2].item.fontSize) // round(18 * 0.75) = 14
    }

    @Test
    fun testAppSettings_EditorFontSizeClamping() {
        val min = AppSettingsRepository.MIN_EDITOR_FONT_SIZE
        val max = AppSettingsRepository.MAX_EDITOR_FONT_SIZE
        val defaultSize = AppSettingsRepository.DEFAULT_EDITOR_FONT_SIZE

        assertEquals(10, min)
        assertEquals(32, max)
        assertEquals(16, defaultSize)

        // Verify clamping logic
        assertEquals(min, (5).coerceIn(min, max))
        assertEquals(max, (50).coerceIn(min, max))
        assertEquals(20, (20).coerceIn(min, max))
    }

    @Test
    fun testAppSettings_EntityViewMode() {
        assertEquals("GRID", AppSettingsRepository.ENTITY_VIEW_MODE_GRID)
        assertEquals("LIST", AppSettingsRepository.ENTITY_VIEW_MODE_LIST)

        val defaultSettings = de.luca.dungeon_master_manager.data.AppSettings()
        assertEquals("GRID", defaultSettings.entityViewMode)
    }

    @Test
    fun testAppSettings_EditorTimelineVisible() {
        val defaultSettings = de.luca.dungeon_master_manager.data.AppSettings()
        assertTrue(defaultSettings.isEditorTimelineVisible)
    }

    @Test
    fun testCleanHeadingTitle() {
        assertEquals("Title", cleanHeadingTitle("**Title**"))
        assertEquals("Bold & Italic", cleanHeadingTitle("**Bold** & *Italic*"))
        assertEquals("Underlined Heading", cleanHeadingTitle("__Underlined Heading__"))
        assertEquals("Meet entity_123", cleanHeadingTitle("Meet @[entity_123]"))
    }

    @Test
    fun testExtractMarkdownHeadings() {
        val markdown = """
            # Act I: The Journey Begins
            Some intro text describing the realm.
            ## Chapter 1: The Crossroads
            A fork in the ancient forest road.
            ### Hidden Cave
            Deep under the roots.
            # Act II: The Shadows Deepen
            The darkness grows.
        """.trimIndent()

        val headings = extractMarkdownHeadings(markdown)
        assertEquals(4, headings.size)

        assertEquals(1, headings[0].level)
        assertEquals("Act I: The Journey Begins", headings[0].title)
        assertEquals(0, headings[0].lineIndex)

        assertEquals(2, headings[1].level)
        assertEquals("Chapter 1: The Crossroads", headings[1].title)
        assertEquals(2, headings[1].lineIndex)

        assertEquals(3, headings[2].level)
        assertEquals("Hidden Cave", headings[2].title)
        assertEquals(4, headings[2].lineIndex)

        assertEquals(1, headings[3].level)
        assertEquals("Act II: The Shadows Deepen", headings[3].title)
        assertEquals(6, headings[3].lineIndex)
    }

    @Test
    fun testFindEntityAtPosition_HoverDetection() {
        val ent1 = Entity("id-grom", "Grom the Goblin", "NPC", null, null, null)
        val text = "Meet @[id-grom] here."
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(0), listOf(ent1), fantasyFamily)
        assertEquals("Meet Grom the Goblin here.", annotated.text)

        // Mock layout where each character is 10px wide and 20px high
        val mockLayout = object : EntityHoverTextLayout {
            override val textLength: Int get() = annotated.text.length
            override fun getBoundingBox(offset: Int): Rect {
                val x = offset * 10f
                return Rect(x, 0f, x + 10f, 20f)
            }
        }

        // 'G' in "Grom..." is at transformed index 5 -> x between 50 and 60
        val posOverGrom = Offset(55f, 10f)
        val resultOverGrom = findEntityAtPosition(posOverGrom, mockLayout, mapping, text, listOf(ent1))
        assertEquals(ent1, resultOverGrom, "Hovering over entity name must find the entity")

        // Hovering right on the gap/boundary between characters (e.g. x = 70f between 'r' and 'o')
        val posOnLetterGap = Offset(70f, 10f)
        assertEquals(ent1, findEntityAtPosition(posOnLetterGap, mockLayout, mapping, text, listOf(ent1)), "Hovering on boundary between letters must find the entity")

        // Hovering on the space between words in "Grom the Goblin" (e.g. space at index 9, x = 95f)
        val posOnWordSpace = Offset(95f, 10f)
        assertEquals(ent1, findEntityAtPosition(posOnWordSpace, mockLayout, mapping, text, listOf(ent1)), "Hovering on space between words inside entity must find the entity")

        // Hovering slightly above the line within 2px padding (y = -1f)
        val posSlightlyAbove = Offset(55f, -1f)
        assertEquals(ent1, findEntityAtPosition(posSlightlyAbove, mockLayout, mapping, text, listOf(ent1)), "Hovering slightly above entity text within padding must find the entity")

        // Hovering over "Meet " (x = 20) -> not an entity
        val posOverMeet = Offset(20f, 10f)
        val resultOverMeet = findEntityAtPosition(posOverMeet, mockLayout, mapping, text, listOf(ent1))
        assertEquals(null, resultOverMeet, "Hovering over normal text must return null")

        // Hovering far to the right in empty margin (x = 400, line ends earlier)
        val posFarRight = Offset(400f, 10f)
        val resultFarRight = findEntityAtPosition(posFarRight, mockLayout, mapping, text, listOf(ent1))
        assertEquals(null, resultFarRight, "Hovering in margin far to the right must return null")

        // Null layout returns null
        assertEquals(null, findEntityAtPosition(posOverGrom, null as EntityHoverTextLayout?, mapping, text, listOf(ent1)))
    }

    @Test
    fun testImageLink_CursorOutside_HidesSyntaxAndShowsIconAndTitle() {
        val text = "Look at [Dragon Cave](Maps/cave.png) now."
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(0), emptyList(), fantasyFamily)

        // Collapsed to "Look at 🖼 Dragon Cave now."
        assertEquals("Look at 🖼 Dragon Cave now.", annotated.text)

        val linkAnnotations = annotated.getStringAnnotations("image_link", 0, annotated.length)
        assertEquals(1, linkAnnotations.size)
        assertEquals("Maps/cave.png", linkAnnotations[0].item)

        val titleAnnotations = annotated.getStringAnnotations("image_title", 0, annotated.length)
        assertEquals(1, titleAnnotations.size)
        assertEquals("Dragon Cave", titleAnnotations[0].item)

        // Verify OffsetMapping
        // 'L' (orig 0) -> trans 0
        assertEquals(0, mapping.originalToTransformed(0))
        // Click on 'D' in "Dragon" (trans 10) -> maps into the image link range in original text
        val origForDragon = mapping.transformedToOriginal(10)
        assertTrue(origForDragon in 8..35, "Transformed click inside collapsed link should map to original link range")
    }

    @Test
    fun testImageLink_CursorInside_ShowsRawSyntax() {
        val text = "Look at [Dragon Cave](Maps/cave.png) now."
        // Cursor at index 12 (inside "Dragon")
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(12), emptyList(), fantasyFamily)

        // Expanded for editing
        assertEquals("Look at [Dragon Cave](Maps/cave.png) now.", annotated.text)
        assertEquals(12, mapping.originalToTransformed(12))
        assertEquals(12, mapping.transformedToOriginal(12))
    }

    @Test
    fun testImageLink_BangPrefix_AlsoSupported() {
        val text = "![Dragon](Images/dragon.png)"
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(0), emptyList(), fantasyFamily)

        assertEquals("🖼 Dragon", annotated.text)
        val linkAnnotations = annotated.getStringAnnotations("image_link", 0, annotated.length)
        assertEquals("Images/dragon.png", linkAnnotations[0].item)
    }

    @Test
    fun testNonImageLink_RemainsUncollapsed() {
        val text = "Visit [Google](https://google.com) today."
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(0), emptyList(), fantasyFamily)

        // Non-image markdown links should remain plain text without collapsing to 🖼
        assertEquals("Visit [Google](https://google.com) today.", annotated.text)
    }

    @Test
    fun testFindImageLinkAtPosition_HoverDetection() {
        val text = "See [Dragon Cave](Maps/cave.png) here."
        val (annotated, mapping) = parseMarkdown(text, primaryColor, TextRange(0), emptyList(), fantasyFamily)
        assertEquals("See 🖼 Dragon Cave here.", annotated.text)

        val mockLayout = object : EntityHoverTextLayout {
            override val textLength: Int get() = annotated.text.length
            override fun getBoundingBox(offset: Int): Rect {
                val x = offset * 10f
                return Rect(x, 0f, x + 10f, 20f)
            }
        }

        // "🖼 Dragon Cave" starts at transformed index 4 ("See ") -> x from 40f to (4 + 15) * 10 = 190f
        val posOverLink = Offset(60f, 10f)
        val hit = findImageLinkAtPosition(posOverLink, mockLayout, mapping, text, "")
        assertTrue(hit != null, "Hovering over image link text must find the link")
        assertEquals("Dragon Cave", hit.title)
        assertEquals("Maps/cave.png", hit.relativePath)

        // Hovering over "See " (x = 10f) -> null
        val posOverSee = Offset(10f, 10f)
        val hitSee = findImageLinkAtPosition(posOverSee, mockLayout, mapping, text, "")
        assertEquals(null, hitSee, "Hovering over normal text must return null")

        // Hovering far to right -> null
        val posFar = Offset(500f, 10f)
        assertEquals(null, findImageLinkAtPosition(posFar, mockLayout, mapping, text, ""))
    }

    @Test
    fun testResolveImageFile_Normalization() {
        val projectPath = java.io.File(System.getProperty("java.io.tmpdir"), "test_dm_project").absolutePath
        val testFile = java.io.File(projectPath, "Images/sub/test.png")
        testFile.parentFile.mkdirs()
        testFile.writeText("dummy")

        try {
            // Forward slash in relative path
            val resolved1 = resolveImageFile("Images/sub/test.png", projectPath)
            assertTrue(resolved1 != null && resolved1.exists())
            assertEquals(testFile.canonicalPath, resolved1.canonicalPath)

            // Backslash in relative path
            val resolved2 = resolveImageFile("Images\\sub\\test.png", projectPath)
            assertTrue(resolved2 != null && resolved2.exists())
            assertEquals(testFile.canonicalPath, resolved2.canonicalPath)
        } finally {
            testFile.delete()
            testFile.parentFile.delete()
        }
    }

    @Test
    fun testImagePickerHelper_ScanProjectImages() {
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_picker_scan_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val img1 = java.io.File(tempDir, "Images/dragon.png").also { it.parentFile.mkdirs(); it.writeText("img") }
            val img2 = java.io.File(tempDir, "Maps/dungeon.webp").also { it.parentFile.mkdirs(); it.writeText("img") }
            val ignoredGit = java.io.File(tempDir, ".git/ignored.png").also { it.parentFile.mkdirs(); it.writeText("git") }
            val nonImg = java.io.File(tempDir, "Notes/session.md").also { it.parentFile.mkdirs(); it.writeText("md") }

            val scanned = ImagePickerHelper.scanProjectImages(tempDir.absolutePath)
            assertEquals(2, scanned.size)
            assertTrue(scanned.any { it.name == "dragon.png" })
            assertTrue(scanned.any { it.name == "dungeon.webp" })
            assertTrue(scanned.none { it.name == "ignored.png" })
            assertTrue(scanned.none { it.name == "session.md" })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testImagePickerHelper_ToRelativeProjectPath() {
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_picker_rel")
        val file = java.io.File(tempDir, "SubFolder/MyImage.png")

        val rel = ImagePickerHelper.toRelativeProjectPath(file, tempDir.absolutePath)
        assertEquals("SubFolder/MyImage.png", rel)
    }

    @Test
    fun testImagePickerHelper_ConflictResolutionAndImport() {
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_picker_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val externalImg = java.io.File(System.getProperty("java.io.tmpdir"), "external_pic.png").also { it.writeText("data") }

        try {
            // First import -> Images/external_pic.png
            val imported1 = ImagePickerHelper.importExternalImage(externalImg, tempDir.absolutePath)
            assertTrue(imported1 != null && imported1.exists())
            assertEquals("external_pic.png", imported1.name)

            // Second import -> Images/external_pic_1.png
            val imported2 = ImagePickerHelper.importExternalImage(externalImg, tempDir.absolutePath)
            assertTrue(imported2 != null && imported2.exists())
            assertEquals("external_pic_1.png", imported2.name)
        } finally {
            externalImg.delete()
            tempDir.deleteRecursively()
        }
    }
}
