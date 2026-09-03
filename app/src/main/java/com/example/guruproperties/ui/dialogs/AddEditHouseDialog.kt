package com.example.guruproperties.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.Tenant
import java.util.Calendar
import java.util.Locale

@Composable
fun AddEditHouseDialog(
    house: House?,
    availableTenants: List<Tenant> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (House) -> Unit
) {
    val context = LocalContext.current
    var houseName by remember { mutableStateOf(house?.houseName ?: "") }
    var location by remember { mutableStateOf(house?.location ?: "") }
    var monthlyRent by remember { mutableStateOf(house?.monthlyRent?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var advance by remember { mutableStateOf(house?.advance?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var monthlyRentRevision by remember { mutableStateOf(house?.monthlyRentRevision?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var revisionDate by remember { mutableStateOf(house?.revisionDate ?: "") }
    var tenancyDate by remember { mutableStateOf(house?.tenancyDate ?: "") }
    var tenantName by remember { mutableStateOf(house?.tenantName ?: "") }
    var phoneNumber by remember { mutableStateOf(house?.phoneNumber ?: "") }

    var isTenantDropdownExpanded by remember { mutableStateOf(false) }

    val tenantOptions = remember(availableTenants) {
        availableTenants.map { it.tenantName }.filter { it.isNotBlank() }.distinct()
    }

    val scrollState = rememberScrollState()

    fun openDatePicker(initialDate: String, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        var y = cal.get(Calendar.YEAR)
        var m = cal.get(Calendar.MONTH)
        var d = cal.get(Calendar.DAY_OF_MONTH)

        if (initialDate.isNotBlank() && initialDate.contains("-")) {
            val parts = initialDate.split("-")
            if (parts.size >= 3) {
                y = parts[0].toIntOrNull() ?: y
                m = (parts[1].toIntOrNull() ?: (m + 1)) - 1
                d = parts[2].substringBefore(" ").toIntOrNull() ?: d
            }
        }

        DatePickerDialog(context, { _, year, monthOfYear, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            onSelected(formatted)
        }, y, m, d).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (house == null) "Add Property" else "Edit Property Details",
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
                OutlinedTextField(
                    value = houseName,
                    onValueChange = { houseName = it },
                    label = { Text("House Name (e.g. Salem House)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = monthlyRent,
                        onValueChange = { monthlyRent = it },
                        label = { Text("Monthly Rent (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = advance,
                        onValueChange = { advance = it },
                        label = { Text("Advance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = monthlyRentRevision,
                        onValueChange = { monthlyRentRevision = it },
                        label = { Text("Rent Revision (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Choosable Revision Date Picker
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = revisionDate,
                            onValueChange = { revisionDate = it },
                            label = { Text("Revision Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            trailingIcon = {
                                IconButton(onClick = { openDatePicker(revisionDate) { revisionDate = it } }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openDatePicker(revisionDate) { revisionDate = it } },
                            singleLine = true
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Choosable Tenancy Date Picker
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = tenancyDate,
                            onValueChange = { tenancyDate = it },
                            label = { Text("Tenancy Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            trailingIcon = {
                                IconButton(onClick = { openDatePicker(tenancyDate) { tenancyDate = it } }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openDatePicker(tenancyDate) { tenancyDate = it } },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Selectable Tenant Name Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = tenantName,
                            onValueChange = { tenantName = it },
                            label = { Text("Tenant Name") },
                            trailingIcon = {
                                IconButton(onClick = { isTenantDropdownExpanded = !isTenantDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Tenant")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTenantDropdownExpanded = true },
                            singleLine = true
                        )
                        if (tenantOptions.isNotEmpty()) {
                            DropdownMenu(
                                expanded = isTenantDropdownExpanded,
                                onDismissRequest = { isTenantDropdownExpanded = false }
                            ) {
                                tenantOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            tenantName = option
                                            val matchedTenant = availableTenants.find { it.tenantName.equals(option, ignoreCase = true) }
                                            if (matchedTenant != null && matchedTenant.phoneNumber.isNotBlank() && phoneNumber.isBlank()) {
                                                phoneNumber = matchedTenant.phoneNumber
                                            }
                                            isTenantDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (houseName.isNotBlank()) {
                        val nameClean = houseName.trim()
                        val updated = (house ?: House()).copy(
                            houseId = nameClean,
                            houseName = nameClean,
                            location = location.trim(),
                            monthlyRent = monthlyRent.toDoubleOrNull() ?: 0.0,
                            advance = advance.toDoubleOrNull() ?: 0.0,
                            monthlyRentRevision = monthlyRentRevision.toDoubleOrNull() ?: 0.0,
                            revisionDate = revisionDate.trim(),
                            tenancyDate = tenancyDate.trim(),
                            tenantName = tenantName.trim(),
                            phoneNumber = phoneNumber.trim()
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
