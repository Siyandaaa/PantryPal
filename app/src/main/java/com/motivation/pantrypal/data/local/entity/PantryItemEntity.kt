package com.motivation.pantrypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The single source of truth for "What's in My Pantry?" - PantryPal's pantry data lives only on-device. */
@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val quantity: Double,
    val unit: String?,
    val category: String,
    val expiryDate: String? = null,
    val location: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
