package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class House(
    @DocumentId
    var docId: String = "",
    @get:PropertyName("sNo") @set:PropertyName("sNo")
    var sNo: Long = 0L,
    @get:PropertyName("sno") @set:PropertyName("sno")
    var sno: Long = 0L,
    var houseId: String = "",
    var houseName: String = "",
    var location: String = "",
    var monthlyRent: Double = 0.0,
    var advance: Double = 0.0,
    var monthlyRentRevision: Double = 0.0,
    var revisionDate: String = "",
    var tenancyDate: String = "",
    var tenantName: String = "",
    var phoneNumber: String = ""
) {
    val displaySNo: Long
        get() = if (sNo > 0L) sNo else sno
}

