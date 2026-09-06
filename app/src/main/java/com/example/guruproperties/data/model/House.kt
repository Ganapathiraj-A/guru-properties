package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class House(
    @DocumentId
    var docId: String = "",
    var houseId: String = "",
    var houseName: String = "",
    var location: String = "",
    var monthlyRent: Double = 0.0,
    var advance: Double = 0.0,
    var tenancyDate: String = "",
    var tenantName: String = "",
    var phoneNumber: String = ""
)

