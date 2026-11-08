package com.example.core.network

import com.example.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SupabaseAuthApi {
    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Body request: EmailPasswordRequest
    ): Response<AuthResponseDto>

    @POST("auth/v1/signup")
    suspend fun signUpWithEmail(
        @Body request: SignUpRequest
    ): Response<AuthResponseDto>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponseDto>

    @POST("auth/v1/token?grant_type=id_token")
    suspend fun signInWithIdToken(
        @Body request: IdTokenRequest
    ): Response<AuthResponseDto>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body request: RecoverPasswordRequest
    ): Response<ResponseBody>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(): Response<SupabaseUserDto>

    @POST("auth/v1/logout")
    suspend fun logout(): Response<ResponseBody>
}

interface SupabaseRestApi {
    // Destinations
    @GET("rest/v1/destinations")
    suspend fun getDestinations(
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): Response<List<DestinationDto>>

    @GET("rest/v1/destinations")
    suspend fun getDestinationById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Response<List<DestinationDto>>

    // Tours / Plans
    @GET("rest/v1/tours")
    suspend fun getTours(
        @Query("select") select: String = "*",
        @Query("order") order: String = "start_date.asc"
    ): Response<List<TourDto>>

    @GET("rest/v1/tours")
    suspend fun getTourById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Response<List<TourDto>>

    @POST("rest/v1/tours")
    @Headers("Prefer: return=representation")
    suspend fun createTour(
        @Body tour: CreateTourDto
    ): Response<List<TourDto>>

    @PATCH("rest/v1/tours")
    @Headers("Prefer: return=representation")
    suspend fun updateTour(
        @Query("id") id: String,
        @Body tour: UpdateTourDto
    ): Response<List<TourDto>>

    @DELETE("rest/v1/tours")
    suspend fun deleteTour(
        @Query("id") id: String
    ): Response<ResponseBody>

    // Tour Members
    @GET("rest/v1/tour_members")
    suspend fun getTourMembers(
        @Query("tour_id") tourId: String,
        @Query("select") select: String = "*"
    ): Response<List<TourMemberDto>>

    @POST("rest/v1/tour_members")
    @Headers("Prefer: return=representation")
    suspend fun addTourMember(
        @Body member: AddMemberDto
    ): Response<List<TourMemberDto>>

    // Tour Itinerary
    @GET("rest/v1/tour_itinerary")
    suspend fun getItinerary(
        @Query("tour_id") tourId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "day_number.asc,time.asc"
    ): Response<List<TourItineraryDto>>

    @POST("rest/v1/tour_itinerary")
    @Headers("Prefer: return=representation")
    suspend fun addItineraryItem(
        @Body item: CreateItineraryDto
    ): Response<List<TourItineraryDto>>

    // Expenses
    @GET("rest/v1/tour_expenses")
    suspend fun getTourExpenses(
        @Query("tour_id") tourId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<TourExpenseDto>>

    @POST("rest/v1/tour_expenses")
    @Headers("Prefer: return=representation")
    suspend fun addExpense(
        @Body expense: CreateExpenseDto
    ): Response<List<TourExpenseDto>>

    // Settlements
    @GET("rest/v1/settlements")
    suspend fun getSettlements(
        @Query("tour_id") tourId: String,
        @Query("select") select: String = "*"
    ): Response<List<SettlementDto>>

    @POST("rest/v1/settlements")
    @Headers("Prefer: return=representation")
    suspend fun addSettlement(
        @Body settlement: CreateSettlementDto
    ): Response<List<SettlementDto>>

    // Tour Guides
    @GET("rest/v1/tour_guides")
    suspend fun getTourGuides(
        @Query("select") select: String = "*"
    ): Response<List<TourGuideDto>>

    // Tour Comments
    @GET("rest/v1/tour_comments")
    suspend fun getTourComments(
        @Query("tour_id") tourId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<TourCommentDto>>

    @POST("rest/v1/tour_comments")
    @Headers("Prefer: return=representation")
    suspend fun addComment(
        @Body comment: CreateCommentDto
    ): Response<List<TourCommentDto>>

    // Saved Tours
    @GET("rest/v1/saved_tours")
    suspend fun getSavedTours(
        @Query("user_id") userId: String,
        @Query("select") select: String = "*"
    ): Response<List<SavedTourDto>>

    @POST("rest/v1/saved_tours")
    @Headers("Prefer: return=representation")
    suspend fun saveTour(
        @Body saved: SaveTourRequestDto
    ): Response<List<SavedTourDto>>

    @DELETE("rest/v1/saved_tours")
    suspend fun unsaveTour(
        @Query("tour_id") tourId: String,
        @Query("user_id") userId: String
    ): Response<ResponseBody>

    // Notifications
    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Query("user_id") userId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<NotificationDto>>

    @PATCH("rest/v1/notifications")
    @Headers("Prefer: return=representation")
    suspend fun markNotificationRead(
        @Query("id") id: String,
        @Body update: Map<String, Boolean>
    ): Response<List<NotificationDto>>

    // Profile
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>

    @PATCH("rest/v1/profiles")
    @Headers("Prefer: return=representation")
    suspend fun updateProfile(
        @Query("id") id: String,
        @Body profile: UpdateProfileDto
    ): Response<List<ProfileDto>>
}
