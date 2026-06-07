package com.jishiyong.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_memories",
    indices = [Index(value = ["type", "key"])]
)
data class AgentMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val key: String,
    val valueJson: String,
    val searchableText: String,
    val confidence: Float,
    val hits: Int,
    val updatedAt: Long
)

@Fts4
@Entity(tableName = "agent_memory_fts")
data class AgentMemoryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val searchableText: String
)
