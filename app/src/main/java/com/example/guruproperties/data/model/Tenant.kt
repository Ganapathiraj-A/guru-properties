package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Tenant(
    @DocumentId
    val docId: String = "",
    val tenantId: String = "",
    val tenantName: String = "",
    val phoneNumber: String = "",
    val houseId: String = "",
    val addedAt: String = ""
)

