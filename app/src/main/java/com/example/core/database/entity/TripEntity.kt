package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Database entity representing a Trip for local trip management and offline persistence.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "trip_name")
    val tripName: String,

    @ColumnInfo(name = "destination")
    val destination: String,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "end_date")
    val endDate: String,

    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "status")
    val status: String = "upcoming", // upcoming, ongoing, completed

    @ColumnInfo(name = "budget")
    val budget: Double = 0.0,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
