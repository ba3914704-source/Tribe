package com.example.feature.tourdetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.designsystem.TribePrimaryButton
import com.example.domain.model.*
import com.example.ui.theme.TribeOngoingBadge
import com.example.ui.theme.TribePrimary
import java.math.BigDecimal

enum class TripDetailTab {
    OVERVIEW, ITINERARY, EXPENSES, MEMBERS, CHECKLIST, COMMENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tour: Tour,
    members: List<TourMember>,
    itinerary: List<ItineraryItem>,
    expenses: List<Expense>,
    expenseSummary: ExpenseSummary,
    settlements: List<Settlement> = emptyList(),
    checklist: List<ChecklistItem> = emptyList(),
    comments: List<TourComment>,
    onBackClick: () -> Unit,
    onAddExpense: (title: String, amount: BigDecimal, category: ExpenseCategory, paidByName: String) -> Unit,
    onAddItineraryItem: (day: Int, time: String, location: String, title: String) -> Unit,
    onInviteMember: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit = {},
    onAddChecklistItem: (String) -> Unit = {},
    onSettleUp: (String, String, BigDecimal) -> Unit = { _, _, _ -> },
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TripDetailTab.OVERVIEW) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddItineraryDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showAddChecklistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("trip_details_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShareClick, modifier = Modifier.testTag("trip_share_btn")) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("trip_details_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Photo Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(220.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val imgRes = if (tour.destinationName.contains("Bandarban", ignoreCase = true)) {
                            R.drawable.dest_bandarban
                        } else {
                            R.drawable.dest_sajek
                        }

                        Image(
                            painter = painterResource(id = imgRes),
                            contentDescription = tour.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                        )

                        // Title & Details
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tour.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${tour.startDate} • ${tour.totalMembers} Members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            // Share Pill Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.clickable(onClick = onShareClick)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = TribePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.share),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TribePrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sub Navigation Tabs: Overview, Itinerary, Expenses, Members, Comments
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 20.dp,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = TribePrimary
                        )
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    TripDetailTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = when (tab) {
                                        TripDetailTab.OVERVIEW -> stringResource(R.string.tab_overview)
                                        TripDetailTab.ITINERARY -> stringResource(R.string.tab_itinerary)
                                        TripDetailTab.EXPENSES -> stringResource(R.string.tab_expenses)
                                        TripDetailTab.MEMBERS -> stringResource(R.string.tab_members)
                                        TripDetailTab.CHECKLIST -> stringResource(R.string.tab_checklist)
                                        TripDetailTab.COMMENTS -> "Comments"
                                    },
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == tab) TribePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("trip_tab_${tab.name}")
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                TripDetailTab.OVERVIEW -> {
                    item {
                        OverviewTabContent(
                            tour = tour,
                            members = members,
                            onViewItinerary = { selectedTab = TripDetailTab.ITINERARY },
                            onInviteClick = { showInviteDialog = true }
                        )
                    }
                }
                TripDetailTab.ITINERARY -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Itinerary Timeline",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = { showAddItineraryDialog = true }) {
                                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Activity", tint = TribePrimary)
                            }
                        }
                    }
                    items(itinerary) { item ->
                        ItineraryTimelineItem(item = item)
                    }
                }
                TripDetailTab.EXPENSES -> {
                    item {
                        ExpensesTabContent(
                            tour = tour,
                            expenses = expenses,
                            summary = expenseSummary,
                            settlements = settlements,
                            onAddExpenseClick = { showAddExpenseDialog = true },
                            onSettleUp = onSettleUp
                        )
                    }
                }
                TripDetailTab.MEMBERS -> {
                    item {
                        MembersTabContent(
                            members = members,
                            onInviteClick = { showInviteDialog = true }
                        )
                    }
                }
                TripDetailTab.CHECKLIST -> {
                    item {
                        ChecklistTabContent(
                            checklist = checklist,
                            onToggleItem = onToggleChecklistItem,
                            onAddClick = { showAddChecklistDialog = true }
                        )
                    }
                }
                TripDetailTab.COMMENTS -> {
                    item {
                        CommentsTabContent(
                            comments = comments,
                            onAddComment = onAddComment
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, amount, cat, payer ->
                onAddExpense(title, amount, cat, payer)
                showAddExpenseDialog = false
            }
        )
    }

    // Add Itinerary Dialog
    if (showAddItineraryDialog) {
        AddItineraryDialog(
            onDismiss = { showAddItineraryDialog = false },
            onConfirm = { day, time, loc, title ->
                onAddItineraryItem(day, time, loc, title)
                showAddItineraryDialog = false
            }
        )
    }

    // Invite Member Dialog
    if (showInviteDialog) {
        var memberName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text(stringResource(R.string.invite_member)) },
            text = {
                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Friend's Name / Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memberName.isNotBlank()) {
                            onInviteMember(memberName)
                            showInviteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TribePrimary)
                ) {
                    Text("Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add Checklist Dialog
    if (showAddChecklistDialog) {
        var itemTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChecklistDialog = false },
            title = { Text("Add Packing Item") },
            text = {
                OutlinedTextField(
                    value = itemTitle,
                    onValueChange = { itemTitle = it },
                    label = { Text("Item Name (e.g. Flashlight, Sunscreen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemTitle.isNotBlank()) {
                            onAddChecklistItem(itemTitle)
                            showAddChecklistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TribePrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChecklistDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun OverviewTabContent(
    tour: Tour,
    members: List<TourMember>,
    onViewItinerary: () -> Unit,
    onInviteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = tour.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Group Members Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.group_members)} (${tour.totalMembers})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onInviteClick) {
                Text("+ Invite", color = TribePrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Members Avatars Row
        Row(
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayMembers = if (members.isNotEmpty()) members else listOf(
                TourMember("1", tour.id, "1", "owner", "accepted", "Dhaka Rider", null),
                TourMember("2", tour.id, "2", "admin", "accepted", "Rahat", null),
                TourMember("3", tour.id, "3", "member", "accepted", "Sifat", null),
                TourMember("4", tour.id, "4", "member", "accepted", "Fahim", null),
                TourMember("5", tour.id, "5", "member", "accepted", "Tanvir", null)
            )

            displayMembers.take(5).forEach { member ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(TribePrimary.copy(alpha = 0.2f))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.userName.take(1).uppercase(),
                        color = TribePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // +1 pill
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+1",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TribePrimaryButton(
            text = stringResource(R.string.view_itinerary),
            onClick = onViewItinerary,
            modifier = Modifier.fillMaxWidth(),
            testTag = "overview_view_itinerary_btn"
        )
    }
}

@Composable
fun ExpensesTabContent(
    tour: Tour,
    expenses: List<Expense>,
    summary: ExpenseSummary,
    settlements: List<Settlement> = emptyList(),
    onAddExpenseClick: () -> Unit,
    onSettleUp: (String, String, BigDecimal) -> Unit = { _, _, _ -> }
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredExpenses = remember(selectedFilter, expenses) {
        when (selectedFilter) {
            "You Paid" -> expenses.filter { it.paidByName.equals("Me", ignoreCase = true) || it.paidByName.contains("Dhaka", ignoreCase = true) }
            "Others" -> expenses.filter { !it.paidByName.equals("Me", ignoreCase = true) && !it.paidByName.contains("Dhaka", ignoreCase = true) }
            else -> expenses
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Total Expense Card (Emerald Green matching Mockup 7)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TribePrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.total_expense),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.formattedTotalExpense,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.your_share),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.formattedYourShare,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips: All, You Paid, Others
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "You Paid", "Others").forEach { chip ->
                val isSelected = selectedFilter == chip
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = chip },
                    label = { Text(chip) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TribePrimary.copy(alpha = 0.15f),
                        selectedLabelColor = TribePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expense List
        filteredExpenses.forEach { exp ->
            ExpenseListItemRow(expense = exp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Settlements section
        if (settlements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settlements & Debts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            settlements.forEach { s ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${s.fromUserName} → ${s.toUserName}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (s.isSettled) "Settled" else "Pending Payment",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (s.isSettled) TribePrimary else MaterialTheme.colorScheme.error
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.formattedAmount,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (!s.isSettled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onSettleUp(s.fromUserName, s.toUserName, s.amount) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TribePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Settle", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "+ Add Expense" Button
        TribePrimaryButton(
            text = "+ " + stringResource(R.string.add_expense),
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth(),
            testTag = "add_expense_btn"
        )
    }
}

@Composable
fun ChecklistTabContent(
    checklist: List<ChecklistItem>,
    onToggleItem: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val completedCount = checklist.count { it.isCompleted }
            Column {
                Text(
                    text = "Packing Checklist",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$completedCount of ${checklist.size} items packed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TribePrimary
                )
            }
            TribePrimaryButton(
                text = "+ Add Item",
                onClick = onAddClick,
                modifier = Modifier.height(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        checklist.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onToggleItem(item.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggleItem(item.id) },
                        colors = CheckboxDefaults.colors(checkedColor = TribePrimary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                            ),
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseListItemRow(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            val (icon, bgColor) = when (expense.category) {
                ExpenseCategory.TRANSPORT -> Pair(Icons.Default.DirectionsCar, Color(0xFFFF9800))
                ExpenseCategory.FOOD -> Pair(Icons.Default.Restaurant, Color(0xFF4CAF50))
                ExpenseCategory.HOTEL -> Pair(Icons.Default.Hotel, Color(0xFF2196F3))
                else -> Pair(Icons.Default.Receipt, Color(0xFF9C27B0))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = bgColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "By ${expense.paidByName} • ${expense.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = expense.formattedAmount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ItineraryTimelineItem(item: ItineraryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(TribePrimary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D${item.dayNumber}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(TribePrimary.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = TribePrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${item.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MembersTabContent(
    members: List<TourMember>,
    onInviteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trip Members (${members.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            TribePrimaryButton(
                text = "+ Invite Member",
                onClick = onInviteClick,
                modifier = Modifier.height(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        members.forEach { m ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(TribePrimary.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.userName.take(1).uppercase(),
                            color = TribePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = m.userName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = m.role.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsTabContent(
    comments: List<TourComment>,
    onAddComment: (String) -> Unit
) {
    var newComment by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Community Notes & Comments",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                placeholder = { Text("Write a comment...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newComment.isNotBlank()) {
                        onAddComment(newComment)
                        newComment = ""
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = TribePrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        comments.forEach { c ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = c.userName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = c.createdAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = c.comment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: BigDecimal, category: ExpenseCategory, payer: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var payer by remember { mutableStateOf("Me") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Hotel Stay, Dinner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (৳ BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ExpenseCategory.HOTEL, ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (title.isNotBlank() && amount > BigDecimal.ZERO) {
                        onConfirm(title, amount, selectedCategory, payer)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TribePrimary)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AddItineraryDialog(
    onDismiss: () -> Unit,
    onConfirm: (day: Int, time: String, location: String, title: String) -> Unit
) {
    var dayStr by remember { mutableStateOf("1") }
    var time by remember { mutableStateOf("10:00 AM") }
    var location by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayStr,
                        onValueChange = { dayStr = it },
                        label = { Text("Day #") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val day = dayStr.toIntOrNull() ?: 1
                    if (title.isNotBlank()) {
                        onConfirm(day, time, location, title)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TribePrimary)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
