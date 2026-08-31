package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId

data class RentCollection(
    @DocumentId
    val docId: String = "",
    val sNo: Int = 0,
    val houseId: String = "",
    val pendingAmt: Double = 0.0,
    val paidAmt: Double = 0.0,
    val paidDT: String = "",
    val paidBy: String = "",
    val paidThru: String = ""
)
