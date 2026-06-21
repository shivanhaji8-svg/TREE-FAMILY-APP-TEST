package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamilyMember
import com.example.ui.theme.KurdPrimary
import com.example.ui.theme.KurdDarkBlue
import com.example.ui.theme.KurdSecondary
import com.example.ui.theme.KurdText

@Composable
fun OutlineTreeView(
    members: List<FamilyMember>,
    selectedMember: FamilyMember?,
    onSelectMember: (FamilyMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Plain Text Tree, 1 = JSON Coding Schema
    val clipboardManager = LocalClipboardManager.current
    var isCopiedProposed by remember { mutableStateOf(false) }

    // Generate nested text-based ASCII representations
    val asciiTree = remember(members) {
        generateAsciiTreeString(members)
    }

    val schemaJson = remember {
        """{
  "familyMember": {
    "id": 1001,
    "name": "Jane Doe",
    "birthYear": "1955",
    "agePlaceholder": "71 (computed if alive)",
    "generation": 2,
    "biographyNote": "Community organizer, educator.",
    "relationships": {
      "spouse": {
        "name": "John Doe",
        "birthYear": "1950",
        "relationshipType": "Husband"
      },
      "sons": [
        {
          "name": "Robert Doe",
          "birthYear": "1980",
          "relationshipType": "Son (Branches Out)"
        },
        {
          "name": "Kevin Doe",
          "birthYear": "1985",
          "relationshipType": "Son"
        }
      ],
      "daughters": [
        {
          "name": "Sarah Doe",
          "birthYear": "1982",
          "relationshipType": "Daughter"
        }
      ]
    }
  }
}"""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // crisp background style
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Headers (Monospace Outline vs Developer Schema)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabWeightModifier = Modifier.weight(1f)
            
            // Tab 1: Text-Based Tree
            Button(
                onClick = { selectedTab = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) Color.White else Color.Transparent,
                    contentColor = if (selectedTab == 0) KurdDarkBlue else Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (selectedTab == 0) 1.dp else 0.dp
                ),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = tabWeightModifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TextFormat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Text Hierarchy Outline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tab 2: Coding Schema
            Button(
                onClick = { selectedTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) Color.White else Color.Transparent,
                    contentColor = if (selectedTab == 1) KurdDarkBlue else Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (selectedTab == 1) 1.dp else 0.dp
                ),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = tabWeightModifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("JSON Developer Schema", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subheader Controls Block (Copy to Clipboard / Select Indicator)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedTab == 0) "PURE TEXT-BASED RELATIONSHIPS OUTLINE" else "JSON IMPLEMENTATION STRUCTURE",
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    val textToCopy = if (selectedTab == 0) asciiTree else schemaJson
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    isCopiedProposed = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCopiedProposed) Color(0xFF10B981) else KurdPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isCopiedProposed) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isCopiedProposed) "Copied!" else "Copy Text",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Reset copy indication helper when text tab changes
            LaunchedEffect(selectedTab) {
                isCopiedProposed = false
            }
            LaunchedEffect(asciiTree) {
                isCopiedProposed = false
            }
        }

        // Code and Monospace text display viewport
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (selectedTab == 0) {
                        // Interactive Text Tree Rendering
                        if (members.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No family members found. Generate or add a member to start.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            // Beautiful interactive clickable rendering list representing hierarchical structure
                            Text(
                                text = asciiTree,
                                color = Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        // JSON Schema Display Panel
                        Text(
                            text = schemaJson,
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // Interactive click feedback guidance footer
        Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                Text(
                    text = "This purely text-based representation maps spouse, sons, and daughters details to fulfill real-time copy/paste export needs. Edit direct spouses and daughters on the sidebar properties inspector.",
                    fontSize = 11.sp,
                    color = Color(0xFF1E3A8A),
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// DFS Text Tree ASCII compiler representing Spouse, Sons, Daughters
fun generateAsciiTreeString(members: List<FamilyMember>): String {
    if (members.isEmpty()) return "No patrilineal descendants declared."

    val output = StringBuilder()
    output.append("FAMILY KURD - COMPLETE PATRILINEAL RELATIONSHIP OUTLINE\n")
    output.append("========================================================\n\n")

    val childrenMap = members.groupBy { it.parentId }
    val roots = members.filter { it.parentId == null }

    val activeRoots = if (roots.isEmpty()) {
        val memberIds = members.map { it.id }.toSet()
        members.filter { it.parentId == null || !memberIds.contains(it.parentId) }
    } else {
        roots
    }

    fun buildNodeText(member: FamilyMember, prefix: String, isLast: Boolean) {
        val nodeSymbol = if (isLast) "└── " else "├── "
        val childPrefix = prefix + if (isLast) "    " else "│   "

        // Primary Name \& details stamp
        output.append(prefix)
              .append(nodeSymbol)
              .append("👤 ")
              .append(member.name)
              .append(" (Gen ${member.generation}, Birth: ${member.birthYear.ifEmpty { "N/A" }})")
              .append("\n")

        // 1. Spouse Info (Wife / Husband) nest
        if (member.spouse.isNotEmpty()) {
            output.append(childPrefix)
                  .append("├── 💍 Spouse: ")
                  .append(member.spouse)
                  .append("\n")
        } else {
            output.append(childPrefix)
                  .append("├── 💍 Spouse: [No Spouse Listed - Placeholders: Name, Age]\n")
        }

        // 2. Daughters Info nest
        if (member.daughters.isNotEmpty()) {
            output.append(childPrefix)
                  .append("├── 👧 Daughters:\n")
            
            val daughterList = member.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            daughterList.forEachIndexed { idx, daughterName ->
                val daughterSymbol = if (idx == daughterList.lastIndex) "└── " else "├── "
                output.append(childPrefix)
                      .append("│   ")
                      .append(daughterSymbol)
                      .append("🎀 ")
                      .append(daughterName)
                      .append("\n")
            }
        } else {
            output.append(childPrefix)
                  .append("├── 👧 Daughters: [No Daughters Listed - Placeholders: Name, Age]\n")
        }

        // 3. Sons Info (linked descendants in database) nest
        val directSons = childrenMap[member.id] ?: emptyList()
        if (directSons.isNotEmpty()) {
            output.append(childPrefix)
                  .append("└── 👦 Sons & Branches:\n")
            
            val sonsPrefix = childPrefix + "    "
            directSons.forEachIndexed { index, son ->
                val sonLast = index == directSons.lastIndex
                buildNodeText(son, sonsPrefix, sonLast)
            }
        } else {
            output.append(childPrefix)
                  .append("└── 👦 Sons & Branches: [No Descendants Listed - Placeholders: Name, Age]\n")
        }
        
        output.append("\n")
    }

    activeRoots.forEachIndexed { idx, root ->
        val lastRoot = idx == activeRoots.lastIndex
        buildNodeText(root, "", lastRoot)
    }

    return output.toString()
}
