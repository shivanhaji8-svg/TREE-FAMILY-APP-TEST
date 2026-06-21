package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamilyMember
import com.example.ui.theme.KurdPrimary
import com.example.ui.theme.KurdDarkBlue
import com.example.ui.theme.KurdSecondary
import com.example.ui.theme.KurdText

@Composable
fun TreeView(
    members: List<FamilyMember>,
    selectedMember: FamilyMember?,
    onSelectMember: (FamilyMember) -> Unit,
    onAddMemberClick: (FamilyMember) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
    onDeleteSpouse: (FamilyMember) -> Unit,
    onDeleteDaughter: (FamilyMember, Int) -> Unit,
    onToggleExpanded: (FamilyMember) -> Unit,
    showMaleOnly: Boolean,
    onToggleMaleOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (members.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No descendants found. Tap '+' to create the patriarch ancestor.",
                color = Color(0xFF64748B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    // Node Dimension Settings - Optimized for Absolute Compact Layout (Single-Screen Harmony)
    val nodeWidth = 120.dp
    val nodeHeight = 70.dp
    val horizontalSpacing = 60.dp
    val verticalSpacing = 80.dp

    // Filter out Female nodes from layout calculations if Male Only is toggled
    val activeMembers = remember(members, showMaleOnly) {
        if (showMaleOnly) members.filter { it.gender != "Female" } else members
    }

    // Calculate layout coordinates
    val positions = remember(activeMembers, showMaleOnly) {
        calculateLayoutPositions(
            members = activeMembers,
            nodeWidth = nodeWidth,
            nodeHeight = nodeHeight,
            horizontalSpacing = horizontalSpacing,
            verticalSpacing = verticalSpacing,
            showMaleOnly = showMaleOnly
        )
    }

    // Calculate Canvas container bounds based on layout offsets
    val canvasBounds = remember(positions) {
        var maxX = 1600.dp
        var maxY = 1200.dp
        positions.forEach { (_, offset) ->
            if (offset.x + nodeWidth + 200.dp > maxX) {
                maxX = offset.x + nodeWidth + 200.dp
            }
            if (offset.y + nodeHeight + 180.dp > maxY) {
                maxY = offset.y + nodeHeight + 180.dp
            }
        }
        DpOffset(maxX, maxY)
    }

    // Zoom and pan navigation state - Defaulting to 70% zoom
    var zoomScale by remember { mutableStateOf(0.7f) }
    var panOffset by remember { mutableStateOf(Offset(250f, 180f)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9)) // Clean backdrop canvas
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.2f, 2.5f)
                    panOffset += pan
                }
            }
    ) {
        // Infinite Interactive Grid Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            // Draw schematic background pattern and connector lines
            Canvas(
                modifier = Modifier.size(width = canvasBounds.x, height = canvasBounds.y)
            ) {
                // Draw aesthetic layout grid dots
                val dotRadius = 1.2.dp.toPx()
                val dotSpacing = 28.dp.toPx()
                val gridColor = Color(0xFFCBD5E1).copy(alpha = 0.45f)
                var gx = 0f
                while (gx < size.width) {
                    var gy = 0f
                    while (gy < size.height) {
                        drawCircle(color = gridColor, radius = dotRadius, center = Offset(gx, gy))
                        gy += dotSpacing
                    }
                    gx += dotSpacing
                }

                // Drawing lines between couples and children
                val dashPattern = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                val strokeW = 2.dp.toPx()
                val lineColor = Color(0xFF94A3B8)

                activeMembers.forEach { m ->
                    val mKey = "member_${m.id}"
                    val mOff = positions[mKey] ?: return@forEach

                    val mx = (mOff.x + nodeWidth / 2).toPx()
                    val my = (mOff.y + nodeHeight / 2).toPx()

                    // Spouse horizontal connection details
                    val hasSpouse = m.spouse.isNotEmpty() && !showMaleOnly
                    val sKey = "spouse_${m.id}"
                    val sOff = if (!showMaleOnly) positions[sKey] else null

                    val midParentX: Float
                    val midParentY: Float

                    if (hasSpouse && sOff != null) {
                        val sx = sOff.x.toPx()
                        val sy = (sOff.y + nodeHeight / 2).toPx()

                        // Solid romantic Rose pink line for spouses side-by-side
                        drawLine(
                            color = Color(0xFFF43F5E),
                            start = Offset(mOff.x.toPx() + nodeWidth.toPx(), my),
                            end = Offset(sx, sy),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Midpoint is the exact center of spouses horizontal bridge
                        midParentX = (mOff.x.toPx() + nodeWidth.toPx() + sx) / 2f
                        midParentY = my
                    } else {
                        // Center bottom of single node
                        midParentX = mx
                        midParentY = mOff.y.toPx() + nodeHeight.toPx()
                    }

                    // Map all offspring (Sons and Daughters) only if parent is expanded
                    val sons = if (m.isExpanded) {
                        activeMembers.filter { it.parentId == m.id }
                    } else {
                        emptyList()
                    }
                    val daughters = if (m.isExpanded && m.daughters.isNotEmpty() && !showMaleOnly) {
                        m.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }

                    val totalChildrenCount = sons.size + daughters.size
                    if (totalChildrenCount > 0) {
                        // Trunk line splits at mid-level of row gap
                        val midY = (mOff.y + nodeHeight + verticalSpacing / 2).toPx()

                        // Draw trunk line coming down from parent midpoint
                        drawLine(
                            color = lineColor,
                            start = Offset(midParentX, midParentY),
                            end = Offset(midParentX, midY),
                            strokeWidth = strokeW,
                            pathEffect = dashPattern
                        )

                        // Connect sons
                        sons.forEach { son ->
                            val sonKey = "member_${son.id}"
                            val sonOff = positions[sonKey]
                            if (sonOff != null) {
                                val cx = (sonOff.x + nodeWidth / 2).toPx()
                                val cy = sonOff.y.toPx()

                                // Branch horizontal pathway
                                drawLine(
                                    color = lineColor,
                                    start = Offset(midParentX, midY),
                                    end = Offset(cx, midY),
                                    strokeWidth = strokeW,
                                    pathEffect = dashPattern
                                )
                                // Dropdown to son top port
                                drawLine(
                                    color = lineColor,
                                    start = Offset(cx, midY),
                                    end = Offset(cx, cy),
                                    strokeWidth = strokeW,
                                    pathEffect = dashPattern
                                )
                            }
                        }

                        // Connect daughters
                        daughters.forEachIndexed { dIdx, _ ->
                            val dKey = "daughter_${m.id}_$dIdx"
                            val dOff = positions[dKey]
                            if (dOff != null) {
                                val cx = (dOff.x + nodeWidth / 2).toPx()
                                val cy = dOff.y.toPx()

                                // Branch horizontal pathway
                                drawLine(
                                    color = lineColor,
                                    start = Offset(midParentX, midY),
                                    end = Offset(cx, midY),
                                    strokeWidth = strokeW,
                                    pathEffect = dashPattern
                                )
                                // Dropdown to daughter top port
                                drawLine(
                                    color = lineColor,
                                    start = Offset(cx, midY),
                                    end = Offset(cx, cy),
                                    strokeWidth = strokeW,
                                    pathEffect = dashPattern
                                )
                            }
                        }
                    }
                }
            }

            // Lay out interactive nodes
            positions.forEach { (key, offset) ->
                if (key.startsWith("member_")) {
                    val mId = key.substringAfter("member_").toLongOrNull() ?: return@forEach
                    val member = activeMembers.firstOrNull { it.id == mId } ?: return@forEach
                    val isSelected = selectedMember?.id == member.id

                    // Real Member Node (Male / Patrilineal Trunk)
                    Box(
                        modifier = Modifier
                            .offset(x = offset.x, y = offset.y)
                            .size(nodeWidth, nodeHeight + 16.dp)
                    ) {
                        val bColor = if (isSelected) KurdPrimary else Color(0xFFE2E8F0)
                        val bStroke = if (isSelected) 2.dp else 1.dp
                        val sElev = if (isSelected) 6.dp else 1.5.dp

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = sElev),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(nodeWidth)
                                .height(nodeHeight)
                                .border(bStroke, bColor, RoundedCornerShape(12.dp))
                                .clickable { onSelectMember(member) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "#${String.format("%04d", member.id)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = member.name,
                                        color = KurdText,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (member.birthYear.isNotEmpty()) {
                                        Text(
                                            text = "Born ${member.birthYear}",
                                            color = Color(0xFF64748B),
                                            fontSize = 7.5.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = if (member.parentId == null) Color(0xFF24B1B1).copy(alpha = 0.10f) else Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = if (member.parentId == null) "FOUNDER" else "GEN ${member.generation}",
                                        color = if (member.parentId == null) Color(0xFF24B1B1) else Color(0xFF3B82F6),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Center Add Node Trigger Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-6).dp)
                                .size(24.dp)
                                .shadow(3.dp, CircleShape)
                                .background(KurdPrimary, CircleShape)
                                .clip(CircleShape)
                                .clickable { onAddMemberClick(member) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick add relative",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Individual Node Collapse/Expand Button (Visible only on nodes with offspring)
                        val hasDescendants = activeMembers.any { it.parentId == member.id } || (member.daughters.isNotEmpty() && !showMaleOnly)
                        if (hasDescendants) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(20.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { onToggleExpanded(member) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (member.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle branch expansion",
                                    tint = KurdPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(Color.Red, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { onDeleteMember(member) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete member",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                } else if (key.startsWith("spouse_") && !showMaleOnly) {
                    val mId = key.substringAfter("spouse_").toLongOrNull() ?: return@forEach
                    val member = activeMembers.firstOrNull { it.id == mId } ?: return@forEach

                    // Spouse Card (Horizontal Pink block)
                    Box(
                        modifier = Modifier
                            .offset(x = offset.x, y = offset.y)
                            .size(nodeWidth, nodeHeight)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)), // Warm pink canvas background
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color(0xFFFDA4AF), RoundedCornerShape(12.dp))
                                .clickable { onSelectMember(member) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Relationship icon",
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Text(
                                        text = "WIFE / SPOUSE",
                                        color = Color(0xFFE11D48),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = member.spouse,
                                        color = Color(0xFF881337),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Wife of ${member.name}",
                                        color = Color(0xFF9F1239),
                                        fontSize = 7.5.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFFFFE4E6),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = "SPOUSE NODE",
                                        color = Color(0xFFE11D48),
                                        fontSize = 6.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Top end direct close/delete spouse connection button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp)
                                .background(Color(0xFFE11D48), CircleShape)
                                .clip(CircleShape)
                                .clickable { onDeleteSpouse(member) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete spouse",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                } else if (key.startsWith("daughter_") && !showMaleOnly) {
                    val parts = key.split("_")
                    val parentId = parts[1].toLongOrNull() ?: return@forEach
                    val dIdx = parts[2].toIntOrNull() ?: return@forEach
                    val parent = activeMembers.firstOrNull { it.id == parentId } ?: return@forEach
                    val daughtersList = parent.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val daughterName = daughtersList.getOrNull(dIdx) ?: "Daughter"

                    // Daughter Card (Horizontal Purple block)
                    Box(
                        modifier = Modifier
                            .offset(x = offset.x, y = offset.y)
                            .size(nodeWidth, nodeHeight)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)), // Beautiful violet canvas background
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Gender icon",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Text(
                                        text = "DAUGHTER",
                                        color = Color(0xFF6D28D9),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = daughterName,
                                        color = Color(0xFF4C1D95),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Daughter of ${parent.name}",
                                        color = Color(0xFF5B21B6),
                                        fontSize = 7.5.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFFEDE9FE),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = "GEN ${parent.generation + 1}",
                                        color = Color(0xFF7C3AED),
                                        fontSize = 6.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Top end direct close/delete daughter button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp)
                                .background(Color(0xFF6D28D9), CircleShape)
                                .clip(CircleShape)
                                .clickable { onDeleteDaughter(parent, dIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete daughter",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Left Bottom corner Overlay Controls (Adobe UI Style)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color(0xFF475569))
                    }

                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        color = Color(0xFF1E293B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        maxLines = 1
                    )

                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(2.5f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color(0xFF475569))
                    }

                    VerticalDivider(
                        modifier = Modifier.height(18.dp),
                        color = Color(0xFFE2E8F0)
                    )

                    IconButton(
                        onClick = {
                            zoomScale = 0.7f
                            panOffset = Offset(250f, 180f)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Workspace", tint = KurdPrimary)
                    }

                    VerticalDivider(
                        modifier = Modifier.height(18.dp),
                        color = Color(0xFFE2E8F0)
                    )

                    // Gender toggle check box
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleMaleOnly(!showMaleOnly) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = showMaleOnly,
                            onCheckedChange = null, // entire row is clickable
                            colors = CheckboxDefaults.colors(checkedColor = KurdPrimary),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تەنیا نێر (Male Only)",
                            color = Color(0xFF1E293B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Layout coordinate mapper implementation
fun calculateLayoutPositions(
    members: List<FamilyMember>,
    nodeWidth: Dp,
    nodeHeight: Dp,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    showMaleOnly: Boolean
): Map<String, DpOffset> {
    val positions = mutableMapOf<String, DpOffset>()
    if (members.isEmpty()) return positions

    val childrenMap = members.groupBy { it.parentId }
    val roots = members.filter { it.parentId == null }

    val activeRoots = if (roots.isEmpty()) {
        val memberIds = members.map { it.id }.toSet()
        members.filter { it.parentId == null || !memberIds.contains(it.parentId) }
    } else {
        roots
    }

    var nextLeafXDp = 16.dp

    // Recursive layout pass
    fun layoutNode(member: FamilyMember, depth: Int): Dp {
        val y = (depth * (nodeHeight.value + verticalSpacing.value)).dp

        // Filter sons & daughters to empty if parent is not expanded to hide hidden branches in layout calculation
        val sons = if (member.isExpanded) {
            childrenMap[member.id] ?: emptyList()
        } else {
            emptyList()
        }
        val daughtersList = if (member.isExpanded && member.daughters.isNotEmpty() && !showMaleOnly) {
            member.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        // Combined horizontal row layout keys
        val childKeys = mutableListOf<String>()
        val sonKeysMap = mutableMapOf<String, FamilyMember>()

        sons.forEach { son ->
            val key = "member_${son.id}"
            childKeys.add(key)
            sonKeysMap[key] = son
        }
        daughtersList.forEachIndexed { idx, _ ->
            val key = "daughter_${member.id}_$idx"
            childKeys.add(key)
        }

        val hasSpouse = member.spouse.isNotEmpty() && !showMaleOnly
        val coupledWidth = if (hasSpouse) {
            nodeWidth * 2 + 16.dp
        } else {
            nodeWidth
        }

        val x: Dp
        if (childKeys.isEmpty()) {
            x = nextLeafXDp
            positions["member_${member.id}"] = DpOffset(x, y)
            if (hasSpouse) {
                positions["spouse_${member.id}"] = DpOffset(x + nodeWidth + 16.dp, y)
            }
            nextLeafXDp += coupledWidth + horizontalSpacing
        } else {
            // Track start boundary of children layout to prevent parent overflow/overlap to the left of it
            val startingX = nextLeafXDp

            val childXs = mutableListOf<Dp>()
            childKeys.forEach { childKey ->
                if (sonKeysMap.containsKey(childKey)) {
                    val son = sonKeysMap[childKey]!!
                    val childX = layoutNode(son, depth + 1)
                    childXs.add(childX)
                } else {
                    val childX = nextLeafXDp
                    positions[childKey] = DpOffset(childX, y + nodeHeight + verticalSpacing)
                    childXs.add(childX)
                    nextLeafXDp += nodeWidth + horizontalSpacing
                }
            }

            val firstChildX = childXs.first()
            val lastChildX = childXs.last()

            // Center parent perfectly over its child span range
            val centerOfChildren = (firstChildX + lastChildX + nodeWidth) / 2
            val calculatedX = centerOfChildren - coupledWidth / 2

            // CRITICAL OVERLAP FIX: Force coordinate X to always stay to the right of startingX limit
            x = calculatedX.coerceAtLeast(startingX)

            positions["member_${member.id}"] = DpOffset(x, y)
            if (hasSpouse) {
                positions["spouse_${member.id}"] = DpOffset(x + nodeWidth + 16.dp, y)
            }

            if (x + coupledWidth + horizontalSpacing > nextLeafXDp) {
                nextLeafXDp = x + coupledWidth + horizontalSpacing
            }
        }

        return x
    }

    activeRoots.forEach { layoutNode(it, 0) }
    return positions
}
