package com.example.guruproperties.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class AppUser(
    @DocumentId
    val docId: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "Manager",
    val status: String = "Active",
    val addedAt: String = ""
)

