package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val accessToken: String,
    val refreshToken: String?,
    val avatarUrl: String?
)

data class Destination(
    val id: String,
    val name: String,
    val division: String,
    val district: String,
    val description: String,
    val imageUrl: String,
    val rating: Double,
    val isFeatured: Boolean,
    val category: String
)

data class Tour(
    val id: String,
    val title: String,
    val description: String,
    val destinationName: String,
    val startDate: String,
    val endDate: String,
    val status: TourStatus,
    val isPublic: Boolean,
    val creatorId: String?,
    val imageUrl: String,
    val estimatedBudget: BigDecimal,
    val totalMembers: Int,
    val rating: Double,
    val travelersCount: String,
    val isSaved: Boolean = false
) {
    val durationDays: Int
        get() = 3 // Standard default or computed from dates
}

enum class TourStatus {
    UPCOMING, ONGOING, COMPLETED
}

data class TourMember(
    val id: String,
    val tourId: String,
    val userId: String,
    val role: String,
    val status: String,
    val userName: String,
    val avatarUrl: String?
)

data class ItineraryItem(
    val id: String,
    val tourId: String,
    val dayNumber: Int,
    val title: String,
    val time: String,
    val location: String,
    val description: String
)

data class Expense(
    val id: String,
    val tourId: String,
    val title: String,
    val amount: BigDecimal,
    val category: ExpenseCategory,
    val paidByName: String,
    val paidById: String,
    val date: String
) {
    val formattedAmount: String
        get() = formatCurrency(amount)
}

enum class ExpenseCategory {
    TRANSPORT, FOOD, HOTEL, ACTIVITIES, SHOPPING, GENERAL
}

data class ExpenseSummary(
    val totalExpense: BigDecimal,
    val yourShare: BigDecimal,
    val yourTotalPaid: BigDecimal,
    val netBalance: BigDecimal // positive: user is owed money; negative: user owes
) {
    val formattedTotalExpense: String
        get() = formatCurrency(totalExpense)

    val formattedYourShare: String
        get() = formatCurrency(yourShare)

    val formattedNetBalance: String
        get() = formatCurrency(netBalance.abs())
}

data class Settlement(
    val id: String,
    val tourId: String,
    val fromUserName: String,
    val toUserName: String,
    val amount: BigDecimal,
    val isSettled: Boolean
) {
    val formattedAmount: String
        get() = formatCurrency(amount)
}

data class TourGuide(
    val id: String,
    val name: String,
    val region: String,
    val experience: String,
    val languages: String,
    val phone: String,
    val rating: Double,
    val avatarUrl: String?
)

data class TourComment(
    val id: String,
    val tourId: String,
    val userName: String,
    val avatarUrl: String?,
    val comment: String,
    val createdAt: String
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)

data class ChecklistItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val category: String = "Essentials"
)

fun formatCurrency(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return "৳ " + formatter.format(amount.setScale(2, RoundingMode.HALF_EVEN))
}
