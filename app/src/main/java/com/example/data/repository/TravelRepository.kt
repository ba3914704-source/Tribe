package com.example.data.repository

import com.example.core.network.SupabaseRestApi
import com.example.data.remote.dto.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

interface TravelRepository {
    val destinationsFlow: Flow<List<Destination>>
    val toursFlow: Flow<List<Tour>>
    val notificationsFlow: Flow<List<NotificationItem>>

    suspend fun refreshData()
    suspend fun refreshUserNotifications(userId: String)
    suspend fun getTourById(id: String): Tour?
    suspend fun createPlan(
        ownerId: String,
        title: String,
        destinationName: String,
        startDate: String,
        endDate: String,
        budget: BigDecimal,
        isPublic: Boolean,
        description: String
    ): Result<Tour>

    suspend fun getItinerary(tourId: String): List<ItineraryItem>
    suspend fun addItineraryItem(tourId: String, userId: String, day: Int, time: String, location: String, title: String): Result<ItineraryItem>

    suspend fun getExpenses(tourId: String, userId: String?): List<Expense>
    suspend fun addExpense(
        tourId: String,
        userId: String,
        title: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        paidByName: String,
        paidById: String
    ): Result<Expense>

    suspend fun getExpenseSummary(tourId: String, currentUserName: String): ExpenseSummary
    suspend fun getSettlements(tourId: String): List<Settlement>
    suspend fun addSettlement(tourId: String, fromUser: String, toUser: String, amount: BigDecimal): Result<Settlement>

    suspend fun getTourMembers(tourId: String): List<TourMember>
    suspend fun addTourMember(tourId: String, memberName: String): Result<TourMember>

    suspend fun getTourComments(tourId: String, userId: String?): List<TourComment>
    suspend fun addComment(tourId: String, userId: String, text: String): Result<TourComment>

    suspend fun getTourGuides(): List<TourGuide>
    suspend fun refreshSavedTours(userId: String)
    suspend fun toggleSaveTour(tourId: String, userId: String?): Boolean
    suspend fun getChecklist(tourId: String): List<ChecklistItem>
    suspend fun toggleChecklistItem(tourId: String, itemId: String): Boolean
    suspend fun addChecklistItem(tourId: String, title: String): ChecklistItem
    suspend fun markNotificationRead(userId: String, id: String)
    suspend fun markAllNotificationsRead(userId: String)
}

class DefaultTravelRepository(
    private val restApi: SupabaseRestApi
) : TravelRepository {

    // In-memory persistent caches for responsive UX and offline robustness
    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    override val destinationsFlow: Flow<List<Destination>> = _destinations.asStateFlow()

    private val _tours = MutableStateFlow<List<Tour>>(emptyList())
    override val toursFlow: Flow<List<Tour>> = _tours.asStateFlow()

    private val _itineraryMap = MutableStateFlow<Map<String, List<ItineraryItem>>>(emptyMap())
    private val _expensesMap = MutableStateFlow<Map<String, List<Expense>>>(emptyMap())
    private val _membersMap = MutableStateFlow<Map<String, List<TourMember>>>(emptyMap())
    private val _commentsMap = MutableStateFlow<Map<String, List<TourComment>>>(emptyMap())
    private val _settlementsMap = MutableStateFlow<Map<String, List<Settlement>>>(emptyMap())

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    override val notificationsFlow: Flow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _checklistMap = MutableStateFlow<Map<String, List<ChecklistItem>>>(emptyMap())

    override suspend fun refreshData() {
        try {
            val destResponse = restApi.getDestinations()
            if (destResponse.isSuccessful) {
                val mapped = destResponse.body()!!.map { dto ->
                    Destination(
                        id = dto.id,
                        name = dto.name,
                        division = dto.region ?: dto.country,
                        district = "",
                        description = dto.description ?: "",
                        imageUrl = dto.imageUrl ?: "",
                        rating = 0.0,
                        isFeatured = dto.isFeatured ?: false,
                        category = ""
                    )
                }
                _destinations.value = mapped
            }
        } catch (_: Exception) {
            // Keep local data gracefully
        }

        try {
            val tourResponse = restApi.getTours()
            if (tourResponse.isSuccessful) {
                val mappedTours = tourResponse.body().orEmpty().map(::toTour)
                _tours.value = mappedTours
            }
        } catch (_: Exception) {
            // Keep local data gracefully
        }
    }

    override suspend fun refreshUserNotifications(userId: String) {
        runCatching {
            val response = restApi.getNotifications("eq.$userId")
            if (response.isSuccessful) {
                _notifications.value = response.body().orEmpty().map { dto ->
                    NotificationItem(
                        id = dto.id,
                        title = dto.title,
                        message = dto.body.orEmpty(),
                        isRead = dto.isRead ?: false,
                        createdAt = dto.createdAt.orEmpty()
                    )
                }
            }
        }
    }

    override suspend fun getTourById(id: String): Tour? {
        _tours.value.find { it.id == id }?.let { return it }
        return runCatching {
            val response = restApi.getTourById("eq.$id")
            response.body()?.firstOrNull()?.let(::toTour)
        }.getOrNull()
    }

    override suspend fun createPlan(
        ownerId: String,
        title: String,
        destinationName: String,
        startDate: String,
        endDate: String,
        budget: BigDecimal,
        isPublic: Boolean,
        description: String
    ): Result<Tour> {
        return try {
            val response = restApi.createTour(
                CreateTourDto(
                    ownerId = ownerId,
                    title = title,
                    description = description.takeIf { it.isNotBlank() },
                    destinationName = destinationName,
                    startDate = startDate,
                    endDate = endDate,
                    status = "upcoming",
                    visibility = if (isPublic) "public" else "private",
                    estimatedBudget = budget.toDouble()
                )
            )
            if (!response.isSuccessful) {
                Result.failure(IllegalStateException(response.errorBody()?.string() ?: "Tour could not be saved (${response.code()})"))
            } else {
                val created = response.body()?.firstOrNull()
                    ?: return Result.failure(IllegalStateException("The server did not return the created tour"))
                val tour = toTour(created)
                _tours.update { current -> listOf(tour) + current.filterNot { it.id == tour.id } }
                Result.success(tour)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun toTour(dto: TourDto) = Tour(
        id = dto.id,
        title = dto.title,
        description = dto.description.orEmpty(),
        destinationName = dto.destinationName.orEmpty(),
        startDate = dto.startDate.orEmpty(),
        endDate = dto.endDate.orEmpty(),
        status = when (dto.status?.lowercase()) {
            "ongoing" -> TourStatus.ONGOING
            "completed" -> TourStatus.COMPLETED
            else -> TourStatus.UPCOMING
        },
        isPublic = dto.visibility == "public",
        creatorId = dto.creatorId,
        imageUrl = dto.imageUrl.orEmpty(),
        estimatedBudget = BigDecimal(dto.estimatedBudget ?: 0.0),
        totalMembers = dto.memberStats?.activeMemberCount ?: 0,
        rating = 0.0,
        travelersCount = ""
    )

    override suspend fun getItinerary(tourId: String): List<ItineraryItem> {
        return try {
            val response = restApi.getItinerary("eq.$tourId")
            if (!response.isSuccessful) return emptyList()
            val items = response.body().orEmpty().map { dto ->
                ItineraryItem(
                    id = dto.id,
                    tourId = dto.tourId,
                    dayNumber = dto.dayNumber,
                    title = dto.title,
                    time = dto.startsAt?.substringAfter('T')?.take(5).orEmpty(),
                    location = dto.location.orEmpty(),
                    description = dto.description.orEmpty()
                )
            }
            _itineraryMap.update { it + (tourId to items) }
            items
        } catch (_: Exception) {
            _itineraryMap.value[tourId].orEmpty()
        }
    }

    override suspend fun addItineraryItem(
        tourId: String,
        userId: String,
        day: Int,
        time: String,
        location: String,
        title: String
    ): Result<ItineraryItem> {
        return try {
            val tourStart = _tours.value.firstOrNull { it.id == tourId }?.startDate.orEmpty()
            val timezone = TimeZone.getTimeZone("Asia/Dhaka")
            val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = timezone
            }
            val start = dateParser.parse(tourStart)
                ?: return Result.failure(IllegalArgumentException("This tour has no valid start date"))
            val dayCalendar = Calendar.getInstance(timezone).apply {
                setTime(start)
                add(Calendar.DAY_OF_MONTH, day - 1)
            }
            val date = dateParser.format(dayCalendar.time)
            val dateTimeParser = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).apply {
                isLenient = false
                timeZone = timezone
            }
            val startAt = dateTimeParser.parse("$date ${time.trim()}")
                ?: return Result.failure(IllegalArgumentException("Enter the time in a format such as 07:30 AM"))
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                timeZone = timezone
            }.format(startAt)
            val response = restApi.addItineraryItem(
                CreateItineraryDto(
                    tourId = tourId,
                    dayNumber = day,
                    title = title.trim(),
                    location = location.trim().takeIf(String::isNotBlank),
                    startsAt = timestamp,
                    createdBy = userId
                )
            )
            if (!response.isSuccessful) {
                Result.failure(IllegalStateException(response.errorBody()?.string() ?: "Activity could not be saved (${response.code()})"))
            } else {
                val dto = response.body()?.firstOrNull()
                    ?: return Result.failure(IllegalStateException("The server did not return the saved activity"))
                val item = ItineraryItem(
                    id = dto.id,
                    tourId = dto.tourId,
                    dayNumber = dto.dayNumber,
                    title = dto.title,
                    time = dto.startsAt?.substringAfter('T')?.take(5).orEmpty(),
                    location = dto.location.orEmpty(),
                    description = dto.description.orEmpty()
                )
                _itineraryMap.update { current -> current + (tourId to (current[tourId].orEmpty() + item)) }
                Result.success(item)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun getExpenses(tourId: String, userId: String?): List<Expense> {
        return try {
            val response = restApi.getTourExpenses("eq.$tourId")
            if (!response.isSuccessful) return _expensesMap.value[tourId].orEmpty()
            val expenses = response.body().orEmpty().map { dto ->
                Expense(
                    id = dto.id,
                    tourId = dto.tourId,
                    title = dto.title,
                    amount = dto.amount,
                    category = when (dto.category?.lowercase()) {
                        "transport" -> ExpenseCategory.TRANSPORT
                        "accommodation" -> ExpenseCategory.HOTEL
                        "food" -> ExpenseCategory.FOOD
                        "activity", "ticket" -> ExpenseCategory.ACTIVITIES
                        "shopping" -> ExpenseCategory.SHOPPING
                        else -> ExpenseCategory.GENERAL
                    },
                    paidByName = if (dto.paidById == userId) "You" else "Member",
                    paidById = dto.paidById,
                    date = dto.expenseDate.orEmpty()
                )
            }
            _expensesMap.update { it + (tourId to expenses) }
            expenses
        } catch (_: Exception) {
            _expensesMap.value[tourId].orEmpty()
        }
    }

    override suspend fun addExpense(
        tourId: String,
        userId: String,
        title: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        paidByName: String,
        paidById: String
    ): Result<Expense> {
        return try {
            val categoryValue = when (category) {
                ExpenseCategory.TRANSPORT -> "transport"
                ExpenseCategory.FOOD -> "food"
                ExpenseCategory.HOTEL -> "accommodation"
                ExpenseCategory.ACTIVITIES -> "activity"
                ExpenseCategory.SHOPPING -> "shopping"
                ExpenseCategory.GENERAL -> "other"
            }
            val response = restApi.addExpense(
                CreateExpenseDto(
                    tourId = tourId,
                    paidById = userId,
                    title = title.trim(),
                    amount = amount,
                    category = categoryValue
                )
            )
            if (!response.isSuccessful) {
                Result.failure(IllegalStateException(response.errorBody()?.string() ?: "Expense could not be saved (${response.code()})"))
            } else {
                val dto = response.body()?.firstOrNull()
                    ?: return Result.failure(IllegalStateException("The server did not return the saved expense"))
                val expense = Expense(
                    id = dto.id,
                    tourId = dto.tourId,
                    title = dto.title,
                    amount = dto.amount,
                    category = category,
                    paidByName = "You",
                    paidById = dto.paidById,
                    date = dto.expenseDate.orEmpty()
                )
                _expensesMap.update { current -> current + (tourId to (listOf(expense) + current[tourId].orEmpty())) }
                Result.success(expense)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun getExpenseSummary(tourId: String, currentUserName: String): ExpenseSummary {
        val expenses = _expensesMap.value[tourId] ?: emptyList()
        val total = expenses.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }

        val members = _membersMap.value[tourId] ?: emptyList()
        val memberCount = members.size
        val memberCountBd = BigDecimal(memberCount)

        val yourShare = if (memberCount > 0 && total > BigDecimal.ZERO) {
            total.divide(memberCountBd, 2, RoundingMode.HALF_EVEN)
        } else {
            BigDecimal.ZERO
        }

        val yourTotalPaid = expenses
            .filter { it.paidByName.equals("You", ignoreCase = true) || it.paidByName.equals(currentUserName, ignoreCase = true) }
            .fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }

        val netBalance = yourTotalPaid.subtract(yourShare)

        return ExpenseSummary(
            totalExpense = total,
            yourShare = yourShare,
            yourTotalPaid = yourTotalPaid,
            netBalance = netBalance
        )
    }

    override suspend fun getSettlements(tourId: String): List<Settlement> {
        return _settlementsMap.value[tourId].orEmpty()
    }

    override suspend fun addSettlement(
        tourId: String,
        fromUser: String,
        toUser: String,
        amount: BigDecimal
    ): Result<Settlement> {
        val settlement = Settlement(
            id = UUID.randomUUID().toString(),
            tourId = tourId,
            fromUserName = fromUser,
            toUserName = toUser,
            amount = amount,
            isSettled = true
        )
        val map = _settlementsMap.value.toMutableMap()
        val list = map[tourId]?.toMutableList() ?: mutableListOf()
        list.add(0, settlement)
        map[tourId] = list
        _settlementsMap.value = map
        return Result.success(settlement)
    }

    override suspend fun getTourMembers(tourId: String): List<TourMember> {
        return try {
            val response = restApi.getTourMembers("eq.$tourId")
            if (!response.isSuccessful) return _membersMap.value[tourId].orEmpty()
            val members = response.body().orEmpty().map { dto ->
                TourMember(
                    id = dto.id,
                    tourId = dto.tourId,
                    userId = dto.userId,
                    role = dto.role.orEmpty(),
                    status = dto.status.orEmpty(),
                    userName = dto.userName ?: "Member",
                    avatarUrl = dto.avatarUrl
                )
            }
            _membersMap.update { it + (tourId to members) }
            members
        } catch (_: Exception) {
            _membersMap.value[tourId].orEmpty()
        }
    }

    override suspend fun addTourMember(tourId: String, memberName: String): Result<TourMember> {
        val member = TourMember(
            id = UUID.randomUUID().toString(),
            tourId = tourId,
            userId = UUID.randomUUID().toString(),
            role = "member",
            status = "accepted",
            userName = memberName,
            avatarUrl = null
        )
        val map = _membersMap.value.toMutableMap()
        val list = map[tourId]?.toMutableList() ?: mutableListOf()
        list.add(member)
        map[tourId] = list
        _membersMap.value = map

        // Update tour member count
        val tours = _tours.value.map { tour ->
            if (tour.id == tourId) tour.copy(totalMembers = tour.totalMembers + 1) else tour
        }
        _tours.value = tours

        return Result.success(member)
    }

    override suspend fun getTourComments(tourId: String, userId: String?): List<TourComment> {
        return try {
            val response = restApi.getTourComments("eq.$tourId")
            if (!response.isSuccessful) return _commentsMap.value[tourId].orEmpty()
            val comments = response.body().orEmpty().map { dto ->
                TourComment(
                    id = dto.id,
                    tourId = dto.tourId,
                    userName = if (dto.userId == userId) "You" else "Member",
                    avatarUrl = null,
                    comment = dto.body,
                    createdAt = dto.createdAt.orEmpty()
                )
            }
            _commentsMap.update { it + (tourId to comments) }
            comments
        } catch (_: Exception) {
            _commentsMap.value[tourId].orEmpty()
        }
    }

    override suspend fun addComment(tourId: String, userId: String, text: String): Result<TourComment> {
        return try {
            val response = restApi.addComment(CreateCommentDto(tourId = tourId, userId = userId, body = text.trim()))
            if (!response.isSuccessful) {
                Result.failure(IllegalStateException(response.errorBody()?.string() ?: "Comment could not be saved (${response.code()})"))
            } else {
                val dto = response.body()?.firstOrNull()
                    ?: return Result.failure(IllegalStateException("The server did not return the saved comment"))
                val comment = TourComment(
                    id = dto.id,
                    tourId = dto.tourId,
                    userName = "You",
                    avatarUrl = null,
                    comment = dto.body,
                    createdAt = dto.createdAt.orEmpty()
                )
                _commentsMap.update { current -> current + (tourId to (listOf(comment) + current[tourId].orEmpty())) }
                Result.success(comment)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun getTourGuides(): List<TourGuide> {
        return emptyList()
    }

    override suspend fun refreshSavedTours(userId: String) {
        runCatching {
            val response = restApi.getSavedTours("eq.$userId")
            if (response.isSuccessful) {
                val savedIds = response.body().orEmpty().mapTo(hashSetOf()) { it.tourId }
                _tours.update { tours -> tours.map { it.copy(isSaved = it.id in savedIds) } }
            }
        }
    }

    override suspend fun toggleSaveTour(tourId: String, userId: String?): Boolean {
        val currentTour = _tours.value.firstOrNull { it.id == tourId } ?: return false
        if (userId.isNullOrBlank()) return currentTour.isSaved
        val response = runCatching {
            if (currentTour.isSaved) {
                restApi.unsaveTour("eq.$tourId", "eq.$userId")
            } else {
                restApi.saveTour(SaveTourRequestDto(tourId = tourId, userId = userId))
            }
        }.getOrNull() ?: return currentTour.isSaved
        if (!response.isSuccessful) return currentTour.isSaved
        val saved = !currentTour.isSaved
        _tours.update { tours -> tours.map { if (it.id == tourId) it.copy(isSaved = saved) else it } }
        return saved
    }

    override suspend fun getChecklist(tourId: String): List<ChecklistItem> {
        return _checklistMap.value[tourId].orEmpty()
    }

    override suspend fun toggleChecklistItem(tourId: String, itemId: String): Boolean {
        val map = _checklistMap.value.toMutableMap()
        val currentList = (map[tourId] ?: emptyList()).toMutableList()
        var newStatus = false
        val updatedList = currentList.map { item ->
            if (item.id == itemId) {
                newStatus = !item.isCompleted
                item.copy(isCompleted = newStatus)
            } else item
        }
        map[tourId] = updatedList
        _checklistMap.value = map
        return newStatus
    }

    override suspend fun addChecklistItem(tourId: String, title: String): ChecklistItem {
        val item = ChecklistItem(
            id = UUID.randomUUID().toString(),
            title = title,
            isCompleted = false
        )
        val map = _checklistMap.value.toMutableMap()
        val currentList = (map[tourId] ?: emptyList()).toMutableList()
        currentList.add(item)
        map[tourId] = currentList
        _checklistMap.value = map
        return item
    }

    override suspend fun markNotificationRead(userId: String, id: String) {
        val response = runCatching { restApi.markNotificationRead("eq.$id", mapOf("is_read" to true)) }.getOrNull()
        if (response?.isSuccessful == true) {
            _notifications.update { items -> items.map { if (it.id == id) it.copy(isRead = true) else it } }
        }
    }

    override suspend fun markAllNotificationsRead(userId: String) {
        val response = runCatching { restApi.markNotificationsRead("eq.$userId", mapOf("is_read" to true)) }.getOrNull()
        if (response?.isSuccessful == true) {
            _notifications.update { items -> items.map { it.copy(isRead = true) } }
        }
    }

}
