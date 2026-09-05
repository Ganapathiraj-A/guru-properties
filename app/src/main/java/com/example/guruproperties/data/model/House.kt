package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class House(
    @DocumentId
    var docId: String = "",
    @get:PropertyName("sNo") @set:PropertyName("sNo")
    var sNo: Int = 0,
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
    // Secondary property setter for fallback lowercase 'sno'
    @PropertyName("sno")
    fun setSno(s: Int) {
        if (sNo == 0 && s != 0) {
            sNo = s
        }
    }
}
