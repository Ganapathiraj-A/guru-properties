package com.example.guruproperties.data.repository

import android.util.Log
import com.example.guruproperties.data.model.AppUser
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.RentCollection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PropertyRepository {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("PropertyRepository", "FirebaseAuth not available", e)
            null
        }
    }

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("PropertyRepository", "Firebase not initialized. Running in local fallback mode.", e)
            null
        }
    }

    // Demo/Local Current User State
    val currentUserState = MutableStateFlow<AppUser?>(
        AppUser(
            docId = "u1",
            uid = "demo_admin_uid",
            email = "admin@guruproperties.com",
            displayName = "Guru Property Admin",
            role = "Admin",
            status = "Active",
            addedAt = "2026-01-01"
        )
    )

    // Local App Users List
    private val localUsers = MutableStateFlow<List<AppUser>>(
        listOf(
            AppUser(
                docId = "u1",
                uid = "demo_admin_uid",
                email = "admin@guruproperties.com",
                displayName = "Guru Property Admin",
                role = "Admin",
                status = "Active",
                addedAt = "2026-01-01"
            ),
            AppUser(
                docId = "u2",
                uid = "demo_manager_uid",
                email = "manager@guruproperties.com",
                displayName = "Property Manager",
                role = "Manager",
                status = "Active",
                addedAt = "2026-03-15"
            )
        )
    )

    // Local in-memory state for fallback/offline demo
    private val localHouses = MutableStateFlow<List<House>>(
        listOf(
            House(
                docId = "h1",
                sNo = 1,
                houseId = "H101",
                houseName = "Guru Villa 1A",
                location = "Main Road, Sector 4",
                monthlyRent = 15000.0,
                advance = 50000.0,
                monthlyRentRevision = 16500.0,
                revisionDate = "2026-12-01",
                tenancyDate = "2025-01-15",
                tenantName = "Rajesh Kumar",
                phoneNumber = "9876543210"
            ),
            House(
                docId = "h2",
                sNo = 2,
                houseId = "H102",
                houseName = "Guru Residency 2B",
                location = "Lake View, Phase 2",
                monthlyRent = 22000.0,
                advance = 70000.0,
                monthlyRentRevision = 24000.0,
                revisionDate = "2027-03-01",
                tenancyDate = "2024-06-01",
                tenantName = "Anita Sharma",
                phoneNumber = "9123456789"
            )
        )
    )

    private val localCollections = MutableStateFlow<List<RentCollection>>(
        listOf(
            RentCollection(
                docId = "c1",
                sNo = 1,
                houseId = "H101",
                pendingAmt = 0.0,
                paidAmt = 15000.0,
                paidDT = "2026-08-05 10:30 AM",
                paidBy = "Rajesh Kumar",
                paidThru = "UPI"
            ),
            RentCollection(
                docId = "c2",
                sNo = 2,
                houseId = "H102",
                pendingAmt = 2000.0,
                paidAmt = 20000.0,
                paidDT = "2026-08-02 04:15 PM",
                paidBy = "Anita Sharma",
                paidThru = "Bank Transfer"
            )
        )
    )

    fun getCurrentFirebaseUser(): FirebaseUser? = auth?.currentUser

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Error signing out", e)
        }
        currentUserState.value = null
    }

    fun loginAsDemoUser(email: String = "admin@guruproperties.com", name: String = "Guru Property Admin") {
        val user = AppUser(
            docId = "demo_${System.currentTimeMillis()}",
            uid = "uid_${email.hashCode()}",
            email = email,
            displayName = name,
            role = "Admin",
            status = "Active",
            addedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        currentUserState.value = user
    }

    fun getUsersFlow(): Flow<List<AppUser>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            val job = launch {
                localUsers.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
        } else {
            val listener = firestore.collection("app_users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PropertyRepository", "Firestore users listener error", error)
                        trySend(localUsers.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val users = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(AppUser::class.java)?.copy(docId = doc.id)
                        }
                        if (users.isNotEmpty()) {
                            localUsers.value = users
                        }
                        trySend(localUsers.value)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveUser(user: AppUser) {
        val firestore = db
        if (firestore != null) {
            try {
                if (user.docId.isBlank()) {
                    firestore.collection("app_users").add(user)
                } else {
                    firestore.collection("app_users").document(user.docId).set(user)
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error saving user to Firestore", e)
                saveUserLocally(user)
            }
        } else {
            saveUserLocally(user)
        }
    }

    private fun saveUserLocally(user: AppUser) {
        val currentList = localUsers.value.toMutableList()
        val index = currentList.indexOfFirst { it.docId == user.docId || (it.email.equals(user.email, ignoreCase = true) && user.email.isNotBlank()) }
        val formattedDate = if (user.addedAt.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else user.addedAt
        if (index >= 0) {
            currentList[index] = user.copy(addedAt = formattedDate)
        } else {
            val newUser = user.copy(
                docId = if (user.docId.isBlank()) "u_${System.currentTimeMillis()}" else user.docId,
                addedAt = formattedDate
            )
            currentList.add(newUser)
        }
        localUsers.value = currentList
    }

    suspend fun deleteUser(docId: String) {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("app_users").document(docId).delete()
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error deleting user", e)
            }
        }
        localUsers.value = localUsers.value.filterNot { it.docId == docId }
    }

    fun getHousesFlow(): Flow<List<House>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            val job = launch {
                localHouses.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
        } else {
            val listener = firestore.collection("houses")
                .orderBy("sNo", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PropertyRepository", "Firestore house listener error", error)
                        trySend(localHouses.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val houses = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(House::class.java)?.copy(docId = doc.id)
                        }
                        if (houses.isNotEmpty()) {
                            localHouses.value = houses
                        }
                        trySend(localHouses.value)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    fun getCollectionsFlow(): Flow<List<RentCollection>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            val job = launch {
                localCollections.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
        } else {
            val listener = firestore.collection("collections")
                .orderBy("sNo", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PropertyRepository", "Firestore collections listener error", error)
                        trySend(localCollections.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val collections = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(RentCollection::class.java)?.copy(docId = doc.id)
                        }
                        if (collections.isNotEmpty()) {
                            localCollections.value = collections
                        }
                        trySend(localCollections.value)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveHouse(house: House) {
        val firestore = db
        if (firestore != null) {
            try {
                if (house.docId.isBlank()) {
                    firestore.collection("houses").add(house)
                } else {
                    firestore.collection("houses").document(house.docId).set(house)
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error saving house to Firestore", e)
                saveHouseLocally(house)
            }
        } else {
            saveHouseLocally(house)
        }
    }

    private fun saveHouseLocally(house: House) {
        val currentList = localHouses.value.toMutableList()
        val index = currentList.indexOfFirst { it.docId == house.docId || (it.houseId.equals(house.houseId, ignoreCase = true) && house.houseId.isNotBlank()) }
        if (index >= 0) {
            currentList[index] = house
        } else {
            val nextSNo = (currentList.maxOfOrNull { it.sNo } ?: 0) + 1
            val newHouse = house.copy(
                docId = if (house.docId.isBlank()) "h_${System.currentTimeMillis()}" else house.docId,
                sNo = if (house.sNo <= 0) nextSNo else house.sNo
            )
            currentList.add(newHouse)
        }
        localHouses.value = currentList
    }

    suspend fun deleteHouse(docId: String) {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("houses").document(docId).delete()
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error deleting house", e)
            }
        }
        localHouses.value = localHouses.value.filterNot { it.docId == docId }
    }

    suspend fun saveCollection(collection: RentCollection) {
        val firestore = db
        if (firestore != null) {
            try {
                if (collection.docId.isBlank()) {
                    firestore.collection("collections").add(collection)
                } else {
                    firestore.collection("collections").document(collection.docId).set(collection)
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error saving collection to Firestore", e)
                saveCollectionLocally(collection)
            }
        } else {
            saveCollectionLocally(collection)
        }
    }

    private fun saveCollectionLocally(collection: RentCollection) {
        val currentList = localCollections.value.toMutableList()
        val index = currentList.indexOfFirst { it.docId == collection.docId }
        if (index >= 0) {
            currentList[index] = collection
        } else {
            val nextSNo = (currentList.maxOfOrNull { it.sNo } ?: 0) + 1
            val formattedDate = if (collection.paidDT.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
            } else collection.paidDT

            val newColl = collection.copy(
                docId = if (collection.docId.isBlank()) "c_${System.currentTimeMillis()}" else collection.docId,
                sNo = if (collection.sNo <= 0) nextSNo else collection.sNo,
                paidDT = formattedDate
            )
            currentList.add(newColl)
        }
        localCollections.value = currentList
    }

    suspend fun deleteCollection(docId: String) {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("collections").document(docId).delete()
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error deleting collection", e)
            }
        }
        localCollections.value = localCollections.value.filterNot { it.docId == docId }
    }
}
