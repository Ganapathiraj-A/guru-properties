package com.example.guruproperties.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.guruproperties.data.model.Tenant

@Composable
fun AddEditTenantDialog(
    tenant: Tenant?,
    onDismiss: () -> Unit,
    onSave: (Tenant) -> Unit
) {
    var tenantName by remember { mutableStateOf(tenant?.tenantName ?: "") }
    var phoneNumber by remember { mutableStateOf(tenant?.phoneNumber ?: "") }
    var houseId by remember { mutableStateOf(tenant?.houseId ?: "") }

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

                OutlinedTextField(
                    value = houseId,
                    onValueChange = { houseId = it },
                    label = { Text("Associated HouseID (e.g. H101 - Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
