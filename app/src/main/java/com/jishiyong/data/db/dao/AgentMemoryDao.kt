package com.jishiyong.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jishiyong.data.db.entity.AgentMemoryEntity
import com.jishiyong.data.db.entity.AgentMemoryFtsEntity

@Dao
interface AgentMemoryDao {

    @Query("SELECT * FROM agent_memories ORDER BY updatedAt DESC")
    suspend fun getAllMemories(): List<AgentMemoryEntity>

    @Query(
        """
        SELECT m.* FROM agent_memories AS m
        INNER JOIN agent_memory_fts AS fts ON m.id = fts.rowid
        WHERE agent_memory_fts MATCH :query
        ORDER BY m.hits DESC, m.updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchMemories(query: String, limit: Int): List<AgentMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<AgentMemoryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFtsEntries(entries: List<AgentMemoryFtsEntity>)

    @Query("DELETE FROM agent_memory_fts")
    suspend fun clearFtsEntries()

    @Query("DELETE FROM agent_memories")
    suspend fun clearMemories()

    @Transaction
    suspend fun replaceAllMemories(memories: List<AgentMemoryEntity>) {
        clearFtsEntries()
        clearMemories()
        if (memories.isEmpty()) return

        val ids = insertMemories(memories.map { it.copy(id = 0) })
        insertFtsEntries(
            ids.zip(memories).map { (id, memory) ->
                AgentMemoryFtsEntity(
                    rowId = id,
                    searchableText = memory.searchableText
                )
            }
        )
    }
}
