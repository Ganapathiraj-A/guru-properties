package com.example.guruproperties.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.RentCollection
import com.example.guruproperties.data.model.Tenant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditCollectionDialog(
    collection: RentCollection?,
    availableHouses: List<House>,
    availableTenants: List<Tenant> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (RentCollection) -> Unit
) {
    var houseId by remember { mutableStateOf(collection?.houseId ?: if (availableHouses.isNotEmpty()) availableHouses.first().houseId else "") }
    var pendingAmt by remember { mutableStateOf(collection?.pendingAmt?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var paidAmt by remember { mutableStateOf(collection?.paidAmt?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    
    val currentFormattedDate = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date()) }
    var paidDT by remember {
        mutableStateOf(
            collection?.paidDT?.ifBlank { null } ?: currentFormattedDate
        )
    }
    
    var paidBy by remember {
        mutableStateOf(
            collection?.paidBy?.ifBlank { null }
                ?: availableHouses.find { it.houseId.equals(houseId, ignoreCase = true) }?.tenantName
                ?: ""
        )
    }
    var paidThru by remember { mutableStateOf(collection?.paidThru?.ifBlank { "UPI" } ?: "UPI") }

    var isHouseDropdownExpanded by remember { mutableStateOf(false) }
    var isPaidByDropdownExpanded by remember { mutableStateOf(false) }
    var isPaymentModeExpanded by remember { mutableStateOf(false) }
    var isDateDropdownExpanded by remember { mutableStateOf(false) }

    val paymentModes = listOf("UPI", "Cash", "Bank Transfer", "Cheque", "Credit Card", "Net Banking", "Other")

    // Unique list of tenant names combining managed tenants and property tenants
    val tenantOptions = remember(availableHouses, availableTenants) {
        val namesFromTenants = availableTenants.map { it.tenantName }
        val namesFromHouses = availableHouses.map { it.tenantName }
        (namesFromTenants + namesFromHouses).filter { it.isNotBlank() }.distinct()
    }


    // Quick date options for PaidDT dropdown
    val dateOptions = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(cal.time)
        val todayMorning = "${sdf.format(cal.time)} 09:30 AM"
        val todayEvening = "${sdf.format(cal.time)} 06:00 PM"
        
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterdayStr = "${sdf.format(cal.time)} 10:00 AM"

        cal.time = Date()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartStr = "${sdf.format(cal.time)} 09:00 AM"

        listOf(
            "Now ($nowStr)" to nowStr,
            "Today Morning ($todayMorning)" to todayMorning,
            "Today Evening ($todayEvening)" to todayEvening,
            "Yesterday ($yesterdayStr)" to yesterdayStr,
            "1st of Month ($monthStartStr)" to monthStartStr
        )
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (collection == null) "Add Rent Collection" else "Edit Rent Collection",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. House ID Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = houseId,
                        onValueChange = {
                            houseId = it
                            val matched = availableHouses.find { h -> h.houseId.equals(it, ignoreCase = true) }
                            if (matched != null) {
                                paidBy = matched.tenantName
                            }
                        },
                        label = { Text("HouseID (e.g. H101)") },
                        trailingIcon = {
                            IconButton(onClick = { isHouseDropdownExpanded = !isHouseDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select House")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHouseDropdownExpanded = true },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isHouseDropdownExpanded,
                        onDismissRequest = { isHouseDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        availableHouses.forEach { houseItem ->
                            DropdownMenuItem(
                                text = { Text("${houseItem.houseId} - ${houseItem.houseName} (${houseItem.tenantName})") },
                                onClick = {
                                    houseId = houseItem.houseId
                                    paidBy = houseItem.tenantName
                                    val rent = houseItem.monthlyRent
                                    if (paidAmt.isBlank() && rent > 0) {
                                        paidAmt = rent.toString()
                                    }
                                    isHouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amounts
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paidAmt,
                        onValueChange = { paidAmt = it },
                        label = { Text("Paid Amt (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = pendingAmt,
                        onValueChange = { pendingAmt = it },
                        label = { Text("Pending Amt (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // 2. Paid By Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = { paidBy = it },
                        label = { Text("Paid By (Select or Type Name)") },
                        trailingIcon = {
                            IconButton(onClick = { isPaidByDropdownExpanded = !isPaidByDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Tenant")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPaidByDropdownExpanded = true },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isPaidByDropdownExpanded,
                        onDismissRequest = { isPaidByDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        tenantOptions.forEach { tenant ->
                            DropdownMenuItem(
                                text = { Text(tenant) },
                                onClick = {
                                    paidBy = tenant
                                    isPaidByDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 3. Paid Through (Payment Mode) Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paidThru,
                        onValueChange = { paidThru = it },
                        label = { Text("Paid Through (Payment Mode)") },
                        trailingIcon = {
                            IconButton(onClick = { isPaymentModeExpanded = !isPaymentModeExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Mode")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPaymentModeExpanded = true },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isPaymentModeExpanded,
                        onDismissRequest = { isPaymentModeExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        paymentModes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    paidThru = mode
                                    isPaymentModeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 4. Paid Date & Time (PaidDT) Dropdown / Quick Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paidDT,
                        onValueChange = { paidDT = it },
                        label = { Text("Paid Date & Time (PaidDT)") },
                        trailingIcon = {
                            IconButton(onClick = { isDateDropdownExpanded = !isDateDropdownExpanded }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date & Time")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDateDropdownExpanded = true },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isDateDropdownExpanded,
                        onDismissRequest = { isDateDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        dateOptions.forEach { (label, valStr) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    paidDT = valStr
                                    isDateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (houseId.isNotBlank()) {
                        val updated = (collection ?: RentCollection()).copy(
                            houseId = houseId.trim(),
                            pendingAmt = pendingAmt.toDoubleOrNull() ?: 0.0,
                            paidAmt = paidAmt.toDoubleOrNull() ?: 0.0,
                            paidDT = paidDT.trim(),
                            paidBy = paidBy.trim(),
                            paidThru = paidThru.trim()
                        )
                        onSave(updated)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
