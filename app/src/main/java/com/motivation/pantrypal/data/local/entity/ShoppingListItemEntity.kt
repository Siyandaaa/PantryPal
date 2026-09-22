package com.motivation.pantrypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: String?,
    val aisle: String,
    val isChecked: Boolean = false,
    val isManuallyAdded: Boolean = false
)
