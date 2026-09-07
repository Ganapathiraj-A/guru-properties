package com.example.guruproperties.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.guruproperties.data.model.House

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RentStatusFilter {
    ALL,
    COLLECTED,
    PENDING
}

enum class PropertyMonthRentStatus {
    COLLECTED,
    PARTIALLY_PAID,
    PENDING_NO_PAYMENT
}

@Composable
fun PropertyListScreen(
    houses: List<House>,
    collections: List<com.example.guruproperties.data.model.RentCollection> = emptyList(),
    tenants: List<com.example.guruproperties.data.model.Tenant> = emptyList(),
    onEditHouse: (House) -> Unit,
    onDeleteHouse: (String) -> Unit
) {
    var selectedHouseForDetails by remember { mutableStateOf<House?>(null) }
    var selectedFilter by remember { mutableStateOf(RentStatusFilter.ALL) }

    // Current Month in yyyy-MM format (e.g. "2026-09")
    val currentYearMonth = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val currentMonthDisplay = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Helper to calculate monthly status for a house
    fun getHouseMonthStatus(house: House): Triple<PropertyMonthRentStatus, Double, Double> {
        val houseCollectionsThisMonth = collections.filter {
            (it.houseId.equals(house.houseName, ignoreCase = true) ||
                    (house.houseId.isNotBlank() && it.houseId.equals(house.houseId, ignoreCase = true))) &&
                    it.paidDT.startsWith(currentYearMonth)
        }
        val monthPaid = houseCollectionsThisMonth.sumOf { it.paidAmt }
        val monthPending = houseCollectionsThisMonth.sumOf { it.pendingAmt }

        val status = when {
            monthPaid > 0.0 && monthPending == 0.0 -> PropertyMonthRentStatus.COLLECTED
            monthPaid > 0.0 && monthPending > 0.0 -> PropertyMonthRentStatus.PARTIALLY_PAID
            else -> PropertyMonthRentStatus.PENDING_NO_PAYMENT
        }
        return Triple(status, monthPaid, monthPending)
    }

    val filteredHouses = remember(houses, collections, selectedFilter, currentYearMonth) {
        when (selectedFilter) {
            RentStatusFilter.ALL -> houses
            RentStatusFilter.COLLECTED -> houses.filter {
                val (status, _, _) = getHouseMonthStatus(it)
                status == PropertyMonthRentStatus.COLLECTED
            }
            RentStatusFilter.PENDING -> houses.filter {
                val (status, _, _) = getHouseMonthStatus(it)
                status == PropertyMonthRentStatus.PENDING_NO_PAYMENT || status == PropertyMonthRentStatus.PARTIALLY_PAID
            }
        }
    }

    val totalCollectedCount = remember(houses, collections, currentYearMonth) {
        houses.count { getHouseMonthStatus(it).first == PropertyMonthRentStatus.COLLECTED }
    }
    val totalPendingCount = remember(houses, collections, currentYearMonth) {
        houses.count {
            val s = getHouseMonthStatus(it).first
            s == PropertyMonthRentStatus.PENDING_NO_PAYMENT || s == PropertyMonthRentStatus.PARTIALLY_PAID
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilter == RentStatusFilter.ALL,
                onClick = { selectedFilter = RentStatusFilter.ALL },
                label = { Text("All (${houses.size})") }
            )
            FilterChip(
                selected = selectedFilter == RentStatusFilter.COLLECTED,
                onClick = { selectedFilter = RentStatusFilter.COLLECTED },
                label = { Text("Collected ($totalCollectedCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (selectedFilter == RentStatusFilter.COLLECTED) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = selectedFilter == RentStatusFilter.PENDING,
                onClick = { selectedFilter = RentStatusFilter.PENDING },
                label = { Text("Pending ($totalPendingCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (selectedFilter == RentStatusFilter.PENDING) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.error
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }

        if (filteredHouses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (houses.isEmpty()) "No property details found" else "No properties match '$selectedFilter' for $currentMonthDisplay",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHouses) { house ->
                    val (monthStatus, monthPaid, monthPending) = getHouseMonthStatus(house)

                    // Total across all time
                    val houseCollections = collections.filter {
                        it.houseId.equals(house.houseName, ignoreCase = true) ||
                                (house.houseId.isNotBlank() && it.houseId.equals(house.houseId, ignoreCase = true))
                    }
                    val totalPaidForHouse = houseCollections.sumOf { it.paidAmt }
                    val totalPendingForHouse = houseCollections.sumOf { it.pendingAmt }

                    // Find active assigned tenant if not directly on house record
                    val assignedTenant = tenants.find {
                        it.houseId.equals(house.houseName, ignoreCase = true) ||
                                (house.houseId.isNotBlank() && it.houseId.equals(house.houseId, ignoreCase = true))
                    }
                    val activeTenantName = house.tenantName.ifBlank { assignedTenant?.tenantName ?: "" }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHouseForDetails = house },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (monthStatus) {
                                            PropertyMonthRentStatus.COLLECTED -> MaterialTheme.colorScheme.primaryContainer
                                            PropertyMonthRentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.tertiaryContainer
                                            PropertyMonthRentStatus.PENDING_NO_PAYMENT -> MaterialTheme.colorScheme.errorContainer
                                        }
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (monthStatus) {
                                        PropertyMonthRentStatus.COLLECTED -> Icons.Default.CheckCircle
                                        PropertyMonthRentStatus.PARTIALLY_PAID -> Icons.Default.HourglassEmpty
                                        PropertyMonthRentStatus.PENDING_NO_PAYMENT -> Icons.Default.Home
                                    },
                                    contentDescription = null,
                                    tint = when (monthStatus) {
                                        PropertyMonthRentStatus.COLLECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                                        PropertyMonthRentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.onTertiaryContainer
                                        PropertyMonthRentStatus.PENDING_NO_PAYMENT -> MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = house.houseName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Month Status Tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (monthStatus) {
                                                    PropertyMonthRentStatus.COLLECTED -> MaterialTheme.colorScheme.primaryContainer
                                                    PropertyMonthRentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.tertiaryContainer
                                                    PropertyMonthRentStatus.PENDING_NO_PAYMENT -> MaterialTheme.colorScheme.errorContainer
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (monthStatus) {
                                                PropertyMonthRentStatus.COLLECTED -> "Collected"
                                                PropertyMonthRentStatus.PARTIALLY_PAID -> "Partial"
                                                PropertyMonthRentStatus.PENDING_NO_PAYMENT -> "Rent Pending"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (monthStatus) {
                                                PropertyMonthRentStatus.COLLECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                                                PropertyMonthRentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.onTertiaryContainer
                                                PropertyMonthRentStatus.PENDING_NO_PAYMENT -> MaterialTheme.colorScheme.onErrorContainer
                                            }
                                        )
                                    }
                                }

                                if (house.location.isNotBlank()) {
                                    Text(
                                        text = house.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (activeTenantName.isNotBlank()) {
                                    Text(
                                        text = "👤 $activeTenantName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (totalPaidForHouse > 0.0 || totalPendingForHouse > 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (totalPaidForHouse > 0.0) {
                                            Text(
                                                text = "Paid: ₹$totalPaidForHouse",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (totalPendingForHouse > 0.0) {
                                            Text(
                                                text = "Pending: ₹$totalPendingForHouse",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Details",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Details Modal Dialog on Click
    selectedHouseForDetails?.let { house ->
        val assignedTenant = tenants.find {
            it.houseId.equals(house.houseName, ignoreCase = true) ||
                    (house.houseId.isNotBlank() && it.houseId.equals(house.houseId, ignoreCase = true))
        }
        val activeTenantName = house.tenantName.ifBlank { assignedTenant?.tenantName ?: "" }
        val activePhone = house.phoneNumber.ifBlank { assignedTenant?.phoneNumber ?: "" }
        AlertDialog(
            onDismissRequest = { selectedHouseForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = house.houseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "📍 Location: ${house.location.ifBlank { "Not specified" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Monthly Rent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("₹${house.monthlyRent}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Advance Deposit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("₹${house.advance}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Tenancy Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(house.tenancyDate.ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    val houseCollections = collections.filter {
                        it.houseId.equals(house.houseName, ignoreCase = true) ||
                                (house.houseId.isNotBlank() && it.houseId.equals(house.houseId, ignoreCase = true))
                    }
                    val totalPaidForHouse = houseCollections.sumOf { it.paidAmt }
                    val totalPendingForHouse = houseCollections.sumOf { it.pendingAmt }

                    if (totalPaidForHouse > 0.0 || totalPendingForHouse > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("₹$totalPaidForHouse", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("Total Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("₹$totalPendingForHouse", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "👤 Tenant Name: ${activeTenantName.ifBlank { "Unoccupied" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (activePhone.isNotBlank()) {
                        Text(
                            text = "📞 Phone: $activePhone",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val h = house
                            selectedHouseForDetails = null
                            onEditHouse(h)
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = {
                            val docId = house.docId
                            selectedHouseForDetails = null
                            onDeleteHouse(docId)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedHouseForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}
