package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamilyMember
import com.example.ui.theme.KurdBackground
import com.example.ui.theme.KurdBorder
import com.example.ui.theme.KurdDarkBlue
import com.example.ui.theme.KurdPrimary
import com.example.ui.theme.KurdSecondary
import com.example.ui.theme.KurdText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableView(
    members: List<FamilyMember>,
    selectedMember: FamilyMember?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectMember: (FamilyMember) -> Unit,
    onToggleExpanded: (FamilyMember) -> Unit,
    onAddSon: (FamilyMember) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
    onDeleteSpouse: (FamilyMember) -> Unit,
    onDeleteDaughter: (FamilyMember, Int) -> Unit,
    showMaleOnly: Boolean,
    onToggleMaleOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter physical family member list if showMaleOnly is true
    val activeMembers = remember(members, showMaleOnly) {
        if (showMaleOnly) members.filter { it.gender != "Female" } else members
    }

    // Dynamic branch fold logic
    val visibleMembers = remember(activeMembers, searchQuery) {
        if (searchQuery.isNotBlank()) {
            // When searching, ignore folding to let matched people surface anywhere
            activeMembers.sortedWith(compareBy({ it.generation }, { it.id }))
        } else {
            // Respect folder expansions recursively
            getVisibleMembers(activeMembers)
        }
    }

    val childrenCountMap = remember(activeMembers) {
        activeMembers.groupBy { it.parentId }.mapValues { it.value.size }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KurdBackground)
            .padding(16.dp)
    ) {
        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search lineage, birth years, or records...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = Color.Gray) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Text("Clear", color = KurdPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KurdPrimary,
                unfocusedBorderColor = KurdBorder,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        // Gender toggler FilterChip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showMaleOnly,
                onClick = { onToggleMaleOnly(!showMaleOnly) },
                label = { Text("تەنیا نێر (Male Only)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = if (showMaleOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KurdPrimary.copy(alpha = 0.15f),
                    selectedLabelColor = KurdPrimary,
                    selectedLeadingIconColor = KurdPrimary,
                    containerColor = Color.White,
                    labelColor = Color(0xFF475569)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Spreadsheet Header Row
        Surface(
            color = Color(0xFFF1F5F9),
            modifier = Modifier.border(1.dp, KurdBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FAMILY MEMBER RELATION",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1.8f)
                )

                Text(
                    text = "GEN",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(0.4f)
                )

                Text(
                    text = "BIRTH OR STATUS",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(0.8f)
                )

                Text(
                    text = "ACTION",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        if (visibleMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, KurdBorder, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching patrilineal members found.", color = Color.Gray)
            }
        } else {
            // Table Rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, KurdBorder, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(visibleMembers, key = { it.id }) { member ->
                    val isSelected = selectedMember?.id == member.id
                    val numChildren = childrenCountMap[member.id] ?: 0
                    val isParent = numChildren > 0

                    Column {
                        // 1. Core Male patrilineal tree node row
                        val iconLabel = if (member.gender == "Female") "👩" else "👨"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) KurdSecondary else Color.Transparent)
                                .clickable { onSelectMember(member) }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Expanded visual spacing with indents + chevron
                            Row(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .padding(start = (member.generation * 16).dp), // progressive indent
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isParent) {
                                    IconButton(
                                        onClick = { onToggleExpanded(member) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (member.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Toggle Branch",
                                            tint = KurdPrimary
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(24.dp))
                                }

                                Text(
                                    text = "$iconLabel ${member.name}",
                                    color = KurdText,
                                    fontSize = 14.sp,
                                    fontWeight = if (isParent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            // Column 2: Generation count
                            Text(
                                text = "G${member.generation}",
                                color = KurdDarkBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.4f)
                            )

                            // Column 3: Birth Year
                            Text(
                                text = member.birthYear.ifEmpty { "N/A" },
                                color = if (member.birthYear.isEmpty()) Color.LightGray else KurdText,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(0.8f)
                            )

                            // Column 4: Inline actions
                            Row(
                                modifier = Modifier.weight(0.8f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Add Son Action
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(KurdPrimary, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { onAddSon(member) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Son",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Action
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                        .clip(CircleShape)
                                        .clickable { onDeleteMember(member) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // 2. Spouse Sub-row display directly underneath husband tree node
                        if (member.spouse.isNotEmpty() && !showMaleOnly) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF1F2)) // Soft warm rose container
                                    .padding(vertical = 6.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .padding(start = ((member.generation) * 16 + 24).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "└── 💍 Spouse: ${member.spouse}",
                                        color = Color(0xFF9F1239),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Text(
                                    text = "—",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(0.4f)
                                )
                                
                                Text(
                                    text = "Spouse of ${member.name}",
                                    color = Color(0xFFE11D48),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(0.8f)
                                )

                                Row(
                                    modifier = Modifier.weight(0.8f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width(26.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFFFFE4E6), CircleShape)
                                            .clip(CircleShape)
                                            .clickable { onDeleteSpouse(member) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Spouse",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Daughters/Sisters list nested displays
                        if (member.daughters.isNotEmpty() && !showMaleOnly) {
                            val daughtersList = member.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            daughtersList.forEachIndexed { dIdx, dName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F3FF)) // Soft modern lavender
                                        .padding(vertical = 6.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1.8f)
                                            .padding(start = ((member.generation) * 16 + 24).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "└── 👧 Daughter: $dName",
                                            color = Color(0xFF5B21B6),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    Text(
                                        text = "G${member.generation + 1}",
                                        color = Color(0xFF7C3AED),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.4f)
                                    )
                                    
                                    Text(
                                        text = "Daughter of ${member.name}",
                                        color = Color(0xFF7C3AED),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(0.8f)
                                    )

                                    Row(
                                        modifier = Modifier.weight(0.8f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(26.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFFEDE9FE), CircleShape)
                                                .clip(CircleShape)
                                                .clickable { onDeleteDaughter(member, dIdx) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Daughter",
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = KurdBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// Ancestral evaluation helper
private fun getVisibleMembers(allMembers: List<FamilyMember>): List<FamilyMember> {
    val memberMap = allMembers.associateBy { it.id }
    
    // Sort so parent nodes naturally process prior to children
    val sorted = allMembers.sortedWith(compareBy({ it.generation }, { it.id }))
    
    return sorted.filter { member ->
        var parentId = member.parentId
        var visible = true
        while (parentId != null) {
            val parent = memberMap[parentId]
            if (parent == null) {
                break
            }
            if (!parent.isExpanded) {
                visible = false
                break
            }
            parentId = parent.parentId
        }
        visible
    }
}
