package com.example.guruproperties.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditCollectionDialog(
    collection: RentCollection?,
    availableHouses: List<House>,
    onDismiss: () -> Unit,
    onSave: (RentCollection) -> Unit
) {
    var houseId by remember { mutableStateOf(collection?.houseId ?: if (availableHouses.isNotEmpty()) availableHouses.first().houseId else "") }
    var pendingAmt by remember { mutableStateOf(collection?.pendingAmt?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var paidAmt by remember { mutableStateOf(collection?.paidAmt?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var paidDT by remember {
        mutableStateOf(
            collection?.paidDT?.ifBlank { null }
                ?: SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
        )
    }
    var paidBy by remember {
        mutableStateOf(
            collection?.paidBy ?: availableHouses.find { it.houseId.equals(houseId, ignoreCase = true) }?.tenantName ?: ""
        )
    }
    var paidThru by remember { mutableStateOf(collection?.paidThru ?: "UPI") }

    var isHouseDropdownExpanded by remember { mutableStateOf(false) }
    var isPaymentModeExpanded by remember { mutableStateOf(false) }

    val paymentModes = listOf("UPI", "Cash", "Bank Transfer", "Cheque", "Other")
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (collection == null) "Add Rent Collection (Table 2)" else "Edit Rent Collection",
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
                // House ID selection dropdown
                Column {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (availableHouses.isNotEmpty()) isHouseDropdownExpanded = true },
                        singleLine = true
                    )
                    if (availableHouses.isNotEmpty()) {
                        DropdownMenu(
                            expanded = isHouseDropdownExpanded,
                            onDismissRequest = { isHouseDropdownExpanded = false }
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
                }

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

                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Paid By") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    OutlinedTextField(
                        value = paidThru,
                        onValueChange = { paidThru = it },
                        label = { Text("Paid Through (Payment Mode)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPaymentModeExpanded = true },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isPaymentModeExpanded,
                        onDismissRequest = { isPaymentModeExpanded = false }
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

                OutlinedTextField(
                    value = paidDT,
                    onValueChange = { paidDT = it },
                    label = { Text("Paid Date & Time (PaidDT)") },
                    placeholder = { Text("YYYY-MM-DD hh:mm AM/PM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
