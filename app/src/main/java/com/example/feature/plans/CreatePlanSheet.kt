package com.example.feature.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.designsystem.TribePrimaryButton
import com.example.ui.theme.TribePrimary
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanSheet(
    onDismiss: () -> Unit,
    onCreatePlan: (title: String, destination: String, start: String, end: String, budget: BigDecimal, isPublic: Boolean, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("Dec 20, 2024") }
    var endDate by remember { mutableStateOf("Dec 23, 2024") }
    var budgetStr by remember { mutableStateOf("15000") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    val quickDestinations = listOf("Bandarban", "Sajek Valley", "Cox's Bazar", "Sundarbans", "Saint Martin")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("create_plan_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.create_new_plan),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trip Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Trip Title (e.g. Winter Bandarban Escape)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Destination
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_destination_input")
            )

            // Quick chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickDestinations.take(3).forEach { dest ->
                    FilterChip(
                        selected = destination == dest,
                        onClick = { destination = dest },
                        label = { Text(dest, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Budget
            OutlinedTextField(
                value = budgetStr,
                onValueChange = { budgetStr = it },
                label = { Text("Estimated Budget (৳ BDT)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_budget_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Public Tour Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Public Discovery Tour",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Allow others in Explore tab to discover this tour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TribePrimary)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create button
            TribePrimaryButton(
                text = stringResource(R.string.confirm),
                onClick = {
                    val budget = budgetStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onCreatePlan(
                        title.ifBlank { "$destination Trip" },
                        destination.ifBlank { "Bandarban" },
                        startDate,
                        endDate,
                        budget,
                        isPublic,
                        description
                    )
                },
                enabled = title.isNotBlank() || destination.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                testTag = "submit_create_plan_btn"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
