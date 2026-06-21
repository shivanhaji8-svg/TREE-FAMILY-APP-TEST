package com.example.data

import kotlinx.coroutines.flow.Flow

class FamilyRepository(private val dao: FamilyMemberDao) {
    val allMembers: Flow<List<FamilyMember>> = dao.getAllMembersFlow()

    suspend fun insert(member: FamilyMember): Long {
        return dao.insertMember(member)
    }

    suspend fun update(member: FamilyMember) {
        dao.updateMember(member)
    }

    suspend fun deleteWithDescendants(member: FamilyMember) {
        val all = dao.getAllMembers()
        val toDelete = mutableSetOf<Long>()
        
        // Local recursive helper to collect all descendant IDs
        fun collectDescendants(parentId: Long) {
            toDelete.add(parentId)
            val children = all.filter { it.parentId == parentId }
            children.forEach { collectDescendants(it.id) }
        }

        collectDescendants(member.id)

        // Delete each collected member
        for (id in toDelete) {
            val mem = all.firstOrNull { it.id == id }
            if (mem != null) {
                dao.deleteMember(mem)
            }
        }
    }

    suspend fun importTree(members: List<FamilyMember>) {
        dao.insertAll(members)
    }

    suspend fun importTreeForProject(projectId: Int, members: List<FamilyMember>) {
        dao.insertAllForProject(projectId, members)
    }

    suspend fun getAllRawMembers(): List<FamilyMember> {
        return dao.getAllMembers()
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun clearProject(projectId: Int) {
        dao.deleteByProject(projectId)
    }
}
