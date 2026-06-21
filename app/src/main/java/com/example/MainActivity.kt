package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FamilyMember
import com.example.ui.ExportHelper
import com.example.ui.FamilyViewModel
import com.example.ui.TableView
import com.example.ui.TreeView
import com.example.ui.OutlineTreeView
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ArrowDropDown
import com.example.ui.ViewMode
import com.example.ui.theme.KurdBackground
import com.example.ui.theme.KurdBorder
import com.example.ui.theme.KurdDarkBlue
import com.example.ui.theme.KurdPrimary
import com.example.ui.theme.KurdSecondary
import com.example.ui.theme.KurdText
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FamilyKurdApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyKurdApp(
    modifier: Modifier = Modifier,
    viewModel: FamilyViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // State bindings from ViewModel
    val rawMembers by viewModel.allMembers.collectAsStateWithLifecycle()
    val filteredMembers by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMember.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val isScreenMode by viewModel.isScreenMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showMaleOnly by viewModel.showMaleOnly.collectAsStateWithLifecycle()

    // Project state flows
    val currentProjectId by viewModel.currentProjectId.collectAsStateWithLifecycle()
    val createdProjectIds by viewModel.createdProjectIds.collectAsStateWithLifecycle()
    val projectNames by viewModel.projectNames.collectAsStateWithLifecycle()
    val activeProjectName = projectNames[currentProjectId] ?: "Ahmad Haji Ancestry"

    // Dialog state controllers
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showProjectManagerDialog by remember { mutableStateOf(false) }
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var createProjectText by remember { mutableStateOf("") }
    var projectToRename by remember { mutableStateOf<Int?>(null) }
    var renameProjectText by remember { mutableStateOf("") }
    var showPatriarchDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<FamilyMember?>(null) }
    var showAddMemberDialog by remember { mutableStateOf<FamilyMember?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // Export Filter and Format states
    var exportShowSon by remember { mutableStateOf(true) }
    var exportShowSpouse by remember { mutableStateOf(true) }
    var exportShowDaughter by remember { mutableStateOf(true) }
    var exportShowBrother by remember { mutableStateOf(true) }
    var exportShowSister by remember { mutableStateOf(true) }
    var selectedExportFormat by remember { mutableStateOf("PDF") }

    // Quick add / edit text field states
    var newSonName by remember { mutableStateOf("") }
    var newSonBirthYear by remember { mutableStateOf("") }
    var newSonNote by remember { mutableStateOf("") }

    var patriarchName by remember { mutableStateOf("") }
    var patriarchBirthYear by remember { mutableStateOf("") }
    var patriarchNote by remember { mutableStateOf("") }

    // Backup triggers
    LaunchedEffect(rawMembers) {
        if (rawMembers.isEmpty()) {
            showPatriarchDialog = true
        } else {
            showPatriarchDialog = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KurdBackground)
    ) {
        val isLandscape = maxWidth > 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP NAVBAR PANEL (Sleek Interface Redesign)
            if (!isScreenMode) {
                Surface(
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Brand Group with letter 'F' emblem
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(KurdPrimary, RoundedCornerShape(12.dp))
                                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "F",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Family Kurd",
                                        color = Color(0xFF1F2937),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "PATRILINEAL BUILDER",
                                        color = KurdPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Interactive Project switcher dropdown card
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { showProjectManagerDialog = true }
                                        .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Projects List",
                                            tint = KurdPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = activeProjectName,
                                            color = Color(0xFF334155),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 120.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            // Right Controls & Modes Group: ONLY Tree & Table View modes
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Professional Global Collapse/Expand All controls
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.expandAll() }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ExpandMore,
                                                contentDescription = "Expand All",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Expand All", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(16.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.collapseAll() }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ExpandLess,
                                                contentDescription = "Collapse All",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Collapse All", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // View Mode Segmented Controls (Light Slate)
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val buttonColorTree = if (viewMode == ViewMode.TREE) KurdPrimary else Color.Transparent
                                    val textColorTree = if (viewMode == ViewMode.TREE) Color.White else Color(0xFF475569)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(buttonColorTree)
                                            .clickable { viewModel.setViewMode(ViewMode.TREE) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CompareArrows,
                                                contentDescription = "Tree Mode",
                                                tint = textColorTree,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Tree", color = textColorTree, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    val buttonColorTable = if (viewMode == ViewMode.TABLE) KurdPrimary else Color.Transparent
                                    val textColorTable = if (viewMode == ViewMode.TABLE) Color.White else Color(0xFF475569)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(buttonColorTable)
                                            .clickable { viewModel.setViewMode(ViewMode.TABLE) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.List,
                                                contentDescription = "Table Mode",
                                                tint = textColorTable,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Table", color = textColorTable, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Portrait/Mobile vertical dynamic stack to prevent overlap and truncation
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Brand Group with letter 'F' emblem
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(KurdPrimary, RoundedCornerShape(10.dp))
                                            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "F",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Family Kurd",
                                            color = Color(0xFF1F2937),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "PATRILINEAL BUILDER",
                                            color = KurdPrimary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }

                                // Interactive Project switcher dropdown card (Compact)
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { showProjectManagerDialog = true }
                                        .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Projects List",
                                            tint = KurdPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = activeProjectName,
                                            color = Color(0xFF334155),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 110.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            // Secondary mobile toolbar line for Global Expand/Collapse controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.expandAll() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ExpandMore,
                                                contentDescription = "Expand All",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Expand All", color = Color(0xFF475569), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(16.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.collapseAll() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ExpandLess,
                                                contentDescription = "Collapse All",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Collapse All", color = Color(0xFF475569), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Minimized status workspace header for screen mode exit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KurdDarkBlue)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "FAMILY WORKSPACE MODE",
                            color = KurdPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "(${rawMembers.size} patrilineal descendants active)",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(KurdPrimary, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.setScreenMode(false) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Screen Mode", tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Exit Full", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. PRIMARY COMBINED VIEWPORT WORKSPACE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Main Workspace Layout Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (viewMode == ViewMode.TREE) {
                        TreeView(
                            members = rawMembers,
                            selectedMember = selectedMember,
                            onSelectMember = { viewModel.selectMember(it) },
                            onAddMemberClick = { showAddMemberDialog = it },
                            onDeleteMember = { showDeleteConfirmDialog = it },
                            onDeleteSpouse = { viewModel.deleteSpouse(it) },
                            onDeleteDaughter = { member, idx -> viewModel.deleteDaughter(member, idx) },
                            onToggleExpanded = { viewModel.toggleExpanded(it) },
                            showMaleOnly = showMaleOnly,
                            onToggleMaleOnly = { viewModel.setShowMaleOnly(it) }
                        )
                    } else if (viewMode == ViewMode.TABLE) {
                        TableView(
                            members = rawMembers,
                            selectedMember = selectedMember,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onSelectMember = { viewModel.selectMember(it) },
                            onToggleExpanded = { viewModel.toggleExpanded(it) },
                            onAddSon = { showAddMemberDialog = it },
                            onDeleteMember = { showDeleteConfirmDialog = it },
                            onDeleteSpouse = { viewModel.deleteSpouse(it) },
                            onDeleteDaughter = { member, idx -> viewModel.deleteDaughter(member, idx) },
                            showMaleOnly = showMaleOnly,
                            onToggleMaleOnly = { viewModel.setShowMaleOnly(it) }
                        )
                    } else {
                        OutlineTreeView(
                            members = rawMembers,
                            selectedMember = selectedMember,
                            onSelectMember = { viewModel.selectMember(it) }
                        )
                    }
                }

                // Landscape right-hand properties sidebar (Adobe style)
                if (isLandscape && selectedMember != null && !isScreenMode) {
                    Surface(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .border(1.dp, KurdBorder),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        SidebarPropertiesPanel(
                            member = selectedMember!!,
                            onClose = { viewModel.selectMember(null) },
                            onUpdate = { name, year, note, spouse, daughters, gender ->
                                viewModel.updateMember(selectedMember!!, name, year, note, spouse = spouse, daughters = daughters, gender = gender)
                                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onAddRelative = { showAddMemberDialog = selectedMember },
                            onDelete = { showDeleteConfirmDialog = selectedMember }
                        )
                    }
                }
            }

            // Portrait details sliding drawer/sheet (renders on small screens when selectedMember != null)
            if (!isLandscape && selectedMember != null && !isScreenMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, KurdBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = KurdPrimary)
                                Text("Descendant Properties", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KurdText)
                            }
                            IconButton(onClick = { viewModel.selectMember(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        var tempName by remember(selectedMember) { mutableStateOf(selectedMember?.name ?: "") }
                        var tempYear by remember(selectedMember) { mutableStateOf(selectedMember?.birthYear ?: "") }
                        var tempNote by remember(selectedMember) { mutableStateOf(selectedMember?.note ?: "") }
                        var tempSpouse by remember(selectedMember) { mutableStateOf(selectedMember?.spouse ?: "") }
                        var tempDaughters by remember(selectedMember) { mutableStateOf(selectedMember?.daughters ?: "") }
                        var tempGender by remember(selectedMember) { mutableStateOf(selectedMember?.gender ?: "Male") }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Patrilineal Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempYear,
                            onValueChange = { tempYear = it },
                            label = { Text("Birth Year (Estimate/Actual)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempSpouse,
                            onValueChange = { tempSpouse = it },
                            label = { Text("Spouse Name (Husband / Wife)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempDaughters,
                            onValueChange = { tempDaughters = it },
                            label = { Text("Daughters (separated by commas)") },
                            placeholder = { Text("e.g., Zeynab, Maryam") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gender Selector (Portrait Mode)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("GENDER / ڕەگەز", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Male", "Female").forEach { g ->
                                    val isSel = tempGender == g
                                    val displayName = if (g == "Male") "Male / نێر" else "Female / مێ"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) KurdPrimary else Color.Transparent)
                                            .clickable { tempGender = g }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayName,
                                            color = if (isSel) Color.White else Color(0xFF475569),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempNote,
                            onValueChange = { tempNote = it },
                            label = { Text("Descent biography notes") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateMember(selectedMember!!, tempName, tempYear, tempNote, spouse = tempSpouse, daughters = tempDaughters, gender = tempGender)
                                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Info", color = Color.White)
                            }

                            Button(
                                onClick = { showAddMemberDialog = selectedMember },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = KurdDarkBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("Add Relative", color = Color.White)
                                }
                            }

                            Button(
                                onClick = { showDeleteConfirmDialog = selectedMember },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f), contentColor = Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }

            // 3. ELEGANT BOTTOM UTILITY & ACTIONS BAR
            if (!isScreenMode) {
                Surface(
                    color = Color.White,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Tree View (Family Tree)
                        val isTreeActive = viewMode == ViewMode.TREE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setViewMode(ViewMode.TREE) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isTreeActive) KurdPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CompareArrows,
                                        contentDescription = "Tree Mode",
                                        tint = if (isTreeActive) KurdPrimary else Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Family Tree",
                                    color = if (isTreeActive) KurdPrimary else Color(0xFF475569),
                                    fontSize = 9.sp,
                                    fontWeight = if (isTreeActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 2. Table View (Members Table)
                        val isTableActive = viewMode == ViewMode.TABLE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setViewMode(ViewMode.TABLE) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isTableActive) KurdPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.List,
                                        contentDescription = "Table Mode",
                                        tint = if (isTableActive) KurdPrimary else Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Members",
                                    color = if (isTableActive) KurdPrimary else Color(0xFF475569),
                                    fontSize = 9.sp,
                                    fontWeight = if (isTableActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 3. Outline View (Branch Outline)
                        val isOutlineActive = viewMode == ViewMode.OUTLINE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setViewMode(ViewMode.OUTLINE) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isOutlineActive) KurdPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Note,
                                        contentDescription = "Outline Mode",
                                        tint = if (isOutlineActive) KurdPrimary else Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Outline",
                                    color = if (isOutlineActive) KurdPrimary else Color(0xFF475569),
                                    fontSize = 9.sp,
                                    fontWeight = if (isOutlineActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 4. Full Screen Mode (Full Screen)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setScreenMode(true) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Fullscreen,
                                        contentDescription = "Full Screen",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Full Screen",
                                    color = Color(0xFF475569),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 5. Import/Export dialog Button (Import/Export)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showImportExportDialog = true }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Sync",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Sync",
                                    color = Color(0xFF475569),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================== DIALOG COLLECTIONS ==================

        // 1. DYNAMIC DUAL-CONNECTOR FAMILY ADDITION SYSTEM
        if (showAddMemberDialog != null) {
            val relativeNode = showAddMemberDialog!!
            var selectedType by remember { mutableStateOf("Son") }
            var relativeName by remember { mutableStateOf("") }
            var relativeYear by remember { mutableStateOf("") }
            var relativeNote by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddMemberDialog = null },
                title = {
                    Column {
                        Text("Add Family Relative", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KurdText)
                        Text("Connect with: ${relativeNode.name}", color = KurdPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Select relation type to dynamically expand your lineage tree automatically. Connections update in real time.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        // Dual Segment Selector Group
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Spouse", "Son", "Daughter").forEach { type ->
                                        val displayName = when (type) {
                                            "Spouse" -> "Spouse / هاوسەر"
                                            "Son" -> "Son / کور"
                                            "Daughter" -> "Daughter / کچ"
                                            else -> type
                                        }
                                        Button(
                                            onClick = { selectedType = type },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedType == type) KurdPrimary else Color.Transparent,
                                                contentColor = if (selectedType == type) Color.White else Color(0xFF475569)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(displayName, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Brother", "Sister").forEach { type ->
                                        val isEnabled = relativeNode.parentId != null
                                        val displayName = when (type) {
                                            "Brother" -> "Brother / برا"
                                            "Sister" -> "Sister / خوشک"
                                            else -> type
                                        }
                                        Button(
                                            onClick = { selectedType = type },
                                            enabled = isEnabled,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedType == type) KurdPrimary else Color.Transparent,
                                                contentColor = if (selectedType == type) Color.White else Color(0xFF475569)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(displayName, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedType == "Brother" || selectedType == "Sister") {
                            if (relativeNode.parentId == null) {
                                Text(
                                    "Cannot add siblings to the ultimate root founder. Select a descendant to add brothers/sisters.",
                                    color = Color.Red,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = relativeName,
                            onValueChange = { relativeName = it },
                            label = { Text("Name of $selectedType") },
                            placeholder = { Text("Enter name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        if (selectedType != "Daughter" && selectedType != "Sister") {
                            OutlinedTextField(
                                value = relativeYear,
                                onValueChange = { relativeYear = it },
                                label = { Text("Birth Year (Optional)") },
                                placeholder = { Text("e.g. 1990") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = relativeNote,
                                onValueChange = { relativeNote = it },
                                label = { Text("Biography Records / Notes") },
                                placeholder = { Text("Enter biography notes...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 2
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (relativeName.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            when (selectedType) {
                                "Spouse" -> viewModel.addSpouse(relativeNode, relativeName)
                                "Son" -> viewModel.addSon(relativeNode, relativeName, relativeYear, relativeNote)
                                "Daughter" -> viewModel.addDaughter(relativeNode, relativeName)
                                "Brother" -> viewModel.addBrother(relativeNode, relativeName, relativeYear, relativeNote)
                                "Sister" -> viewModel.addSister(relativeNode, relativeName)
                            }
                            showAddMemberDialog = null
                            Toast.makeText(context, "Added $selectedType successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary)
                    ) {
                        Text("Confirm Add", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMemberDialog = null }) {
                        Text("Cancel", color = KurdDarkBlue)
                    }
                }
            )
        }

        // 2. ROOT ANCESTOR PATRIARCH INITIAL GENERATOR
        if (showPatriarchDialog) {
            AlertDialog(
                onDismissRequest = { /* Force entry */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = { Text("Create Initial Ancestor Root", fontWeight = FontWeight.Bold, color = KurdText) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "You are generating a brand new lineage. Set the founder (patriarch root) node of the family tree.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        OutlinedTextField(
                            value = patriarchName,
                            onValueChange = { patriarchName = it },
                            label = { Text("Patriarch Name") },
                            placeholder = { Text("e.g., Haji Ahmad") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = patriarchBirthYear,
                            onValueChange = { patriarchBirthYear = it },
                            label = { Text("Birth Year (Approx)") },
                            placeholder = { Text("e.g., 1900") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = patriarchNote,
                            onValueChange = { patriarchNote = it },
                            label = { Text("Origin details / notes") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (patriarchName.isNotBlank()) {
                                viewModel.addRoot(patriarchName, patriarchBirthYear, patriarchNote)
                                showPatriarchDialog = false
                                Toast.makeText(context, "Patriarch root created successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Please enter a valid Patriarch Name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary)
                    ) {
                        Text("Create Root", color = Color.White)
                    }
                }
            )
        }

        // 3. SECURE CASCADE DELETE CONFIRM DIALOG
        if (showDeleteConfirmDialog != null) {
            val target = showDeleteConfirmDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Confirm Cascade Deletion", fontWeight = FontWeight.Bold, color = Color.Red) },
                text = {
                    Text(
                        text = "Deleting ${target.name} will also permanently collapse and delete ALL of their descendants. This action cannot be reversed. Continue?",
                        fontSize = 14.sp,
                        color = KurdText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMember(target)
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "Cascaded node deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Yes, Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancel", color = KurdDarkBlue)
                    }
                }
            )
        }

        // 4. IMPORT / EXPORT UTILITIES DIALOG
        if (showImportExportDialog) {
            var inputJsonImport by remember { mutableStateOf("") }
            var isErrorImport by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showImportExportDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = KurdPrimary)
                        Text("Project Backup & Exports", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KurdText)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // EXPORT PDF / IMAGE DIRECT ACTIONS (LAUNCHES MODERN SELECTOR)
                        Text("Export Layout Visual Files", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KurdText)
                        Button(
                            onClick = {
                                showImportExportDialog = false
                                showExportSelectionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Text("Export Layout Files (Choose PDF or Image)", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = KurdBorder)

                        // COPY RAW JSON BACKUP
                        Text("JSON Backup Integration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KurdText)
                        Button(
                            onClick = {
                                val json = viewModel.exportToJson()
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "JSON string copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, tint = KurdText, modifier = Modifier.size(16.dp))
                                Text("Copy JSON Core String", color = KurdText, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = KurdBorder)

                        // RESTORE & IMPORT JSON DATA
                        Text("Deport / Restore Family Core", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KurdText)
                        OutlinedTextField(
                            value = inputJsonImport,
                            onValueChange = {
                                inputJsonImport = it
                                isErrorImport = false
                            },
                            isError = isErrorImport,
                            placeholder = { Text("Paste valid Family Kurd JSON back here to restore details...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )

                        if (isErrorImport) {
                            Text("Oops! Invalid format pasted. Ensure brackets are complete.", color = Color.Red, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                if (inputJsonImport.isNotBlank()) {
                                    val success = viewModel.importTreeFromJson(inputJsonImport)
                                    if (success) {
                                        showImportExportDialog = false
                                        Toast.makeText(context, "Database restored with complete lineage!", Toast.LENGTH_LONG).show()
                                    } else {
                                        isErrorImport = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore Database Link", color = Color.White)
                        }

                        HorizontalDivider(color = KurdBorder)

                        // COMPLETELY WIPE LINEAGE
                        Button(
                            onClick = {
                                viewModel.clearTree()
                                showImportExportDialog = false
                                Toast.makeText(context, "All databases wiped. Setup a new patriarch.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.12f), contentColor = Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ClearAll, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Text("Wipe Complete Lineage", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showImportExportDialog = false }) {
                        Text("Done", color = KurdPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showClearAllConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirmDialog = false },
                title = { Text("Clear Entire Family Tree", fontWeight = FontWeight.Bold, color = Color.Red) },
                text = { Text("Are you absolutely sure you want to clear all descendants and the patriarch? This action is irreversible!") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearTree()
                            showClearAllConfirmDialog = false
                            Toast.makeText(context, "Family Tree has been cleared.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirmDialog = false }) {
                        Text("Cancel", color = KurdDarkBlue)
                    }
                }
            )
        }

        // 5. EXPORT FORMAT CHOOSE DIALOG (PDF Vs HIGH RES JPG)
        if (showExportSelectionDialog) {
            AlertDialog(
                onDismissRequest = { showExportSelectionDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = KurdPrimary)
                        Text("Export Workspace Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KurdText)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Customize which relationships should be rendered in your export file layout.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        Text(
                            "1. Select Relationships Filter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = KurdDarkBlue
                        )

                        // 5 Checkboxes beautifully laid out:
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Son Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportShowSon = !exportShowSon }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = exportShowSon,
                                    onCheckedChange = { exportShowSon = it },
                                    colors = CheckboxDefaults.colors(checkedColor = KurdPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Son / کوڕ", fontSize = 13.sp, color = KurdText, fontWeight = FontWeight.SemiBold)
                            }

                            // Spouse Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportShowSpouse = !exportShowSpouse }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = exportShowSpouse,
                                    onCheckedChange = { exportShowSpouse = it },
                                    colors = CheckboxDefaults.colors(checkedColor = KurdPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Spouse / هاوسەر", fontSize = 13.sp, color = KurdText, fontWeight = FontWeight.SemiBold)
                            }

                            // Daughter Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportShowDaughter = !exportShowDaughter }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = exportShowDaughter,
                                    onCheckedChange = { exportShowDaughter = it },
                                    colors = CheckboxDefaults.colors(checkedColor = KurdPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Daughter / کچ", fontSize = 13.sp, color = KurdText, fontWeight = FontWeight.SemiBold)
                            }

                            // Brother Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportShowBrother = !exportShowBrother }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = exportShowBrother,
                                    onCheckedChange = { exportShowBrother = it },
                                    colors = CheckboxDefaults.colors(checkedColor = KurdPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Brother / برا", fontSize = 13.sp, color = KurdText, fontWeight = FontWeight.SemiBold)
                            }

                            // Sister Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportShowSister = !exportShowSister }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = exportShowSister,
                                    onCheckedChange = { exportShowSister = it },
                                    colors = CheckboxDefaults.colors(checkedColor = KurdPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sister / خوشک", fontSize = 13.sp, color = KurdText, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0))

                        Text(
                            "2. Select Export Format",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = KurdDarkBlue
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // PDF format selection
                            Surface(
                                onClick = { selectedExportFormat = "PDF" },
                                color = if (selectedExportFormat == "PDF") Color(0xFFFFF1F2) else Color.White,
                                border = BorderStroke(1.dp, if (selectedExportFormat == "PDF") Color(0xFFFDA4AF) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Note, contentDescription = "PDF icon", tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("PDF Report", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExportFormat == "PDF") Color(0xFF9F1239) else Color(0xFF64748B))
                                }
                            }

                            // JPG Format Selection
                            Surface(
                                onClick = { selectedExportFormat = "JPG" },
                                color = if (selectedExportFormat == "JPG") Color(0xFFF0FDF4) else Color.White,
                                border = BorderStroke(1.dp, if (selectedExportFormat == "JPG") Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Image icon", tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Panoramic JPG", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExportFormat == "JPG") Color(0xFF166534) else Color(0xFF64748B))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedExportFormat == "PDF") {
                                ExportHelper.sharePdf(
                                    context = context,
                                    members = rawMembers,
                                    showSon = exportShowSon,
                                    showSpouse = exportShowSpouse,
                                    showDaughter = exportShowDaughter,
                                    showBrother = exportShowBrother,
                                    showSister = exportShowSister
                                )
                            } else {
                                ExportHelper.shareTreeImage(
                                    context = context,
                                    members = rawMembers,
                                    showSon = exportShowSon,
                                    showSpouse = exportShowSpouse,
                                    showDaughter = exportShowDaughter,
                                    showBrother = exportShowBrother,
                                    showSister = exportShowSister
                                )
                            }
                            showExportSelectionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Generate Export", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportSelectionDialog = false }) {
                        Text("Cancel", color = KurdDarkBlue)
                    }
                }
            )
        }

        // 6. MULTI-PROJECT WORKSPACE SYSTEM MANAGER (UP TO 4 PROJECTS)
        if (showProjectManagerDialog) {
            AlertDialog(
                onDismissRequest = { showProjectManagerDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = KurdPrimary)
                        Text("Project Workspace Manager", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KurdText)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Manage up to 4 parallel family tree workspaces on demand. Each project stores member profiles, spouses, daughters, and lineage models separately.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        createdProjectIds.forEach { i ->
                            val name = projectNames[i] ?: "New Family Tree $i"
                            val isActive = currentProjectId == i

                            Surface(
                                onClick = {
                                    viewModel.setProject(i)
                                    showProjectManagerDialog = false
                                },
                                color = if (isActive) KurdSecondary else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isActive) KurdPrimary else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (isActive) KurdPrimary else Color(0xFF94A3B8),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$i",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = KurdText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isActive) {
                                                Text(
                                                    text = "Active Workspace",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = KurdPrimary
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Button to rename this project slot
                                        IconButton(
                                            onClick = {
                                                projectToRename = i
                                                renameProjectText = name
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename Project",
                                                tint = if (isActive) KurdPrimary else Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Button to delete this project workspace
                                        if (createdProjectIds.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteProject(i)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Project Workspace",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (createdProjectIds.size < 4) {
                            Button(
                                onClick = { showCreateProjectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Create Project",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Create New Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProjectManagerDialog = false }) {
                        Text("Close", color = KurdPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 7. INLINE PROJECT RENAME DIALOG
        if (projectToRename != null) {
            val targetId = projectToRename!!
            AlertDialog(
                onDismissRequest = { projectToRename = null },
                title = {
                    Text("Rename Family Workspace Slot #$targetId", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KurdText)
                },
                text = {
                    OutlinedTextField(
                        value = renameProjectText,
                        onValueChange = { renameProjectText = it },
                        label = { Text("Enter custom project name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KurdPrimary,
                            unfocusedBorderColor = KurdBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.renameProject(targetId, renameProjectText)
                            projectToRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary)
                    ) {
                        Text("Save Changes", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectToRename = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // 8. CREATE NEW PROJECT DIALOG
        if (showCreateProjectDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProjectDialog = false },
                title = {
                    Text("Create New Family Tree", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KurdText)
                },
                text = {
                    OutlinedTextField(
                        value = createProjectText,
                        onValueChange = { createProjectText = it },
                        label = { Text("Workspace Name") },
                        placeholder = { Text("e.g. Haji Rashid Descendants") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KurdPrimary,
                            unfocusedBorderColor = KurdBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = viewModel.createNewProject(createProjectText)
                            if (success) {
                                showCreateProjectDialog = false
                                createProjectText = ""
                                showProjectManagerDialog = false
                                Toast.makeText(context, "New family tree initialized! Set the founders.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Cannot exceed 4 parallel project workspaces.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary)
                    ) {
                        Text("Create Workspace", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProjectDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun SidebarPropertiesPanel(
    member: FamilyMember,
    onClose: () -> Unit,
    onUpdate: (String, String, String, String, String, String) -> Unit,
    onAddRelative: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editName by remember(member) { mutableStateOf(member.name) }
    var editYear by remember(member) { mutableStateOf(member.birthYear) }
    var editNote by remember(member) { mutableStateOf(member.note) }
    var editSpouse by remember(member) { mutableStateOf(member.spouse) }
    var editDaughters by remember(member) { mutableStateOf(member.daughters) }
    var editGender by remember(member) { mutableStateOf(member.gender) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(18.dp))
                Text("Descent Inspector", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KurdDarkBlue)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Inspector", modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = KurdBorder)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PATRILINEAL DETAILS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            
            Surface(
                color = KurdSecondary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(16.dp))
                    Text("Gen Level ${member.generation} descent", fontSize = 12.sp, color = KurdDarkBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Editable Input Fields
        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Display Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = editYear,
            onValueChange = { editYear = it },
            label = { Text("Birth Year / Period") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = editSpouse,
            onValueChange = { editSpouse = it },
            label = { Text("Spouse Name") },
            placeholder = { Text("Husband / Wife Placeholder") },
            leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = editDaughters,
            onValueChange = { editDaughters = it },
            label = { Text("Daughters (separated by commas)") },
            placeholder = { Text("e.g. Zeynab, Maryam") },
            leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Gender Selector Segmented Panel
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("GENDER / ڕەگەز", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Male", "Female").forEach { g ->
                    val isSel = editGender == g
                    val displayName = if (g == "Male") "Male / نێر" else "Female / مێ"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) KurdPrimary else Color.Transparent)
                            .clickable { editGender = g }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName,
                            color = if (isSel) Color.White else Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = editNote,
            onValueChange = { editNote = it },
            label = { Text("Biography Notes") },
            leadingIcon = { Icon(Icons.Default.Note, contentDescription = null, tint = KurdPrimary, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            maxLines = 5
        )

        Button(
            onClick = { onUpdate(editName, editYear, editNote, editSpouse, editDaughters, editGender) },
            colors = ButtonDefaults.buttonColors(containerColor = KurdPrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("Update Inspector", color = Color.White)
            }
        }

        HorizontalDivider(color = KurdBorder)

        // Actions panel
        Text("OPTIONS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        Button(
            onClick = onAddRelative,
            colors = ButtonDefaults.buttonColors(containerColor = KurdDarkBlue),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("Add Relative Node", color = Color.White)
            }
        }

        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.12f), contentColor = Color.Red),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Text("Cascade Delete Node", color = Color.Red)
            }
        }
    }
}
