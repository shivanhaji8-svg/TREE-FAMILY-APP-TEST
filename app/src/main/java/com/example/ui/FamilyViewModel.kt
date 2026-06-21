package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FamilyMember
import com.example.data.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ViewMode {
    TREE, TABLE, OUTLINE
}

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FamilyRepository
    private val sharedPrefs = application.getSharedPreferences("family_kurd_projects", Context.MODE_PRIVATE)

    // Project Workspace Tracking Flow
    private val _currentProjectId = MutableStateFlow(1)
    val currentProjectId = _currentProjectId.asStateFlow()

    private val _createdProjectIds = MutableStateFlow<List<Int>>(listOf(1))
    val createdProjectIds = _createdProjectIds.asStateFlow()

    private val _projectNames = MutableStateFlow<Map<Int, String>>(emptyMap())
    val projectNames = _projectNames.asStateFlow()

    // Raw flow from Room
    val allMembers: StateFlow<List<FamilyMember>>

    // Search query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered members for Search & Table View
    val filteredMembers: StateFlow<List<FamilyMember>>

    // Selected member for detail view / editing
    private val _selectedMember = MutableStateFlow<FamilyMember?>(null)
    val selectedMember = _selectedMember.asStateFlow()

    // Modern active view state (Tree or Table)
    private val _viewMode = MutableStateFlow(ViewMode.TREE)
    val viewMode = _viewMode.asStateFlow()

    // Screen Mode (focus view workspace, hides top bar / dashboard panel)
    private val _isScreenMode = MutableStateFlow(false)
    val isScreenMode = _isScreenMode.asStateFlow()

    // Gender Filter state flow (State 1: Show All, State 2: Male Only)
    private val _showMaleOnly = MutableStateFlow(false)
    val showMaleOnly = _showMaleOnly.asStateFlow()

    fun setShowMaleOnly(enabled: Boolean) {
        _showMaleOnly.value = enabled
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FamilyRepository(db.familyMemberDao())

        // Load created projects lists
        val savedIdsStr = sharedPrefs.getString("created_project_ids", "1") ?: "1"
        var ids = savedIdsStr.split(",").mapNotNull { it.toIntOrNull() }.filter { it in 1..4 }.distinct()
        if (ids.isEmpty()) ids = listOf(1)
        _createdProjectIds.value = ids

        // Load active project slot
        val activeId = sharedPrefs.getInt("active_project_id", 1)
        _currentProjectId.value = if (activeId in ids) activeId else ids.first()

        // Load project names
        val names = mutableMapOf<Int, String>()
        for (i in 1..4) {
            val key = "project_name_$i"
            val defaultName = when (i) {
                1 -> "Ahmad Haji Ancestry"
                2 -> "Jafar Descendants"
                3 -> "Kurdish Tribal Heritage"
                else -> "New Family Tree $i"
            }
            names[i] = sharedPrefs.getString(key, defaultName) ?: defaultName
        }
        _projectNames.value = names

        // Project-scoped filtering of allMembers Flow
        allMembers = combine(repository.allMembers, _currentProjectId) { members, activeId ->
            members.filter { it.projectId == activeId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Combine search query and members list to serve search functionality
        filteredMembers = combine(allMembers, _searchQuery) { members, query ->
            if (query.isBlank()) {
                members
            } else {
                members.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.birthYear.contains(query, ignoreCase = true) ||
                    it.note.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Prepopulate default Kurdish lineage if database is completely empty
        viewModelScope.launch {
            val raw = repository.getAllRawMembers()
            if (raw.isEmpty()) {
                prepopulateDefaultTree()
            }
        }
    }

    private suspend fun prepopulateDefaultTree() {
        val roots = listOf(
            FamilyMember(1, "Ahmad Haji", null, "1920", "The patriarch of the Haji family. Respected elder of the community.", 0, spouse = "Fatma Haji", daughters = "Zeynab Haji, Maryam Haji", projectId = 1),
            FamilyMember(2, "Mustafa Ahmad", 1, "1950", "First-born son of Ahmad. Local representative and peacemaker.", 1, spouse = "Amina Mustafa", daughters = "Avan Mustafa, Shirin Mustafa", projectId = 1),
            FamilyMember(3, "Kamil Ahmad", 1, "1955", "Second son of Ahmad. Respected educator and language instructor.", 1, spouse = "Nasrin Kamil", daughters = "Darya Kamil", projectId = 1),
            FamilyMember(4, "Shivan Mustafa", 2, "1980", "Son of Mustafa, grandson of Ahmad. Senior UI/UX Developer and developer of Family Kurd.", 2, spouse = "Layla Shivan", daughters = "Dunya Shivan", projectId = 1),
            FamilyMember(5, "Rebin Mustafa", 2, "1985", "Second son of Mustafa. Creative architectural engineer.", 2, spouse = "Chnar Rebin", daughters = "Rojin Rebin", projectId = 1),
            FamilyMember(6, "Alan Kamil", 3, "1990", "Son of Kamil. Dedicated medical clinician and surgeon.", 2, spouse = "Sazan Alan", daughters = "Hevi Alan", projectId = 1)
        )
        repository.importTree(roots)
    }

    fun setProject(projectId: Int) {
        if (projectId in _createdProjectIds.value) {
            _currentProjectId.value = projectId
            sharedPrefs.edit().putInt("active_project_id", projectId).apply()
            // Reset selection when switching projects
            _selectedMember.value = null
        }
    }

    fun renameProject(projectId: Int, name: String) {
        if (name.isNotBlank()) {
            val updated = _projectNames.value.toMutableMap()
            updated[projectId] = name.trim()
            _projectNames.value = updated
            sharedPrefs.edit().putString("project_name_$projectId", name.trim()).apply()
        }
    }

    fun createNewProject(name: String): Boolean {
        val currentList = _createdProjectIds.value
        if (currentList.size >= 4) return false
        val nextId = (1..4).firstOrNull { it !in currentList } ?: return false
        
        val updatedName = name.trim().ifEmpty { "New Family Tree $nextId" }
        renameProject(nextId, updatedName)

        val newList = currentList + nextId
        _createdProjectIds.value = newList
        sharedPrefs.edit().putString("created_project_ids", newList.joinToString(",")).apply()
        
        // Auto-switch workspace
        setProject(nextId)
        return true
    }

    fun deleteProject(projectId: Int) {
        val currentList = _createdProjectIds.value
        if (currentList.size <= 1) return
        if (projectId !in currentList) return
        
        viewModelScope.launch {
            repository.clearProject(projectId)
        }
        
        val newList = currentList.filter { it != projectId }
        _createdProjectIds.value = newList
        sharedPrefs.edit().putString("created_project_ids", newList.joinToString(",")).apply()
        
        if (_currentProjectId.value == projectId) {
            val fallback = newList.first()
            setProject(fallback)
        }
    }

    fun selectMember(member: FamilyMember?) {
        _selectedMember.value = member
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setScreenMode(enabled: Boolean) {
        _isScreenMode.value = enabled
    }

    fun addSon(parent: FamilyMember, name: String, birthYear: String, note: String) {
        viewModelScope.launch {
            val son = FamilyMember(
                name = name.trim().ifEmpty { "Son of ${parent.name}" },
                parentId = parent.id,
                birthYear = birthYear.trim(),
                note = note.trim(),
                generation = parent.generation + 1,
                projectId = _currentProjectId.value
            )
            val newId = repository.insert(son)
            // Auto select the newly created descendant
            val added = repository.getAllRawMembers().firstOrNull { it.id == newId }
            if (added != null) {
                _selectedMember.value = added
            }
        }
    }

    fun addRoot(name: String, birthYear: String, note: String) {
        viewModelScope.launch {
            val root = FamilyMember(
                name = name.trim().ifEmpty { "Ancestor Patriarch" },
                parentId = null,
                birthYear = birthYear.trim(),
                note = note.trim(),
                generation = 0,
                projectId = _currentProjectId.value
            )
            repository.insert(root)
        }
    }

    fun updateMember(
        member: FamilyMember,
        name: String,
        birthYear: String,
        note: String,
        isExpanded: Boolean = member.isExpanded,
        spouse: String = member.spouse,
        daughters: String = member.daughters,
        gender: String = member.gender
    ) {
        viewModelScope.launch {
            val updated = member.copy(
                name = name.trim().ifEmpty { member.name },
                birthYear = birthYear.trim(),
                note = note.trim(),
                isExpanded = isExpanded,
                spouse = spouse.trim(),
                daughters = daughters.trim(),
                gender = gender
            )
            repository.update(updated)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = updated
            }
        }
    }

    fun toggleExpanded(member: FamilyMember) {
        viewModelScope.launch {
            val updated = member.copy(isExpanded = !member.isExpanded)
            repository.update(updated)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = updated
            }
        }
    }

    fun expandAll() {
        viewModelScope.launch {
            val all = repository.getAllRawMembers()
            all.forEach { m ->
                if (m.projectId == _currentProjectId.value && !m.isExpanded) {
                    repository.update(m.copy(isExpanded = true))
                }
            }
        }
    }

    fun collapseAll() {
        viewModelScope.launch {
            val all = repository.getAllRawMembers()
            all.forEach { m ->
                if (m.projectId == _currentProjectId.value && m.isExpanded) {
                    repository.update(m.copy(isExpanded = false))
                }
            }
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteWithDescendants(member)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = null
            }
        }
    }

    fun addSpouse(member: FamilyMember, spouseName: String) {
        viewModelScope.launch {
            val updated = member.copy(spouse = spouseName.trim())
            repository.update(updated)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = updated
            }
        }
    }

    fun deleteSpouse(member: FamilyMember) {
        viewModelScope.launch {
            val updated = member.copy(spouse = "")
            repository.update(updated)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = updated
            }
        }
    }

    fun addDaughter(member: FamilyMember, daughterName: String) {
        viewModelScope.launch {
            val currentDaughters = member.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val updatedList = currentDaughters + daughterName.trim()
            val updated = member.copy(daughters = updatedList.joinToString(", "))
            repository.update(updated)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = updated
            }
        }
    }

    fun deleteDaughter(member: FamilyMember, index: Int) {
        viewModelScope.launch {
            val currentDaughters = member.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (index in currentDaughters.indices) {
                val updatedList = currentDaughters.toMutableList().apply { removeAt(index) }
                val updated = member.copy(daughters = updatedList.joinToString(", "))
                repository.update(updated)
                if (_selectedMember.value?.id == member.id) {
                    _selectedMember.value = updated
                }
            }
        }
    }

    fun addBrother(member: FamilyMember, name: String, birthYear: String, note: String) {
        viewModelScope.launch {
            val brother = FamilyMember(
                name = name.trim().ifEmpty { "Brother of ${member.name}" },
                parentId = member.parentId,
                birthYear = birthYear.trim(),
                note = note.trim(),
                generation = member.generation,
                projectId = _currentProjectId.value
            )
            repository.insert(brother)
        }
    }

    fun addSister(member: FamilyMember, name: String) {
        viewModelScope.launch {
            val parentId = member.parentId ?: return@launch
            // Find parent to append daughter
            val parent = repository.getAllRawMembers().firstOrNull { it.id == parentId }
            if (parent != null) {
                addDaughter(parent, name)
            }
        }
    }

    fun clearTree() {
        viewModelScope.launch {
            repository.clearProject(_currentProjectId.value)
            _selectedMember.value = null
        }
    }

    fun exportToJson(): String {
        val members = allMembers.value
        val array = JSONArray()
        members.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            obj.put("parentId", m.parentId ?: JSONObject.NULL)
            obj.put("birthYear", m.birthYear)
            obj.put("note", m.note)
            obj.put("generation", m.generation)
            obj.put("isExpanded", m.isExpanded)
            obj.put("spouse", m.spouse)
            obj.put("daughters", m.daughters)
            obj.put("projectId", m.projectId)
            obj.put("gender", m.gender)
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importTreeFromJson(jsonString: String): Boolean {
        return try {
            val list = mutableListOf<FamilyMember>()
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optLong("id", 0)
                val name = obj.getString("name")
                val parentId = if (obj.isNull("parentId")) null else obj.getLong("parentId")
                val birthYear = obj.optString("birthYear", "")
                val note = obj.optString("note", "")
                val generation = obj.optInt("generation", 0)
                val isExpanded = obj.optBoolean("isExpanded", true)
                val spouse = obj.optString("spouse", "")
                val daughters = obj.optString("daughters", "")
                val projectId = obj.optInt("projectId", _currentProjectId.value)
                val gender = obj.optString("gender", "Male")
                list.add(FamilyMember(id, name, parentId, birthYear, note, generation, isExpanded, spouse, daughters, projectId, gender))
            }
            viewModelScope.launch {
                repository.importTreeForProject(_currentProjectId.value, list)
                _selectedMember.value = null
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
