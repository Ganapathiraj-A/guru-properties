package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class RentCollection(
    @DocumentId
    var docId: String = "",
    @get:PropertyName("sNo") @set:PropertyName("sNo")
    var sNo: Long = 0L,
    @get:PropertyName("sno") @set:PropertyName("sno")
    var sno: Long = 0L,
    var houseId: String = "",
    var pendingAmt: Double = 0.0,
    var paidAmt: Double = 0.0,
    var paidDT: String = "",
    var paidBy: String = "",
    var paidThru: String = ""
) {
    val displaySNo: Long
        get() = if (sNo > 0L) sNo else sno
}

