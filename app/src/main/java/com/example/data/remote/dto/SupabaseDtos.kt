package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Auth DTOs
@JsonClass(generateAdapter = true)
data class EmailPasswordRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class IdTokenRequest(
    val provider: String = "google",
    @Json(name = "id_token") val idToken: String,
    val nonce: String? = null
)

@JsonClass(generateAdapter = true)
data class RecoverPasswordRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val user: SupabaseUserDto? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    val id: String,
    val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

// Profile DTO
@JsonClass(generateAdapter = true)
data class ProfileDto(
    val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileDto(
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
    val bio: String? = null
)

// Destination DTO
@JsonClass(generateAdapter = true)
data class DestinationDto(
    val id: String,
    val name: String,
    val division: String? = null,
    val district: String? = null,
    val description: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val rating: Double? = 4.8,
    @Json(name = "is_featured") val isFeatured: Boolean? = false,
    val category: String? = "Hills"
)

// Tour / Plan DTO
@JsonClass(generateAdapter = true)
data class TourDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @Json(name = "destination_id") val destinationId: String? = null,
    @Json(name = "destination_name") val destinationName: String? = null,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null,
    val status: String? = "upcoming", // upcoming, ongoing, completed
    @Json(name = "is_public") val isPublic: Boolean? = true,
    @Json(name = "creator_id") val creatorId: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "estimated_budget") val estimatedBudget: Double? = 0.0,
    @Json(name = "total_members") val totalMembers: Int? = 1,
    val rating: Double? = 4.8,
    @Json(name = "travelers_count") val travelersCount: String? = "1.2k+"
)

@JsonClass(generateAdapter = true)
data class CreateTourDto(
    val title: String,
    val description: String? = null,
    @Json(name = "destination_name") val destinationName: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    val status: String = "upcoming",
    @Json(name = "is_public") val isPublic: Boolean = false,
    @Json(name = "estimated_budget") val estimatedBudget: Double = 0.0,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateTourDto(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    @Json(name = "is_public") val isPublic: Boolean? = null
)

// Tour Member DTO
@JsonClass(generateAdapter = true)
data class TourMemberDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_id") val userId: String,
    val role: String? = "member", // owner, admin, member
    val status: String? = "accepted",
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class AddMemberDto(
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_id") val userId: String,
    val role: String = "member",
    val status: String = "invited"
)

// Itinerary DTO
@JsonClass(generateAdapter = true)
data class TourItineraryDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "day_number") val dayNumber: Int,
    val title: String,
    val time: String? = null,
    val location: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateItineraryDto(
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "day_number") val dayNumber: Int,
    val title: String,
    val time: String? = null,
    val location: String? = null,
    val description: String? = null
)

// Expenses DTO
@JsonClass(generateAdapter = true)
data class TourExpenseDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    val title: String,
    val amount: Double,
    val category: String? = "General",
    @Json(name = "paid_by_name") val paidByName: String? = null,
    @Json(name = "paid_by_id") val paidById: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateExpenseDto(
    @Json(name = "tour_id") val tourId: String,
    val title: String,
    val amount: Double,
    val category: String = "General",
    @Json(name = "paid_by_name") val paidByName: String,
    @Json(name = "paid_by_id") val paidById: String
)

@JsonClass(generateAdapter = true)
data class ExpenseSplitDto(
    val id: String,
    @Json(name = "expense_id") val expenseId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "split_amount") val splitAmount: Double,
    @Json(name = "is_settled") val isSettled: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class TourContributionDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_id") val userId: String,
    val amount: Double,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SettlementDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "from_user_name") val fromUserName: String,
    @Json(name = "to_user_name") val toUserName: String,
    val amount: Double,
    @Json(name = "is_settled") val isSettled: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class CreateSettlementDto(
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "from_user_name") val fromUserName: String,
    @Json(name = "to_user_name") val toUserName: String,
    val amount: Double,
    @Json(name = "is_settled") val isSettled: Boolean = true
)

// Tour Guide DTO
@JsonClass(generateAdapter = true)
data class TourGuideDto(
    val id: String,
    val name: String,
    val region: String? = null,
    val experience: String? = null,
    val languages: String? = null,
    val phone: String? = null,
    val rating: Double? = 4.9,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

// Tour Comments DTO
@JsonClass(generateAdapter = true)
data class TourCommentDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val comment: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateCommentDto(
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_name") val userName: String,
    val comment: String
)

// Saved Tours DTO
@JsonClass(generateAdapter = true)
data class SavedTourDto(
    val id: String,
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SaveTourRequestDto(
    @Json(name = "tour_id") val tourId: String,
    @Json(name = "user_id") val userId: String
)

// Notification DTO
@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val title: String,
    val message: String,
    @Json(name = "is_read") val isRead: Boolean? = false,
    @Json(name = "created_at") val createdAt: String? = null
)
