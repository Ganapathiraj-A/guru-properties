package com.example.guruproperties.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.guruproperties.data.model.Tenant

@Composable
fun AddEditTenantDialog(
    tenant: Tenant?,
    availableHouses: List<House> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Tenant) -> Unit
) {
    var tenantName by remember { mutableStateOf(tenant?.tenantName ?: "") }
    var phoneNumber by remember { mutableStateOf(tenant?.phoneNumber ?: "") }
    var houseId by remember { mutableStateOf(tenant?.houseId ?: "") }
    var isHouseDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (tenant == null) "Add New Tenant" else "Edit Tenant Details",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tenantName,
                    onValueChange = { tenantName = it },
                    label = { Text("Tenant Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Select House using Dropdown (Not direct entry)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = houseId.ifBlank { "None (Unassigned)" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned House / Property") },
                        trailingIcon = {
                            IconButton(onClick = { isHouseDropdownExpanded = !isHouseDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select House")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHouseDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isHouseDropdownExpanded,
                        onDismissRequest = { isHouseDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Unassigned)") },
                            onClick = {
                                houseId = ""
                                isHouseDropdownExpanded = false
                            }
                        )
                        availableHouses.forEach { houseItem ->
                            DropdownMenuItem(
                                text = {
                                    val locationSuffix = if (houseItem.location.isNotBlank()) " - ${houseItem.location}" else ""
                                    Text("${houseItem.houseName}$locationSuffix")
                                },
                                onClick = {
                                    houseId = houseItem.houseName
                                    isHouseDropdownExpanded = false
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
                    if (tenantName.isNotBlank()) {
                        val updated = (tenant ?: Tenant()).copy(
                            tenantName = tenantName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            houseId = houseId.trim()
                        )
                        onSave(updated)
                    }
                }
            ) {
                Text("Save Tenant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
