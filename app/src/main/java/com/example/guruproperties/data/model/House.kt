package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId

data class House(
    @DocumentId
    val docId: String = "",
    val sNo: Int = 0,
    val houseId: String = "",
    val houseName: String = "",
    val location: String = "",
    val monthlyRent: Double = 0.0,
    val advance: Double = 0.0,
    val monthlyRentRevision: Double = 0.0,
    val revisionDate: String = "",
    val tenancyDate: String = "",
    val tenantName: String = "",
    val phoneNumber: String = ""
)
