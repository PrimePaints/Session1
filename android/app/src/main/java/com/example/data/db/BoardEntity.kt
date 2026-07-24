package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
