package com.example.guruproperties.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

@Composable
fun AddEditHouseDialog(
    house: House?,
    onDismiss: () -> Unit,
    onSave: (House) -> Unit
) {
    var houseId by remember { mutableStateOf(house?.houseId ?: "") }
    var houseName by remember { mutableStateOf(house?.houseName ?: "") }
    var location by remember { mutableStateOf(house?.location ?: "") }
    var monthlyRent by remember { mutableStateOf(house?.monthlyRent?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var advance by remember { mutableStateOf(house?.advance?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var monthlyRentRevision by remember { mutableStateOf(house?.monthlyRentRevision?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var revisionDate by remember { mutableStateOf(house?.revisionDate ?: "") }
    var tenancyDate by remember { mutableStateOf(house?.tenancyDate ?: "") }
    var tenantName by remember { mutableStateOf(house?.tenantName ?: "") }
    var phoneNumber by remember { mutableStateOf(house?.phoneNumber ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (house == null) "Add Property (Table 1)" else "Edit Property details",
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = houseId,
                        onValueChange = { houseId = it },
                        label = { Text("HouseID (e.g. H101)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = houseName,
                        onValueChange = { houseName = it },
                        label = { Text("House Name") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

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
                    OutlinedTextField(
                        value = revisionDate,
                        onValueChange = { revisionDate = it },
                        label = { Text("Revision Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tenancyDate,
                        onValueChange = { tenancyDate = it },
                        label = { Text("Tenancy Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = tenantName,
                        onValueChange = { tenantName = it },
                        label = { Text("Tenant Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
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
                    if (houseId.isNotBlank() && houseName.isNotBlank()) {
                        val updated = (house ?: House()).copy(
                            houseId = houseId.trim(),
                            houseName = houseName.trim(),
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
