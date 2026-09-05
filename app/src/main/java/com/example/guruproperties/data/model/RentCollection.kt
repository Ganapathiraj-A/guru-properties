package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class RentCollection(
    @DocumentId
    var docId: String = "",
    @get:PropertyName("sNo") @set:PropertyName("sNo")
    var sNo: Int = 0,
    var houseId: String = "",
    var pendingAmt: Double = 0.0,
    var paidAmt: Double = 0.0,
    var paidDT: String = "",
    var paidBy: String = "",
    var paidThru: String = ""
) {
    @PropertyName("sno")
    fun setSno(s: Int) {
        if (sNo == 0 && s != 0) {
            sNo = s
        }
    }
}
