package de.luca.dungeon_master_manager.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import dungeon_master_manager.shared.generated.resources.Res
import dungeon_master_manager.shared.generated.resources.grimoire_vellum

/**
 * An ornate divider featuring a hairline bronze rule and an optional centered gold diamond glyph (◆).
 */
@Composable
fun GrimoireDivider(
    modifier: Modifier = Modifier,
    showDiamond: Boolean = false,
    color: Color = GrimoireColors.BorderBronze,
    thickness: Dp = 1.dp
) {
    if (!showDiamond) {
        HorizontalDivider(modifier = modifier, thickness = thickness, color = color)
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = thickness,
                color = color
            )
            Text(
                text = " ◆ ",
                color = GrimoireColors.GoldPrimary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = thickness,
                color = color
            )
        }
    }
}

/**
 * A stylized card container with dark slate leather surface and hairline bronze/gold border.
 */
@Composable
fun GrimoireCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    borderColor: Color? = null,
    borderHoverColor: Color = GrimoireColors.GoldPrimary,
    containerColor: Color = GrimoireColors.SurfaceCard,
    texture: DrawableResource? = null,
    textureAlpha: Float = 0.35f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val currentBorderColor = when {
        isHovered && onClick != null -> borderHoverColor
        borderColor != null -> borderColor
        else -> GrimoireColors.BorderBronze
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .then(
                if (texture != null) {
                    Modifier.tiledTexture(texture, alpha = textureAlpha)
                } else Modifier
            )
            .border(BorderStroke(1.dp, currentBorderColor), shape)
            .then(clickableModifier),
        content = content
    )
}

/**
 * An antique vellum parchment card container with hairline bronze border and subtle leather mottling texture.
 */
@Composable
fun GrimoireFolioCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    borderColor: Color? = null,
    borderHoverColor: Color = GrimoireColors.GoldPrimary,
    containerColor: Color = GrimoireColors.SurfaceCard,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GrimoireCard(
        modifier = modifier,
        shape = shape,
        borderColor = borderColor,
        borderHoverColor = borderHoverColor,
        containerColor = containerColor,
        texture = Res.drawable.grimoire_vellum,
        textureAlpha = 0.35f,
        onClick = onClick,
        content = content
    )
}
