package com.example.data.repository

import com.example.core.network.SupabaseRestApi
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

interface TravelRepository {
    val destinationsFlow: Flow<List<Destination>>
    val toursFlow: Flow<List<Tour>>
    val notificationsFlow: Flow<List<NotificationItem>>

    suspend fun refreshData()
    suspend fun getTourById(id: String): Tour?
    suspend fun createPlan(
        title: String,
        destinationName: String,
        startDate: String,
        endDate: String,
        budget: BigDecimal,
        isPublic: Boolean,
        description: String
    ): Result<Tour>

    suspend fun getItinerary(tourId: String): List<ItineraryItem>
    suspend fun addItineraryItem(tourId: String, day: Int, time: String, location: String, title: String): Result<ItineraryItem>

    suspend fun getExpenses(tourId: String): List<Expense>
    suspend fun addExpense(
        tourId: String,
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

    suspend fun getTourComments(tourId: String): List<TourComment>
    suspend fun addComment(tourId: String, userName: String, text: String): Result<TourComment>

    suspend fun getTourGuides(): List<TourGuide>
    suspend fun toggleSaveTour(tourId: String): Boolean
    suspend fun getChecklist(tourId: String): List<ChecklistItem>
    suspend fun toggleChecklistItem(tourId: String, itemId: String): Boolean
    suspend fun addChecklistItem(tourId: String, title: String): ChecklistItem
    suspend fun markNotificationRead(id: String)
    suspend fun markAllNotificationsRead()
}

class DefaultTravelRepository(
    private val restApi: SupabaseRestApi
) : TravelRepository {

    // In-memory persistent caches for responsive UX and offline robustness
    private val _destinations = MutableStateFlow<List<Destination>>(getInitialDestinations())
    override val destinationsFlow: Flow<List<Destination>> = _destinations.asStateFlow()

    private val _tours = MutableStateFlow<List<Tour>>(getInitialTours())
    override val toursFlow: Flow<List<Tour>> = _tours.asStateFlow()

    private val _itineraryMap = MutableStateFlow<Map<String, List<ItineraryItem>>>(getInitialItinerary())
    private val _expensesMap = MutableStateFlow<Map<String, List<Expense>>>(getInitialExpenses())
    private val _membersMap = MutableStateFlow<Map<String, List<TourMember>>>(getInitialMembers())
    private val _commentsMap = MutableStateFlow<Map<String, List<TourComment>>>(getInitialComments())
    private val _settlementsMap = MutableStateFlow<Map<String, List<Settlement>>>(getInitialSettlements())

    private val _notifications = MutableStateFlow<List<NotificationItem>>(getInitialNotifications())
    override val notificationsFlow: Flow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _checklistMap = MutableStateFlow<Map<String, List<ChecklistItem>>>(getInitialChecklist())

    override suspend fun refreshData() {
        try {
            val destResponse = restApi.getDestinations()
            if (destResponse.isSuccessful && !destResponse.body().isNullOrEmpty()) {
                val mapped = destResponse.body()!!.map { dto ->
                    Destination(
                        id = dto.id,
                        name = dto.name,
                        division = dto.division ?: "Bangladesh",
                        district = dto.district ?: "",
                        description = dto.description ?: "",
                        imageUrl = dto.imageUrl ?: "",
                        rating = dto.rating ?: 4.8,
                        isFeatured = dto.isFeatured ?: false,
                        category = dto.category ?: "Hills"
                    )
                }
                _destinations.value = mapped
            }
        } catch (_: Exception) {
            // Keep local data gracefully
        }

        try {
            val tourResponse = restApi.getTours()
            if (tourResponse.isSuccessful && !tourResponse.body().isNullOrEmpty()) {
                val mappedTours = tourResponse.body()!!.map { dto ->
                    Tour(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description ?: "",
                        destinationName = dto.destinationName ?: "Bangladesh",
                        startDate = dto.startDate ?: "2024-12-15",
                        endDate = dto.endDate ?: "2024-12-17",
                        status = when (dto.status?.lowercase()) {
                            "ongoing" -> TourStatus.ONGOING
                            "completed" -> TourStatus.COMPLETED
                            else -> TourStatus.UPCOMING
                        },
                        isPublic = dto.isPublic ?: true,
                        creatorId = dto.creatorId,
                        imageUrl = dto.imageUrl ?: "",
                        estimatedBudget = BigDecimal(dto.estimatedBudget ?: 0.0),
                        totalMembers = dto.totalMembers ?: 1,
                        rating = dto.rating ?: 4.8,
                        travelersCount = dto.travelersCount ?: "1.2k+"
                    )
                }
                _tours.value = mappedTours
            }
        } catch (_: Exception) {
            // Keep local data gracefully
        }
    }

    override suspend fun getTourById(id: String): Tour? {
        return _tours.value.find { it.id == id }
    }

    override suspend fun createPlan(
        title: String,
        destinationName: String,
        startDate: String,
        endDate: String,
        budget: BigDecimal,
        isPublic: Boolean,
        description: String
    ): Result<Tour> {
        val newTour = Tour(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description.ifBlank { "Exciting tour to $destinationName planned with tribe friends!" },
            destinationName = destinationName,
            startDate = startDate,
            endDate = endDate,
            status = TourStatus.UPCOMING,
            isPublic = isPublic,
            creatorId = "current_user",
            imageUrl = "dest_bandarban",
            estimatedBudget = budget,
            totalMembers = 1,
            rating = 5.0,
            travelersCount = "1 traveler"
        )

        val updated = _tours.value.toMutableList().apply { add(0, newTour) }
        _tours.value = updated

        // Initial member
        val owner = TourMember(
            id = UUID.randomUUID().toString(),
            tourId = newTour.id,
            userId = "current_user",
            role = "owner",
            status = "accepted",
            userName = "Me",
            avatarUrl = null
        )
        val m = _membersMap.value.toMutableMap()
        m[newTour.id] = listOf(owner)
        _membersMap.value = m

        return Result.success(newTour)
    }

    override suspend fun getItinerary(tourId: String): List<ItineraryItem> {
        return _itineraryMap.value[tourId] ?: listOf(
            ItineraryItem(
                id = "1",
                tourId = tourId,
                dayNumber = 1,
                title = "Arrival & Check-in",
                time = "08:00 AM",
                location = "Resort Center",
                description = "Check-in, relax and enjoy the scenic mountain view."
            ),
            ItineraryItem(
                id = "2",
                tourId = tourId,
                dayNumber = 1,
                title = "Sunset at Nilgiri / Peak View",
                time = "04:30 PM",
                location = "Nilgiri Viewpoint",
                description = "Witness the glorious clouds and sunset."
            ),
            ItineraryItem(
                id = "3",
                tourId = tourId,
                dayNumber = 2,
                title = "Waterfall Trekking & Cave Tour",
                time = "09:00 AM",
                location = "Nafakhum / Amiakhum",
                description = "Hiking adventure with local guide."
            )
        )
    }

    override suspend fun addItineraryItem(
        tourId: String,
        day: Int,
        time: String,
        location: String,
        title: String
    ): Result<ItineraryItem> {
        val item = ItineraryItem(
            id = UUID.randomUUID().toString(),
            tourId = tourId,
            dayNumber = day,
            title = title,
            time = time,
            location = location,
            description = "Planned group activity"
        )
        val map = _itineraryMap.value.toMutableMap()
        val currentList = map[tourId]?.toMutableList() ?: mutableListOf()
        currentList.add(item)
        map[tourId] = currentList
        _itineraryMap.value = map
        return Result.success(item)
    }

    override suspend fun getExpenses(tourId: String): List<Expense> {
        return _expensesMap.value[tourId] ?: emptyList()
    }

    override suspend fun addExpense(
        tourId: String,
        title: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        paidByName: String,
        paidById: String
    ): Result<Expense> {
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            tourId = tourId,
            title = title,
            amount = amount,
            category = category,
            paidByName = paidByName,
            paidById = paidById,
            date = "Today"
        )
        val map = _expensesMap.value.toMutableMap()
        val currentList = map[tourId]?.toMutableList() ?: mutableListOf()
        currentList.add(0, expense)
        map[tourId] = currentList
        _expensesMap.value = map
        return Result.success(expense)
    }

    override suspend fun getExpenseSummary(tourId: String, currentUserName: String): ExpenseSummary {
        val expenses = _expensesMap.value[tourId] ?: emptyList()
        val total = expenses.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }

        val members = _membersMap.value[tourId] ?: emptyList()
        val memberCount = if (members.isNotEmpty()) members.size else 6
        val memberCountBd = BigDecimal(memberCount)

        val yourShare = if (memberCount > 0 && total > BigDecimal.ZERO) {
            total.divide(memberCountBd, 2, RoundingMode.HALF_EVEN)
        } else {
            BigDecimal.ZERO
        }

        val yourTotalPaid = expenses
            .filter { it.paidByName.equals("Me", ignoreCase = true) || it.paidByName.equals(currentUserName, ignoreCase = true) }
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
        return _settlementsMap.value[tourId] ?: listOf(
            Settlement(
                id = "s1",
                tourId = tourId,
                fromUserName = "Me",
                toUserName = "Rahat",
                amount = BigDecimal("1200"),
                isSettled = false
            ),
            Settlement(
                id = "s2",
                tourId = tourId,
                fromUserName = "Sifat",
                toUserName = "Me",
                amount = BigDecimal("850"),
                isSettled = true
            )
        )
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
        return _membersMap.value[tourId] ?: emptyList()
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

    override suspend fun getTourComments(tourId: String): List<TourComment> {
        return _commentsMap.value[tourId] ?: emptyList()
    }

    override suspend fun addComment(tourId: String, userName: String, text: String): Result<TourComment> {
        val comment = TourComment(
            id = UUID.randomUUID().toString(),
            tourId = tourId,
            userName = userName,
            avatarUrl = null,
            comment = text,
            createdAt = "Just now"
        )
        val map = _commentsMap.value.toMutableMap()
        val list = map[tourId]?.toMutableList() ?: mutableListOf()
        list.add(0, comment)
        map[tourId] = list
        _commentsMap.value = map
        return Result.success(comment)
    }

    override suspend fun getTourGuides(): List<TourGuide> {
        return listOf(
            TourGuide(
                id = "g1",
                name = "Tanvir Ahmed",
                region = "Bandarban & Rangamati",
                experience = "7 years",
                languages = "Bengali, English, Marma",
                phone = "+880 1711-234567",
                rating = 4.9,
                avatarUrl = null
            ),
            TourGuide(
                id = "g2",
                name = "Shuvo Barua",
                region = "Sajek Valley & Khagrachari",
                experience = "5 years",
                languages = "Bengali, English, Chakma",
                phone = "+880 1812-987654",
                rating = 4.8,
                avatarUrl = null
            ),
            TourGuide(
                id = "g3",
                name = "Kazi Mahbub",
                region = "Sundarbans Mangrove Forest",
                experience = "10 years",
                languages = "Bengali, English",
                phone = "+880 1913-456789",
                rating = 5.0,
                avatarUrl = null
            )
        )
    }

    override suspend fun toggleSaveTour(tourId: String): Boolean {
        var isNowSaved = false
        val updated = _tours.value.map { tour ->
            if (tour.id == tourId) {
                isNowSaved = !tour.isSaved
                tour.copy(isSaved = isNowSaved)
            } else tour
        }
        _tours.value = updated
        return isNowSaved
    }

    override suspend fun getChecklist(tourId: String): List<ChecklistItem> {
        return _checklistMap.value[tourId] ?: getInitialChecklist()["default"] ?: emptyList()
    }

    override suspend fun toggleChecklistItem(tourId: String, itemId: String): Boolean {
        val map = _checklistMap.value.toMutableMap()
        val currentList = (map[tourId] ?: getChecklist(tourId)).toMutableList()
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
        val currentList = (map[tourId] ?: getChecklist(tourId)).toMutableList()
        currentList.add(item)
        map[tourId] = currentList
        _checklistMap.value = map
        return item
    }

    override suspend fun markNotificationRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _notifications.value = updated
    }

    override suspend fun markAllNotificationsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
    }

    // Seed Data
    private fun getInitialDestinations(): List<Destination> {
        return listOf(
            Destination(
                id = "dest_1",
                name = "Sajek Valley",
                division = "Chittagong",
                district = "Rangamati",
                description = "Nature's Paradise perched high among rolling clouds in the Hill Tracts.",
                imageUrl = "dest_sajek",
                rating = 4.9,
                isFeatured = true,
                category = "Hills"
            ),
            Destination(
                id = "dest_2",
                name = "Cox's Bazar",
                division = "Chittagong",
                district = "Cox's Bazar",
                description = "The world's longest unbroken natural sandy sea beach stretching 120km.",
                imageUrl = "dest_sajek",
                rating = 4.8,
                isFeatured = false,
                category = "Beach"
            ),
            Destination(
                id = "dest_3",
                name = "Sundarbans",
                division = "Khulna",
                district = "Bagerhat",
                description = "The largest mangrove forest in the world and home of the Royal Bengal Tiger.",
                imageUrl = "welcome_bg",
                rating = 4.9,
                isFeatured = false,
                category = "Heritage"
            ),
            Destination(
                id = "dest_4",
                name = "Saint Martin",
                division = "Chittagong",
                district = "Cox's Bazar",
                description = "Bangladesh's only coral island with turquoise water and coconut groves.",
                imageUrl = "dest_sajek",
                rating = 4.7,
                isFeatured = false,
                category = "Beach"
            ),
            Destination(
                id = "dest_5",
                name = "Bandarban",
                division = "Chittagong",
                district = "Bandarban",
                description = "Highest peaks, waterfalls, cloud-wrapped Nilgiri, and tribal heritage.",
                imageUrl = "dest_bandarban",
                rating = 4.8,
                isFeatured = true,
                category = "Adventure"
            )
        )
    }

    private fun getInitialTours(): List<Tour> {
        return listOf(
            Tour(
                id = "tour_bandarban",
                title = "Bandarban Adventure",
                description = "Explore the beautiful hills and waterfalls of Bandarban with friends!",
                destinationName = "Bandarban",
                startDate = "Dec 15 - 17, 2024",
                endDate = "Dec 17, 2024",
                status = TourStatus.ONGOING,
                isPublic = true,
                creatorId = "user_1",
                imageUrl = "dest_bandarban",
                estimatedBudget = BigDecimal("18500"),
                totalMembers = 6,
                rating = 4.7,
                travelersCount = "1.8k+",
                isSaved = true
            ),
            Tour(
                id = "tour_sundarbans",
                title = "Sundarbans Tour",
                description = "Cruising deep into the mangrove forest waterways and wildlife safaris.",
                destinationName = "Sundarbans",
                startDate = "Jan 10 - 12, 2025",
                endDate = "Jan 12, 2025",
                status = TourStatus.UPCOMING,
                isPublic = true,
                creatorId = "user_2",
                imageUrl = "welcome_bg",
                estimatedBudget = BigDecimal("14000"),
                totalMembers = 4,
                rating = 4.9,
                travelersCount = "950+",
                isSaved = false
            ),
            Tour(
                id = "tour_kuakata",
                title = "Kuakata Beach Tour",
                description = "Watch both sunrise and sunset from the same scenic sandy seashore.",
                destinationName = "Kuakata",
                startDate = "Feb 05 - 08, 2025",
                endDate = "Feb 08, 2025",
                status = TourStatus.UPCOMING,
                isPublic = true,
                creatorId = "user_3",
                imageUrl = "dest_sajek",
                estimatedBudget = BigDecimal("11000"),
                totalMembers = 5,
                rating = 4.8,
                travelersCount = "2.5k+",
                isSaved = false
            ),
            Tour(
                id = "tour_sajek",
                title = "Sajek Valley Escape",
                description = "Experience waking up above the clouds in cozy wooden hillside cottages.",
                destinationName = "Sajek Valley",
                startDate = "Nov 01 - 03, 2024",
                endDate = "Nov 03, 2024",
                status = TourStatus.COMPLETED,
                isPublic = true,
                creatorId = "user_1",
                imageUrl = "dest_sajek",
                estimatedBudget = BigDecimal("15000"),
                totalMembers = 6,
                rating = 5.0,
                travelersCount = "3.1k+",
                isSaved = true
            )
        )
    }

    private fun getInitialMembers(): Map<String, List<TourMember>> {
        return mapOf(
            "tour_bandarban" to listOf(
                TourMember("m1", "tour_bandarban", "u1", "owner", "accepted", "Dhaka Rider", null),
                TourMember("m2", "tour_bandarban", "u2", "admin", "accepted", "Rahat", null),
                TourMember("m3", "tour_bandarban", "u3", "member", "accepted", "Sifat", null),
                TourMember("m4", "tour_bandarban", "u4", "member", "accepted", "Fahim", null),
                TourMember("m5", "tour_bandarban", "u5", "member", "accepted", "Tanvir", null),
                TourMember("m6", "tour_bandarban", "u6", "member", "accepted", "Me", null)
            )
        )
    }

    private fun getInitialItinerary(): Map<String, List<ItineraryItem>> {
        return mapOf(
            "tour_bandarban" to listOf(
                ItineraryItem("it1", "tour_bandarban", 1, "Chander Gari Journey to Nilgiri", "07:30 AM", "Bandarban City", "Exciting off-road ride through misty winding mountains."),
                ItineraryItem("it2", "tour_bandarban", 1, "Nilgiri Resort & Cloud Viewing", "11:00 AM", "Nilgiri", "Enjoy floating cloud cover at 2,200 feet above sea level."),
                ItineraryItem("it3", "tour_bandarban", 2, "Shailopropat Waterfall & Tribal Market", "09:30 AM", "Shailopropat", "Fresh waterfall trek and local handicrafts."),
                ItineraryItem("it4", "tour_bandarban", 3, "Golden Temple (Buddha Dhatu Jadi)", "10:00 AM", "Balaghata", "Cultural visit to the renowned golden temple.")
            )
        )
    }

    private fun getInitialExpenses(): Map<String, List<Expense>> {
        return mapOf(
            "tour_bandarban" to listOf(
                Expense("e1", "tour_bandarban", "Hotel", BigDecimal("5000"), ExpenseCategory.HOTEL, "Sifat", "u3", "Dec 16"),
                Expense("e2", "tour_bandarban", "Food", BigDecimal("1200"), ExpenseCategory.FOOD, "Me", "u6", "Dec 15"),
                Expense("e3", "tour_bandarban", "Transport", BigDecimal("2000"), ExpenseCategory.TRANSPORT, "Rahat", "u2", "Dec 15"),
                Expense("e4", "tour_bandarban", "Resort Stay Nilgiri", BigDecimal("8500"), ExpenseCategory.HOTEL, "Rahat", "u2", "Dec 15"),
                Expense("e5", "tour_bandarban", "BBQ Dinner & Campfire", BigDecimal("1800"), ExpenseCategory.FOOD, "Dhaka Rider", "u1", "Dec 16")
            )
        )
    }

    private fun getInitialSettlements(): Map<String, List<Settlement>> {
        return mapOf(
            "tour_bandarban" to listOf(
                Settlement("s1", "tour_bandarban", "Me", "Rahat", BigDecimal("1200"), false),
                Settlement("s2", "tour_bandarban", "Sifat", "Me", BigDecimal("850"), true),
                Settlement("s3", "tour_bandarban", "Fahim", "Dhaka Rider", BigDecimal("1500"), true)
            )
        )
    }

    private fun getInitialComments(): Map<String, List<TourComment>> {
        return mapOf(
            "tour_bandarban" to listOf(
                TourComment("c1", "tour_bandarban", "Sifat", null, "Nilgiri was so dreamy today, clouds everywhere!", "2 hours ago"),
                TourComment("c2", "tour_bandarban", "Rahat", null, "Don't forget warm jackets for tonight's camp!", "4 hours ago")
            )
        )
    }

    private fun getInitialNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = "n1",
                title = "New Expense Added",
                message = "Sifat added 'Hotel' expense of ৳ 5,000 to Bandarban Adventure.",
                isRead = false,
                createdAt = "10 mins ago"
            ),
            NotificationItem(
                id = "n2",
                title = "Tour Member Joined",
                message = "Tanvir joined your Bandarban Trip.",
                isRead = false,
                createdAt = "2 hours ago"
            ),
            NotificationItem(
                id = "n3",
                title = "Welcome to TRIBE!",
                message = "Discover handpicked tours and start planning your next journey.",
                isRead = true,
                createdAt = "Yesterday"
            )
        )
    }

    private fun getInitialChecklist(): Map<String, List<ChecklistItem>> {
        val defaultItems = listOf(
            ChecklistItem("chk_1", "National ID / Passport copy", true, "Essentials"),
            ChecklistItem("chk_2", "Power bank (20,000 mAh)", true, "Electronics"),
            ChecklistItem("chk_3", "Trekking / Hiking shoes", false, "Gear"),
            ChecklistItem("chk_4", "Warm jacket / Windbreaker", false, "Clothing"),
            ChecklistItem("chk_5", "First-aid & motion sickness meds", true, "Health"),
            ChecklistItem("chk_6", "Raincoat / Waterproof poncho", false, "Clothing"),
            ChecklistItem("chk_7", "Cash in local currency (BDT ৳)", true, "Essentials"),
            ChecklistItem("chk_8", "Reusable water bottle", false, "Gear")
        )
        return mapOf(
            "tour_bandarban" to defaultItems,
            "default" to defaultItems
        )
    }
}

