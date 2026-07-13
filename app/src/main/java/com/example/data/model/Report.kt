package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val status: String = "DILAPORKAN",
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
) : Serializable
