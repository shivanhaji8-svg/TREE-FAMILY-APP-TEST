package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY generation ASC, id ASC")
    fun getAllMembersFlow(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members")
    suspend fun getAllMembers(): List<FamilyMember>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("DELETE FROM family_members")
    suspend fun deleteAll()

    @Query("DELETE FROM family_members WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Int)

    @Transaction
    suspend fun insertAll(members: List<FamilyMember>) {
        // Clear all existing members and inject the incoming collection
        deleteAll()
        members.forEach { 
            // We use simple field insertion.
            insertMember(it)
        }
    }

    @Transaction
    suspend fun insertAllForProject(projectId: Int, members: List<FamilyMember>) {
        deleteByProject(projectId)
        members.forEach {
            insertMember(it.copy(projectId = projectId))
        }
    }
}
