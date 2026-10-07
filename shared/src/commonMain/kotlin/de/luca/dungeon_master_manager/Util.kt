package de.luca.dungeon_master_manager

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.random.Random

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DropdownString(
    label: String?,
    options: List<String>,
    selectedOption: MutableState<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textFieldColors: TextFieldColors = TextFieldDefaults.colors(),
    menuModifier: Modifier = Modifier,
    itemTextColor: Color = Color.Unspecified
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.padding(5.dp)
    ) {
        OutlinedTextField(
            value = selectedOption.value,
            onValueChange = {},
            readOnly = true,
            label =  { if(label != null) Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown öffnen",
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .width(200.dp)
                .onPointerEvent(PointerEventType.Release) {
                    expanded = true
                },
            colors = textFieldColors
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = menuModifier
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option, color = itemTextColor) },
                    onClick = {
                        selectedOption.value = option
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun <T> listPopupStructure(
    listElement: @Composable (item: T) -> Unit,
    list: List<T>,
    showPopup: MutableState<Boolean>,
    popupSize: Pair<Dp, Dp> = Pair(300.dp, 500.dp),
    title: MutableState<String?> = mutableStateOf(null),
    searchStringSelector: ((T) -> String)? = null
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(list, searchQuery) {
        if (searchQuery.isBlank() || searchStringSelector == null) {
            list
        } else {
            list.filter { item ->
                searchStringSelector(item).contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Popup(
        onDismissRequest = { showPopup.value = false },
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true)
    ) {
        Column(
            Modifier
                .size(popupSize.first, popupSize.second)
                .shadow(10.dp, RoundedCornerShape(10.dp))
                .background(Color.LightGray, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Row(
                Modifier.height(45.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (title.value != null) {
                    Box(Modifier.weight(1f))
                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(title.value!!)
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(
                        onClick = { showPopup.value = false },
                        content = { Icon(Icons.Default.Close, "Close") }
                    )
                }
            }
            if (searchStringSelector != null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    placeholder = { Text("Suchen...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Suche") },
                    singleLine = true
                )
            }

            if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (list.isEmpty()) "Keine Elemente gefunden" else "Keine Treffer für die Suche")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredList) { currentItem ->
                        listElement(currentItem)
                    }
                }
            }
        }
    }
}

fun Color.Companion.random(): Color {
    return Color.hsv(
        hue = Random.nextFloat() * 360f,
        saturation = 1f,
        value = 1f
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun colorElement(
    currentColor: Color,
    showPopup: MutableState<Boolean> = remember { mutableStateOf(false) },
    onClick: (Color) -> Unit
) {
    MaterialTheme {
        val color = remember { mutableStateOf(currentColor) }

        var isHovered by remember { mutableStateOf(false) }
        var scale by mutableStateOf(if(isHovered) 1.2f else 1f)
        var shadow by  mutableStateOf(if(isHovered) 5.dp else 2.dp)

        val animatedScale by animateFloatAsState(
            targetValue = scale,
            animationSpec = tween(durationMillis = 250)
        )

        val animatedShadow by animateDpAsState(
            targetValue = shadow,
            animationSpec = tween(durationMillis = 250)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .padding(10.dp)
                    .scale(animatedScale)
                    .shadow(animatedShadow, CircleShape)
                    .background(color.value, CircleShape)
                    .size(30.dp)
                    .onClick { showPopup.value = !showPopup.value }
                    .onPointerEvent(eventType = PointerEventType.Enter) { isHovered = true }
                    .onPointerEvent(eventType = PointerEventType.Exit) { isHovered = false},
            ) {
                if(showPopup.value) {
                    Popup(
                        onDismissRequest = { showPopup.value = false }
                    ) {
                        Column(
                            Modifier
                                .shadow(10.dp, RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
                                .size(420.dp, 365.dp)
                                .padding(5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Color Picker", style = MaterialTheme.typography.titleLargeEmphasized)
                                Box(Modifier.weight(1f))
                                IconButton(
                                    onClick = { showPopup.value = false },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Schließen"
                                        )
                                    },
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Row {
                                // Color picker
                                HsvColorPicker(color)

                                // Basic colors
                                Column(
                                    Modifier.size(150.dp, 150.dp)
                                ) {
                                    Row {
                                        standardColorElement(color, Color.White)
                                        standardColorElement(color, Color.Black)
                                        standardColorElement(color, Color.Gray)
                                    }
                                    Row {
                                        standardColorElement(color, Color.Red)
                                        standardColorElement(color, Color.Green)
                                        standardColorElement(color, Color.Blue)
                                    }
                                    Row {
                                        standardColorElement(color, Color.Yellow)
                                        standardColorElement(color, Color.Cyan)
                                        standardColorElement(color, Color.Magenta)
                                    }
                                }

                                // Color element
                                Box(Modifier
                                    .shadow(2.dp, RoundedCornerShape(10.dp))
                                    .background(color.value, RoundedCornerShape(10.dp))
                                    .size(100.dp, 150.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        showPopup.value = false
                                        onClick(color.value)
                                    },
                                    content = { Text("Übernehmen") },
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Text("Click on color to change it")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun standardColorElement(color: MutableState<Color>, displayColor: Color) {
    var isHovered by remember { mutableStateOf(false) }

    var scale by mutableStateOf(if(isHovered) 1.2f else 1f)
    var shadow by  mutableStateOf(if(isHovered) 5.dp else 2.dp)

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 250)
    )

    val animatedShadow by animateDpAsState(
        targetValue = shadow,
        animationSpec = tween(durationMillis = 250)
    )

    Box(Modifier
        .padding(5.dp)
        .scale(animatedScale)
        .shadow(animatedShadow, CircleShape)
        .background(displayColor, CircleShape)
        .onClick { color.value = displayColor }
        .size(30.dp)
        .onPointerEvent(eventType = PointerEventType.Enter) { isHovered = true }
        .onPointerEvent(eventType = PointerEventType.Exit) { isHovered = false}
    )
}

@Composable
fun HsvColorPicker(color: MutableState<Color>) {
    val initialHsv = remember { color.value.toHsv() }

    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }

    LaunchedEffect(color.value) {
        val currentSliderColor = Color.hsv(hue, saturation, value)
        if (currentSliderColor != color.value) {
            val hsv = color.value.toHsv()
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        }
    }
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Hue Slider
            VerticalSliderColumn(
                label = "H",
                value = hue,
                valueRange = 0f..360f,
                onValueChange = {
                    hue = it
                    color.value = Color.hsv(hue = hue, saturation = saturation, value = value)
                }
            )

            // Saturation Slider
            VerticalSliderColumn(
                label = "S",
                value = saturation,
                valueRange = 0f..1f,
                onValueChange = {
                    saturation = it
                    color.value = Color.hsv(hue = hue, saturation = saturation, value = value)
                }
            )

            // Value Slider
            VerticalSliderColumn(
                label = "V",
                value = value,
                valueRange = 0f..1f,
                onValueChange = {
                    value = it
                    color.value = Color.hsv(hue = hue, saturation = saturation, value = value)
                }
            )
        }
    }
}

@Composable
fun VerticalSliderColumn(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val sliderHeight = 150.dp
    val sliderWidth = 40.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(sliderWidth)
                .height(sliderHeight),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .requiredWidth(sliderHeight)
                    .rotate(-90f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label)
        Text(if(value <= 1) String.format("%.2f", value) else String.format("%.0f", value))
    }
}

fun Color.toHsv(): FloatArray {
    val r = this.red
    val g = this.green
    val b = this.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    var h = 0f
    if (delta != 0f) {
        h = when (max) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            b -> 60f * (((r - g) / delta) + 4f)
            else -> 0f
        }
        if (h < 0f) h += 360f
    }

    val s = if (max == 0f) 0f else delta / max
    val v = max

    return floatArrayOf(h, s, v)
}