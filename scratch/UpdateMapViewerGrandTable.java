import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpdateMapViewerGrandTable {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\Luca\\Desktop\\IdeaProjects\\Dungeon-Master-Manager\\shared\\src\\commonMain\\kotlin\\de\\luca\\dungeon_master_manager\\ui\\MapViewer.kt");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        // Normalize line endings to \n for replacement
        boolean hadCr = content.contains("\r");
        String normalized = content.replace("\r\n", "\n");

        // 1. Ambient chamber background
        String oldBg = "        // Fog of War filling the entire map viewer outside the map image\n" +
                       "        GrimoireDeskSurface(\n" +
                       "            modifier = Modifier.fillMaxSize(),\n" +
                       "            panOffset = androidx.compose.ui.geometry.Offset(animatedOffsetX.value, animatedOffsetY.value),\n" +
                       "            zoomScale = animatedScale.value\n" +
                       "        )";
        String newBg = "        // The Dungeon Master's ambient chamber room floor surrounding the war table\n" +
                       "        GrimoireAmbientChamber(modifier = Modifier.fillMaxSize())";

        if (!normalized.contains(oldBg)) {
            System.err.println("Could not find oldBg!");
        } else {
            normalized = normalized.replace(oldBg, newBg);
            System.out.println("Replaced oldBg successfully.");
        }

        // 2. Main transformed viewport and table container
        String oldBlockStart = "        if (imageBitmap != null) {\n" +
                               "            var showMarkerDialog by remember { mutableStateOf<MapMarker?>(null) }\n" +
                               "            \n" +
                               "            Box(\n" +
                               "                modifier = Modifier\n" +
                               "                    .fillMaxSize()\n" +
                               "                    .clipToBounds()\n" +
                               "                    .pointerInput(Unit) {\n" +
                               "                        detectTransformGestures { centroid, pan, zoom, _ ->\n" +
                               "                            if (!addPinMode && viewportSize.width > 0) {\n" +
                               "                                val oldScale = scale\n" +
                               "                                val newScale = (scale * zoom).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
                               "                                val actualZoom = newScale / oldScale\n" +
                               "\n" +
                               "                                val cx = viewportSize.width / 2f\n" +
                               "                                val cy = viewportSize.height / 2f\n" +
                               "\n" +
                               "                                scale = newScale\n" +
                               "                                // Pan the map plus compensate for the zoom shifting\n" +
                               "                                offsetX = offsetX + pan.x + (centroid.x - cx - offsetX) * (1 - actualZoom)\n" +
                               "                                offsetY = offsetY + pan.y + (centroid.y - cy - offsetY) * (1 - actualZoom)\n" +
                               "                            }\n" +
                               "                        }\n" +
                               "                    }\n" +
                               "                    .pointerInput(Unit) {\n" +
                               "                        awaitPointerEventScope {\n" +
                               "                            while (true) {\n" +
                               "                                val event = awaitPointerEvent()\n" +
                               "                                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {\n" +
                               "                                    if (!addPinMode && viewportSize.width > 0) {\n" +
                               "                                        // Extract the cursor location to use as the focal point\n" +
                               "                                        val cursorPosition = event.changes.first().position\n" +
                               "                                        val delta = event.changes.first().scrollDelta.y\n" +
                               "\n" +
                               "                                        val oldScale = scale\n" +
                               "                                        val newScale = (scale * if (delta > 0) 0.9f else 1.1f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
                               "                                        val actualZoom = newScale / oldScale\n" +
                               "\n" +
                               "                                        val cx = viewportSize.width / 2f\n" +
                               "                                        val cy = viewportSize.height / 2f\n" +
                               "\n" +
                               "                                        scale = newScale\n" +
                               "                                        // Shift the map to compensate for the zoom shifting\n" +
                               "                                        offsetX = offsetX + (cursorPosition.x - cx - offsetX) * (1 - actualZoom)\n" +
                               "                                        offsetY = offsetY + (cursorPosition.y - cy - offsetY) * (1 - actualZoom)\n" +
                               "\n" +
                               "                                        event.changes.forEach { it.consume() }\n" +
                               "                                    }\n" +
                               "                                }\n" +
                               "                            }\n" +
                               "                        }\n" +
                               "                    }\n" +
                               "            ) {\n" +
                               "                val density = androidx.compose.ui.platform.LocalDensity.current\n" +
                               "                val imageWidthDp = with(density) { imageBitmap!!.width.toDp() }\n" +
                               "                val imageHeightDp = with(density) { imageBitmap!!.height.toDp() }\n" +
                               "\n" +
                               "                Box(\n" +
                               "                    modifier = Modifier\n" +
                               "                        .align(Alignment.Center)\n" +
                               "                        .graphicsLayer(\n" +
                               "                            scaleX = animatedScale.value,\n" +
                               "                            scaleY = animatedScale.value,\n" +
                               "                            translationX = animatedOffsetX.value,\n" +
                               "                            translationY = animatedOffsetY.value\n" +
                               "                        )\n" +
                               "                        .requiredSize(imageWidthDp, imageHeightDp)\n" +
                               "                ) {\n" +
                               "                    // 0. Tabletop Props on the wood desk (Dice, Coins, Quill)\n" +
                               "                    TabletopPropsDeskLayer(\n" +
                               "                        mapWidth = imageWidthDp,\n" +
                               "                        mapHeight = imageHeightDp\n" +
                               "                    )";

        String newBlockStart = "        if (imageBitmap != null) {\n" +
                               "            var showMarkerDialog by remember { mutableStateOf<MapMarker?>(null) }\n" +
                               "\n" +
                               "            val density = androidx.compose.ui.platform.LocalDensity.current\n" +
                               "            val imgW = imageBitmap!!.width.toFloat()\n" +
                               "            val imgH = imageBitmap!!.height.toFloat()\n" +
                               "\n" +
                               "            // Viewport-normalized base display size for the map:\n" +
                               "            // At zoom = 1.0f, the map occupies ~72% of the viewport dimensions, maintaining its native aspect ratio.\n" +
                               "            val vw = if (viewportSize.width > 0) viewportSize.width.toFloat() else 1920f\n" +
                               "            val vh = if (viewportSize.height > 0) viewportSize.height.toFloat() else 1080f\n" +
                               "            val targetMapW = vw * 0.72f\n" +
                               "            val targetMapH = vh * 0.72f\n" +
                               "            val fitScale = minOf(targetMapW / imgW, targetMapH / imgH)\n" +
                               "            val baseMapWidth = imgW * fitScale\n" +
                               "            val baseMapHeight = imgH * fitScale\n" +
                               "\n" +
                               "            val baseMapWidthDp = with(density) { baseMapWidth.toDp() }\n" +
                               "            val baseMapHeightDp = with(density) { baseMapHeight.toDp() }\n" +
                               "\n" +
                               "            // Grand War Table dimensions: Generously frames the map with ample desk margins\n" +
                               "            val marginX = maxOf(baseMapWidth * 0.30f, with(density) { 180.dp.toPx() })\n" +
                               "            val marginY = maxOf(baseMapHeight * 0.30f, with(density) { 140.dp.toPx() })\n" +
                               "            val tableWidth = baseMapWidth + marginX * 2f\n" +
                               "            val tableHeight = baseMapHeight + marginY * 2f\n" +
                               "            val tableWidthDp = with(density) { tableWidth.toDp() }\n" +
                               "            val tableHeightDp = with(density) { tableHeight.toDp() }\n" +
                               "\n" +
                               "            Box(\n" +
                               "                modifier = Modifier\n" +
                               "                    .fillMaxSize()\n" +
                               "                    .clipToBounds()\n" +
                               "                    .pointerInput(Unit) {\n" +
                               "                        detectTransformGestures { centroid, pan, zoom, _ ->\n" +
                               "                            if (!addPinMode && viewportSize.width > 0) {\n" +
                               "                                val oldScale = scale\n" +
                               "                                val newScale = (scale * zoom).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
                               "                                val actualZoom = newScale / oldScale\n" +
                               "\n" +
                               "                                val cx = viewportSize.width / 2f\n" +
                               "                                val cy = viewportSize.height / 2f\n" +
                               "\n" +
                               "                                scale = newScale\n" +
                               "                                var nextX = offsetX + pan.x + (centroid.x - cx - offsetX) * (1 - actualZoom)\n" +
                               "                                var nextY = offsetY + pan.y + (centroid.y - cy - offsetY) * (1 - actualZoom)\n" +
                               "\n" +
                               "                                val maxPanX = (viewportSize.width / 2f) + (tableWidth * newScale / 2f) - 100f\n" +
                               "                                val maxPanY = (viewportSize.height / 2f) + (tableHeight * newScale / 2f) - 100f\n" +
                               "                                if (maxPanX > 0) nextX = nextX.coerceIn(-maxPanX, maxPanX)\n" +
                               "                                if (maxPanY > 0) nextY = nextY.coerceIn(-maxPanY, maxPanY)\n" +
                               "\n" +
                               "                                offsetX = nextX\n" +
                               "                                offsetY = nextY\n" +
                               "                            }\n" +
                               "                        }\n" +
                               "                    }\n" +
                               "                    .pointerInput(Unit) {\n" +
                               "                        awaitPointerEventScope {\n" +
                               "                            while (true) {\n" +
                               "                                val event = awaitPointerEvent()\n" +
                               "                                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {\n" +
                               "                                    if (!addPinMode && viewportSize.width > 0) {\n" +
                               "                                        // Extract the cursor location to use as the focal point\n" +
                               "                                        val cursorPosition = event.changes.first().position\n" +
                               "                                        val delta = event.changes.first().scrollDelta.y\n" +
                               "\n" +
                               "                                        val oldScale = scale\n" +
                               "                                        val newScale = (scale * if (delta > 0) 0.9f else 1.1f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)\n" +
                               "                                        val actualZoom = newScale / oldScale\n" +
                               "\n" +
                               "                                        val cx = viewportSize.width / 2f\n" +
                               "                                        val cy = viewportSize.height / 2f\n" +
                               "\n" +
                               "                                        scale = newScale\n" +
                               "                                        var nextX = offsetX + (cursorPosition.x - cx - offsetX) * (1 - actualZoom)\n" +
                               "                                        var nextY = offsetY + (cursorPosition.y - cy - offsetY) * (1 - actualZoom)\n" +
                               "\n" +
                               "                                        val maxPanX = (viewportSize.width / 2f) + (tableWidth * newScale / 2f) - 100f\n" +
                               "                                        val maxPanY = (viewportSize.height / 2f) + (tableHeight * newScale / 2f) - 100f\n" +
                               "                                        if (maxPanX > 0) nextX = nextX.coerceIn(-maxPanX, maxPanX)\n" +
                               "                                        if (maxPanY > 0) nextY = nextY.coerceIn(-maxPanY, maxPanY)\n" +
                               "\n" +
                               "                                        offsetX = nextX\n" +
                               "                                        offsetY = nextY\n" +
                               "\n" +
                               "                                        event.changes.forEach { it.consume() }\n" +
                               "                                    }\n" +
                               "                                }\n" +
                               "                            }\n" +
                               "                        }\n" +
                               "                    }\n" +
                               "            ) {\n" +
                               "                // Grand War Table container\n" +
                               "                Box(\n" +
                               "                    modifier = Modifier\n" +
                               "                        .align(Alignment.Center)\n" +
                               "                        .graphicsLayer(\n" +
                               "                            scaleX = animatedScale.value,\n" +
                               "                            scaleY = animatedScale.value,\n" +
                               "                            translationX = animatedOffsetX.value,\n" +
                               "                            translationY = animatedOffsetY.value\n" +
                               "                        )\n" +
                               "                        .size(tableWidthDp, tableHeightDp)\n" +
                               "                ) {\n" +
                               "                    // 1. Table Elevation Shadow on Chamber Floor\n" +
                               "                    Box(\n" +
                               "                        modifier = Modifier\n" +
                               "                            .fillMaxSize()\n" +
                               "                            .graphicsLayer {\n" +
                               "                                shadowElevation = 28.dp.toPx()\n" +
                               "                                shape = RoundedCornerShape(16.dp)\n" +
                               "                                clip = false\n" +
                               "                            }\n" +
                               "                    )\n" +
                               "\n" +
                               "                    // 2. Grand War Table Surface (Non-repeating Hero Asset)\n" +
                               "                    Image(\n" +
                               "                        painter = painterResource(Res.drawable.tabletop_hero),\n" +
                               "                        contentDescription = \"Grand War Table\",\n" +
                               "                        contentScale = ContentScale.Crop,\n" +
                               "                        modifier = Modifier\n" +
                               "                            .fillMaxSize()\n" +
                               "                            .clip(RoundedCornerShape(14.dp))\n" +
                               "                            .border(\n" +
                               "                                width = 2.dp,\n" +
                               "                                brush = Brush.linearGradient(\n" +
                               "                                    listOf(Color(0x55C6A052), Color(0x203E2723), Color(0x608D6E63))\n" +
                               "                                ),\n" +
                               "                                shape = RoundedCornerShape(14.dp)\n" +
                               "                            )\n" +
                               "                    )\n" +
                               "\n" +
                               "                    // 3. Map Parchment & Accents Container (Centered on the War Table)\n" +
                               "                    Box(\n" +
                               "                        modifier = Modifier\n" +
                               "                            .align(Alignment.Center)\n" +
                               "                            .size(baseMapWidthDp, baseMapHeightDp)\n" +
                               "                    ) {\n" +
                               "                        // 0. Tabletop Props on the wood desk (Dice, Coins, Quill)\n" +
                               "                        TabletopPropsDeskLayer(\n" +
                               "                            mapWidth = baseMapWidthDp,\n" +
                               "                            mapHeight = baseMapHeightDp\n" +
                               "                        )";

        if (!normalized.contains(oldBlockStart)) {
            System.err.println("Could not find oldBlockStart!");
        } else {
            normalized = normalized.replace(oldBlockStart, newBlockStart);
            System.out.println("Replaced oldBlockStart successfully.");
        }

        // 3. Pin placement coordinates
        String oldPinCoords = "val x = (position.x / imageBitmap!!.width).coerceIn(0f, 1f)\n" +
                              "                                        val y = (position.y / imageBitmap!!.height).coerceIn(0f, 1f)";
        String newPinCoords = "val x = (position.x / baseMapWidth).coerceIn(0f, 1f)\n" +
                              "                                        val y = (position.y / baseMapHeight).coerceIn(0f, 1f)";
        if (!normalized.contains(oldPinCoords)) {
            System.err.println("Could not find oldPinCoords!");
        } else {
            normalized = normalized.replace(oldPinCoords, newPinCoords);
            System.out.println("Replaced oldPinCoords successfully.");
        }

        // 4. Props map accents
        String oldAccents = "TabletopPropsMapAccentsLayer(\n" +
                            "                        mapWidth = imageWidthDp,\n" +
                            "                        mapHeight = imageHeightDp\n" +
                            "                    )";
        String newAccents = "TabletopPropsMapAccentsLayer(\n" +
                            "                            mapWidth = baseMapWidthDp,\n" +
                            "                            mapHeight = baseMapHeightDp\n" +
                            "                        )";
        if (!normalized.contains(oldAccents)) {
            System.err.println("Could not find oldAccents!");
        } else {
            normalized = normalized.replace(oldAccents, newAccents);
            System.out.println("Replaced oldAccents successfully.");
        }

        // 5. Marker pixel coordinates
        String oldMarkerCoords = "val pxX = marker.x * imageBitmap!!.width\n" +
                                 "                        val pxY = marker.y * imageBitmap!!.height";
        String newMarkerCoords = "val pxX = marker.x * baseMapWidth\n" +
                                 "                        val pxY = marker.y * baseMapHeight";
        if (!normalized.contains(oldMarkerCoords)) {
            System.err.println("Could not find oldMarkerCoords!");
        } else {
            normalized = normalized.replace(oldMarkerCoords, newMarkerCoords);
            System.out.println("Replaced oldMarkerCoords successfully.");
        }

        // 6. Close the 3 containers at the end of marker rendering
        String oldClose = "                                    }\n" +
                          "                                }\n" +
                          "                            }\n" +
                          "                        }\n" +
                          "                    }\n" +
                          "                }\n" +
                          "            }\n" +
                          "            \n" +
                          "            if (showMarkerDialog != null) {";

        String newClose = "                                    }\n" +
                          "                                }\n" +
                          "                            }\n" +
                          "                        }\n" +
                          "                    }\n" +
                          "                }\n" +
                          "            }\n" +
                          "        }\n" +
                          "            \n" +
                          "            if (showMarkerDialog != null) {";

        if (!normalized.contains(oldClose)) {
            System.err.println("Could not find oldClose!");
        } else {
            normalized = normalized.replace(oldClose, newClose);
            System.out.println("Replaced oldClose successfully.");
        }

        if (hadCr) {
            normalized = normalized.replace("\n", "\r\n");
        }

        Files.writeString(file.toPath(), normalized, StandardCharsets.UTF_8);
        System.out.println("MapViewer.kt updated successfully!");
    }
}
