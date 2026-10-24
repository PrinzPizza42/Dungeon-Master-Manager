package de.luca.dungeon_master_manager.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dungeon_master_manager.shared.generated.resources.Res
import dungeon_master_manager.shared.generated.resources.fog_of_war
import dungeon_master_manager.shared.generated.resources.fog_puff_1
import dungeon_master_manager.shared.generated.resources.fog_puff_2
import dungeon_master_manager.shared.generated.resources.fog_puff_3
import dungeon_master_manager.shared.generated.resources.fog_puff_4
import dungeon_master_manager.shared.generated.resources.grimoire_vellum
import dungeon_master_manager.shared.generated.resources.tabletop_dark
import dungeon_master_manager.shared.generated.resources.stone_floor_dark
import dungeon_master_manager.shared.generated.resources.prop_coins
import dungeon_master_manager.shared.generated.resources.prop_compass
import dungeon_master_manager.shared.generated.resources.prop_quill
import dungeon_master_manager.shared.generated.resources.prop_seal
import dungeon_master_manager.shared.generated.resources.prop_fur_pelt
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dungeon_master_manager.shared.generated.resources.dnd_dice
import kotlin.random.Random
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

/**
 * High-performance GPU-accelerated modifier that tiles a seamless texture across any container size,
 * with optional panOffset and zoomScale to pan and zoom the texture seamlessly.
 */
@Composable
fun Modifier.tiledTexture(
    resource: DrawableResource,
    alpha: Float = 1.0f,
    tint: Color? = null,
    blendMode: BlendMode = BlendMode.SrcOver,
    panOffset: Offset = Offset.Zero,
    zoomScale: Float = 1.0f,
    baseTextureScale: Float = 1.0f
): Modifier {
    val imageBitmap = imageResource(resource)
    val brush = remember(imageBitmap) {
        val shader = ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated)
        ShaderBrush(shader)
    }
    return this.then(
        Modifier.drawWithContent {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val safeScale = (zoomScale * baseTextureScale).coerceAtLeast(0.05f)

            withTransform({
                translate(left = panOffset.x, top = panOffset.y)
                scale(scaleX = safeScale, scaleY = safeScale, pivot = Offset(cx, cy))
            }) {
                val invLeft = (0f - cx - panOffset.x) / safeScale + cx
                val invTop = (0f - cy - panOffset.y) / safeScale + cy
                val invRight = (size.width - cx - panOffset.x) / safeScale + cx
                val invBottom = (size.height - cy - panOffset.y) / safeScale + cy

                drawRect(
                    brush = brush,
                    topLeft = Offset(invLeft - 4f, invTop - 4f),
                    size = Size(invRight - invLeft + 8f, invBottom - invTop + 8f),
                    alpha = alpha,
                    colorFilter = tint?.let { ColorFilter.tint(it, blendMode) }
                )
            }
            drawContent()
        }
    )
}

/**
 * An adventurer's war room desk surface featuring authentic dark oak texture and a subtle framing ambient vignette.
 */
@Composable
fun GrimoireDeskSurface(
    modifier: Modifier = Modifier,
    panOffset: Offset = Offset.Zero,
    zoomScale: Float = 1.0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GrimoireColors.Background)
            .tiledTexture(Res.drawable.tabletop_dark, alpha = 0.85f, panOffset = panOffset, zoomScale = zoomScale)
            .drawWithContent {
                // Smooth ambient vignette framing the workspace perimeter naturally without artificial yellow spotlights
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.maxDimension * 0.70f).coerceAtLeast(400f)
                val ambientVignette = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.60f to Color(0x180E0C0A),
                        0.85f to Color(0x50090807),
                        1.0f to Color(0x8A060504)
                    ),
                    center = centerOffset,
                    radius = radius
                )
                drawRect(brush = ambientVignette, size = size)
                drawContent()
            },
        content = content
    )
}

/**
 * The Dungeon Master's ambient chamber floor background, framing the Grand War Table.
 */
@Composable
fun GrimoireAmbientChamber(
    modifier: Modifier = Modifier,
    panOffset: Offset = Offset.Zero,
    zoomScale: Float = 1.0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12100E))
            .tiledTexture(
                resource = Res.drawable.stone_floor_dark,
                alpha = 0.78f,
                panOffset = panOffset,
                zoomScale = zoomScale,
                baseTextureScale = 1.7f
            )
            .drawWithContent {
                drawContent()

                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // 1. Warm Candlelight Pool (Static atmospheric light, 0% idle GPU load)
                val candleRadius = (size.minDimension * 0.78f).coerceAtLeast(350f)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x32FFB74D),
                            0.35f to Color(0x15FF9800),
                            0.70f to Color.Transparent
                        ),
                        center = centerOffset,
                        radius = candleRadius
                    ),
                    blendMode = BlendMode.Screen
                )

                // 2. Chamber Perimeter Vignette
                val roomRadius = (size.maxDimension * 0.85f).coerceAtLeast(500f)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.45f to Color(0x250A0807),
                            0.75f to Color(0x70060504),
                            1.0f to Color(0xC8040303)
                        ),
                        center = centerOffset,
                        radius = roomRadius
                    )
                )
            },
        content = content
    )
}

/**
 * Deep, multi-layered realistic contact drop shadow cast by the heavy oak war table onto the stone floor.
 */
@Composable
fun BoxScope.RealisticTableDropShadow(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    // Direct contact shadow
    Box(
        modifier = modifier
            .matchParentSize()
            .offset(x = 6.dp, y = 14.dp)
            .background(Color(0xCC050403), shape)
            .blur(16.dp)
    )
    // Diffuse ambient occlusion shadow
    Box(
        modifier = modifier
            .matchParentSize()
            .offset(x = 10.dp, y = 26.dp)
            .background(Color(0x88040302), shape)
            .blur(34.dp)
    )
}

/**
 * Rustic medieval bear fur pelt rug resting on the stone floor, peeking out from under a corner of the table.
 */
@Composable
fun TabletopFurPelt(
    modifier: Modifier = Modifier,
    tableWidth: Dp,
    tableHeight: Dp,
    peltWidth: Dp = 520.dp,
    peltHeight: Dp = 570.dp,
    peekX: Dp = 160.dp,
    peekY: Dp = 150.dp,
    rotation: Float = -14f
) {
    val peltBitmap = imageResource(Res.drawable.prop_fur_pelt)
    Image(
        bitmap = peltBitmap,
        contentDescription = "Fur Pelt Rug",
        modifier = modifier
            .offset(x = -peekX, y = tableHeight - peltHeight + peekY)
            .requiredSize(peltWidth, peltHeight)
            .graphicsLayer {
                rotationZ = rotation
                alpha = 0.98f
            }
    )
}

/**
 * An atmospheric ocean of swirling Fog of War filling the entire Map Viewer viewport
 * outside and behind the cartography.
 */
@Composable
fun FogOfWarSurface(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF324050))
            .tiledTexture(Res.drawable.fog_of_war, alpha = 0.95f)
    )
}

/**
 * A chronicle folio card styled as an antique stacked grimoire document on the adventurer's desk.
 */
@Composable
fun GrimoireFolioCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    borderColor: Color = GrimoireColors.BorderBronze,
    borderHoverColor: Color = GrimoireColors.GoldBright,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GrimoireCard(
        modifier = modifier,
        shape = shape,
        borderColor = borderColor,
        borderHoverColor = borderHoverColor,
        containerColor = GrimoireColors.SurfaceCard,
        texture = Res.drawable.grimoire_vellum,
        textureAlpha = 0.35f,
        onClick = onClick,
        content = content
    )
}

/**
 * A dark vellum tome page container for reading and previewing Markdown notes.
 */
@Composable
fun GrimoireVellumTomePage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GrimoireColors.SurfaceLow, RoundedCornerShape(8.dp))
            .tiledTexture(Res.drawable.grimoire_vellum, alpha = 0.28f)
            .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(8.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Organic multi-sprite Fog of War boundary wreath that dissolves sharp map boundaries with
 * deterministic, irregular billowing fog puffs encroaching naturally into the cartography.
 */
@Composable
fun OrganicMapFogWreath(
    modifier: Modifier = Modifier,
    mapSeed: Int = 42,
    encroachRatio: Float = 0.07f
) {
    val puff1 = imageResource(Res.drawable.fog_puff_1)
    val puff2 = imageResource(Res.drawable.fog_puff_2)
    val puff3 = imageResource(Res.drawable.fog_puff_3)
    val puff4 = imageResource(Res.drawable.fog_puff_4)
    val puffList = remember(puff1, puff2, puff3, puff4) { listOf(puff1, puff2, puff3, puff4) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                // 1. Draw the underlying map image first
                drawContent()

                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawWithContent

                val minDim = size.minDimension
                val reach = (minDim * encroachRatio).coerceIn(24f, 65f)
                val basePuffSize = reach * 2.2f

                // Deterministic pseudo-random placement
                val rng = Random(mapSeed)

                // Helper to draw a single puff sprite centered at (cx, cy)
                fun drawPuff(cx: Float, cy: Float, radius: Float, alpha: Float, rotationDeg: Float, spriteIdx: Int) {
                    val sprite = puffList[spriteIdx % puffList.size]
                    val drawSize = (radius * 2f).toInt()
                    val halfSize = radius

                    withTransform({
                        translate(left = cx - halfSize, top = cy - halfSize)
                        rotate(degrees = rotationDeg, pivot = Offset(halfSize, halfSize))
                    }) {
                        drawImage(
                            image = sprite,
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(drawSize, drawSize),
                            alpha = alpha.coerceIn(0f, 1f)
                        )
                    }
                }

                val step = basePuffSize * 0.30f

                // --- TOP EDGE (Far outer wisps, outer halo, perimeter core, subtle inner encroachment) ---
                var curX = -step * 2.0f
                while (curX <= w + step * 2.0f) {
                    // 1. Far outer wisps drifting far onto the wooden table
                    val farOuterY = -reach * (1.4f + rng.nextFloat() * 0.9f)
                    val farOuterR = (basePuffSize * 0.58f) * (0.90f + rng.nextFloat() * 0.40f)
                    drawPuff(curX, farOuterY, farOuterR, 0.32f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 2. Mid outer halo
                    val midOuterY = -reach * (0.65f + rng.nextFloat() * 0.55f)
                    val midOuterR = (basePuffSize * 0.54f) * (0.85f + rng.nextFloat() * 0.35f)
                    drawPuff(curX + (rng.nextFloat() - 0.5f) * step * 0.4f, midOuterY, midOuterR, 0.45f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 3. Boundary core right on the perimeter
                    val coreY = (rng.nextFloat() - 0.5f) * reach * 0.20f
                    val coreR = (basePuffSize * 0.50f) * (0.85f + rng.nextFloat() * 0.30f)
                    drawPuff(curX + (rng.nextFloat() - 0.5f) * step * 0.3f, coreY, coreR, 0.54f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 4. Subtle inner tendrils reaching gently into the map
                    val innerY = reach * (0.20f + rng.nextFloat() * 0.45f)
                    val innerR = (basePuffSize * 0.40f) * (0.75f + rng.nextFloat() * 0.35f)
                    drawPuff(curX + (rng.nextFloat() - 0.5f) * step * 0.4f, innerY, innerR, 0.40f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    curX += step * (0.82f + rng.nextFloat() * 0.28f)
                }

                // --- BOTTOM EDGE ---
                curX = -step * 2.0f
                while (curX <= w + step * 2.0f) {
                    // 1. Far outer wisps
                    val farOuterY = h + reach * (1.4f + rng.nextFloat() * 0.9f)
                    val farOuterR = (basePuffSize * 0.58f) * (0.90f + rng.nextFloat() * 0.40f)
                    drawPuff(curX, farOuterY, farOuterR, 0.32f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 2. Mid outer halo
                    val midOuterY = h + reach * (0.65f + rng.nextFloat() * 0.55f)
                    val midOuterR = (basePuffSize * 0.54f) * (0.85f + rng.nextFloat() * 0.35f)
                    drawPuff(curX + (rng.nextFloat() - 0.5f) * step * 0.4f, midOuterY, midOuterR, 0.45f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 3. Boundary core
                    val coreY = h + (rng.nextFloat() - 0.5f) * reach * 0.20f
                    val coreR = (basePuffSize * 0.50f) * (0.85f + rng.nextFloat() * 0.30f)
                    drawPuff(curX + (rng.nextFloat() - 0.5f) * step * 0.3f, coreY, coreR, 0.54f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 4. Subtle inner tendrils
                    val innerY = h - reach * (0.20f + rng.nextFloat() * 0.45f)
                    val innerR = (basePuffSize * 0.40f) * (0.75f + rng.nextFloat() * 0.35f)
                    drawPuff(curX + (rng.nextFloat() - 0.5f) * step * 0.4f, innerY, innerR, 0.40f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    curX += step * (0.82f + rng.nextFloat() * 0.28f)
                }

                // --- LEFT EDGE ---
                var curY = -step * 2.0f
                while (curY <= h + step * 2.0f) {
                    // 1. Far outer wisps
                    val farOuterX = -reach * (1.4f + rng.nextFloat() * 0.9f)
                    val farOuterR = (basePuffSize * 0.58f) * (0.90f + rng.nextFloat() * 0.40f)
                    drawPuff(farOuterX, curY, farOuterR, 0.32f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 2. Mid outer halo
                    val midOuterX = -reach * (0.65f + rng.nextFloat() * 0.55f)
                    val midOuterR = (basePuffSize * 0.54f) * (0.85f + rng.nextFloat() * 0.35f)
                    drawPuff(midOuterX, curY + (rng.nextFloat() - 0.5f) * step * 0.4f, midOuterR, 0.45f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 3. Boundary core
                    val coreX = (rng.nextFloat() - 0.5f) * reach * 0.20f
                    val coreR = (basePuffSize * 0.50f) * (0.85f + rng.nextFloat() * 0.30f)
                    drawPuff(coreX, curY + (rng.nextFloat() - 0.5f) * step * 0.3f, coreR, 0.54f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 4. Subtle inner tendrils
                    val innerX = reach * (0.20f + rng.nextFloat() * 0.45f)
                    val innerR = (basePuffSize * 0.40f) * (0.75f + rng.nextFloat() * 0.35f)
                    drawPuff(innerX, curY + (rng.nextFloat() - 0.5f) * step * 0.4f, innerR, 0.40f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    curY += step * (0.82f + rng.nextFloat() * 0.28f)
                }

                // --- RIGHT EDGE ---
                curY = -step * 2.0f
                while (curY <= h + step * 2.0f) {
                    // 1. Far outer wisps
                    val farOuterX = w + reach * (1.4f + rng.nextFloat() * 0.9f)
                    val farOuterR = (basePuffSize * 0.58f) * (0.90f + rng.nextFloat() * 0.40f)
                    drawPuff(farOuterX, curY, farOuterR, 0.32f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 2. Mid outer halo
                    val midOuterX = w + reach * (0.65f + rng.nextFloat() * 0.55f)
                    val midOuterR = (basePuffSize * 0.54f) * (0.85f + rng.nextFloat() * 0.35f)
                    drawPuff(midOuterX, curY + (rng.nextFloat() - 0.5f) * step * 0.4f, midOuterR, 0.45f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 3. Boundary core
                    val coreX = w + (rng.nextFloat() - 0.5f) * reach * 0.20f
                    val coreR = (basePuffSize * 0.50f) * (0.85f + rng.nextFloat() * 0.30f)
                    drawPuff(coreX, curY + (rng.nextFloat() - 0.5f) * step * 0.3f, coreR, 0.54f + rng.nextFloat() * 0.18f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    // 4. Subtle inner tendrils
                    val innerX = w - reach * (0.20f + rng.nextFloat() * 0.45f)
                    val innerR = (basePuffSize * 0.40f) * (0.75f + rng.nextFloat() * 0.35f)
                    drawPuff(innerX, curY + (rng.nextFloat() - 0.5f) * step * 0.4f, innerR, 0.40f + rng.nextFloat() * 0.20f, rng.nextFloat() * 360f, rng.nextInt(puffList.size))

                    curY += step * (0.82f + rng.nextFloat() * 0.28f)
                }

                // --- CORNER CLOUD BASTIONS ---
                val corners = listOf(
                    Offset(0f, 0f) to Offset(1f, 1f),
                    Offset(w, 0f) to Offset(-1f, 1f),
                    Offset(0f, h) to Offset(1f, -1f),
                    Offset(w, h) to Offset(-1f, -1f)
                )
                for ((corner, dir) in corners) {
                    for (c in 0 until 10) {
                        val progress = (c.toFloat() / 9f)
                        val distAlong = (progress * 1.8f - 0.9f) * reach
                        val spread = (rng.nextFloat() - 0.5f) * reach * 1.0f
                        val cOffX = dir.x * distAlong + dir.y * spread
                        val cOffY = dir.y * distAlong + dir.x * spread
                        val puffR = (basePuffSize * 0.54f) * (0.85f + rng.nextFloat() * 0.45f)
                        val rot = rng.nextFloat() * 360f
                        val alpha = 0.42f + rng.nextFloat() * 0.22f
                        val sIdx = rng.nextInt(puffList.size)
                        drawPuff(corner.x + cOffX, corner.y + cOffY, puffR, alpha, rot, sIdx)
                    }
                }
            }
    )
}

/**
 * Procedural multi-layer Fog of War boundary mist that fades map edges seamlessly into the surrounding fog of war.
 * Drawn inside the map canvas coordinates so it pans and zooms naturally with the cartography.
 */
@Composable
fun MapBoundaryFogOverlay(
    modifier: Modifier = Modifier,
    mapSeed: Int = 42,
    fogDepthDp: Dp = 100.dp
) {
    OrganicMapFogWreath(modifier = modifier, mapSeed = mapSeed)
}

/**
 * Modular tabletop props positioned on the wooden desk around the map perimeter:
 * - Vintage metallic RPG dice resting on the lower-right wood grain
 * - Antique hand-hammered gold and silver coins along the bottom-left edge
 * - Iridescent raven feather quill and ink pot along the left flank
 *
 * Drawn beneath the map drop shadow and parchment so they sit naturally on the table.
 */
@Composable
fun TabletopPropsDeskLayer(
    modifier: Modifier = Modifier,
    mapWidth: Dp,
    mapHeight: Dp
) {
    val diceBitmap = imageResource(Res.drawable.dnd_dice)
    val coinsBitmap = imageResource(Res.drawable.prop_coins)
    val quillBitmap = imageResource(Res.drawable.prop_quill)

    Box(modifier = modifier) {
        // 1. Vintage Metal RPG Dice (lower-right desk area)
        Image(
            bitmap = diceBitmap,
            contentDescription = "RPG Dice",
            modifier = Modifier
                .offset(x = mapWidth + 24.dp, y = mapHeight - 85.dp)
                .requiredSize(145.dp, 82.dp)
                .graphicsLayer {
                    rotationZ = 12f
                    alpha = 0.95f
                }
        )

        // 2. Hammered Ancient Fantasy Coins (bottom-left desk area)
        Image(
            bitmap = coinsBitmap,
            contentDescription = "Ancient Coins",
            modifier = Modifier
                .offset(x = (-95).dp, y = mapHeight - 90.dp)
                .requiredSize(115.dp, 115.dp)
                .graphicsLayer {
                    rotationZ = 18f
                    alpha = 0.95f
                }
        )

        // 3. Raven Feather Quill & Vintage Inkwell (left flank on desk)
        Image(
            bitmap = quillBitmap,
            contentDescription = "Quill and Ink",
            modifier = Modifier
                .offset(x = (-135).dp, y = (mapHeight * 0.22f) - 60.dp)
                .requiredSize(155.dp, 155.dp)
                .graphicsLayer {
                    rotationZ = -14f
                    alpha = 0.95f
                }
        )
    }
}

/**
 * Modular tabletop accents resting on and holding down the map parchment corners:
 * - Crimson wax seal stamped near the upper margin
 * - Heavy antique brass cartographer divider tool (caliper) pinning the top-right corner
 *
 * Drawn on top of the map image (under fog and markers) so they physically overlap paper edges.
 */
@Composable
fun TabletopPropsMapAccentsLayer(
    modifier: Modifier = Modifier,
    mapWidth: Dp,
    mapHeight: Dp
) {
    val compassBitmap = imageResource(Res.drawable.prop_compass)
    val sealBitmap = imageResource(Res.drawable.prop_seal)

    Box(modifier = modifier) {
        // 4. Crimson Wax Seal (affixed near top edge of the map)
        Image(
            bitmap = sealBitmap,
            contentDescription = "Wax Seal",
            modifier = Modifier
                .offset(x = (mapWidth * 0.28f) - 42.dp, y = (-42).dp)
                .requiredSize(85.dp, 92.dp)
                .graphicsLayer {
                    rotationZ = -7f
                    alpha = 0.96f
                }
        )

        // 5. Brass Cartographer Compass / Caliper (pinning down top-right corner)
        Image(
            bitmap = compassBitmap,
            contentDescription = "Cartographer Compass",
            modifier = Modifier
                .offset(x = mapWidth - 46.dp, y = (-65).dp)
                .requiredSize(88.dp, 194.dp)
                .graphicsLayer {
                    rotationZ = -28f
                    alpha = 0.96f
                }
        )
    }
}

/**
 * Unified tabletop props overlay placing all 5 authentic war-table accessories around the map.
 */
@Composable
fun TabletopPropsOverlay(
    modifier: Modifier = Modifier,
    mapWidth: Dp,
    mapHeight: Dp
) {
    TabletopPropsDeskLayer(modifier = modifier, mapWidth = mapWidth, mapHeight = mapHeight)
    TabletopPropsMapAccentsLayer(modifier = modifier, mapWidth = mapWidth, mapHeight = mapHeight)
}
