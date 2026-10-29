package de.luca.dungeon_master_manager

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import de.luca.dungeon_master_manager.ui.VerticalTextLayout
import de.luca.dungeon_master_manager.ui.calculateVerticalMove
import de.luca.dungeon_master_manager.ui.expandMarkdownTokenSelection
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
            notesFilePath = null
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
            notesFilePath = null
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
            notesFilePath = null
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
        val mockEnt1 = Entity("e1", "Hero", "NPC", null, null)
        val mockEnt2 = Entity("e2", "Villain", "NPC", null, null)
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

        val mockEnt1 = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null)
        val mockEnt2 = Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null)
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
        val mockEnt = Entity("goblin", "Grom", "NPC", null, null)
        // Cursor at offset 7 (the position of '@')
        val (annotated, _) = parseMarkdown(text, primaryColor, TextRange(7), listOf(mockEnt), fantasyFamily)
        // At the boundary, entity stays collapsed as "Grom" so arrow navigation past it doesn't explode
        assertEquals("Before Grom after", annotated.text)
    }

    @Test
    fun testEntityClick_ExpandsCorrectly() {
        val text = "Before @[goblin] after"
        val mockEnt = Entity("goblin", "Grom", "NPC", null, null)
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
        val ent1 = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null)
        val ent2 = Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null)
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
        val ent1 = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null)
        val ent2 = Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null)
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
        val ent = Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "NPC", "NPC", null, null)
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
            Entity("a904acec-6288-433d-84b3-2b4060eb23a2", "Test NPC 1", "NPC", null, null),
            Entity("d2d19c17-f139-4473-89cd-d6b4af1209d3", "Jerry", "NPC", null, null)
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
        val mockEnt = Entity("king-id", "The King", "NPC", null, null)
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
}
