package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class RentCollection(
    @DocumentId
    var docId: String = "",
    var houseId: String = "",
    var pendingAmt: Double = 0.0,
    var paidAmt: Double = 0.0,
    var paidDT: String = "",
    var paidBy: String = "",
    var paidThru: String = ""
)

