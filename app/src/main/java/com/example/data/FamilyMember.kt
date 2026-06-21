package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long?, // Null represents an ancestral root
    val birthYear: String = "",
    val note: String = "",
    val generation: Int = 0,
    val isExpanded: Boolean = true, // Used in table/tree collapse states
    val spouse: String = "", // Spouse details (Husband / Wife)
    val daughters: String = "", // Non-branching daughters list / text representation
    val projectId: Int = 1, // Support up to 4 parallel custom workspaces (1, 2, 3, or 4)
    val gender: String = "Male"
)
