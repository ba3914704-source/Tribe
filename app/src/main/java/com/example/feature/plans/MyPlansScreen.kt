package com.example.feature.plans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.designsystem.StatusBadge
import com.example.core.designsystem.TribeEmptyState
import com.example.core.designsystem.TribePrimaryButton
import com.example.domain.model.Tour
import com.example.domain.model.TourStatus
import com.example.ui.theme.TribePrimary

@Composable
fun MyPlansScreen(
    plans: List<Tour>,
    onPlanClick: (Tour) -> Unit,
    onCreatePlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by remember { mutableStateOf(TourStatus.ONGOING) }

    val filteredPlans = remember(selectedStatus, plans) {
        plans.filter { it.status == selectedStatus }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_plans_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            // Screen Title
            item {
                Text(
                    text = stringResource(R.string.nav_my_plans),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Segmented Status Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val statusOptions = listOf(
                        TourStatus.UPCOMING to R.string.tab_upcoming,
                        TourStatus.ONGOING to R.string.tab_ongoing,
                        TourStatus.COMPLETED to R.string.tab_completed
                    )

                    statusOptions.forEach { (status, labelRes) ->
                        val isSelected = selectedStatus == status
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) TribePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = if (isSelected) TribePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStatus = status }
                                .testTag("status_tab_${status.name}")
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Plans List
            if (filteredPlans.isEmpty()) {
                item {
                    TribeEmptyState(
                        title = stringResource(R.string.empty_plans_title),
                        subtitle = stringResource(R.string.empty_plans_subtitle),
                        buttonText = stringResource(R.string.create_new_plan),
                        onButtonClick = onCreatePlanClick
                    )
                }
            } else {
                items(filteredPlans) { plan ->
                    MyPlanCardItem(
                        plan = plan,
                        onClick = { onPlanClick(plan) }
                    )
                }
            }
        }

        // Bottom "+ Create New Plan" Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 76.dp)
                .padding(horizontal = 20.dp),
            color = Color.Transparent
        ) {
            TribePrimaryButton(
                text = "+ " + stringResource(R.string.create_new_plan),
                onClick = onCreatePlanClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "plans_create_plan_btn"
            )
        }
    }
}

@Composable
fun MyPlanCardItem(
    plan: Tour,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(180.dp)
            .clickable(onClick = onClick)
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imgRes = if (plan.destinationName.contains("Bandarban", ignoreCase = true)) {
                R.drawable.dest_bandarban
            } else {
                R.drawable.dest_sajek
            }

            Image(
                painter = painterResource(id = imgRes),
                contentDescription = plan.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${plan.startDate} • ${plan.totalMembers} Members",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                StatusBadge(status = plan.status)
            }
        }
    }
}
