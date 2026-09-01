package com.example.guruproperties.data.repository

import android.util.Log
import com.example.guruproperties.data.model.AppUser
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.RentCollection
import com.example.guruproperties.data.model.Tenant
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

    // Default Logged In User State starts as null so user must authenticate
    val currentUserState = MutableStateFlow<AppUser?>(null)

    // Initial Authorized Users List with Gururajan Jayamani as default Admin & ganapathiraj@gmail.com
    private val localUsers = MutableStateFlow<List<AppUser>>(
        listOf(
            AppUser(
                docId = "u_default_admin",
                uid = "uid_gururajan",
                email = "gururajan.jayamani@gmail.com",
                displayName = "Gururajan Jayamani",
                role = "Admin",
                status = "Active",
                addedAt = "2026-01-01"
            ),
            AppUser(
                docId = "u_ganapathiraj",
                uid = "uid_ganapathiraj",
                email = "ganapathiraj@gmail.com",
                displayName = "Ganapathiraj",
                role = "Admin",
                status = "Active",
                addedAt = "2026-01-01"
            ),
            AppUser(
                docId = "u_admin_fallback",
                uid = "uid_admin_fallback",
                email = "admin@guruproperties.com",
                displayName = "Guru Property Admin",
                role = "Admin",
                status = "Active",
                addedAt = "2026-01-01"
            ),
            AppUser(
                docId = "u_manager_fallback",
                uid = "uid_manager_fallback",
                email = "manager@guruproperties.com",
                displayName = "Property Manager",
                role = "Manager",
                status = "Active",
                addedAt = "2026-03-15"
            )
        )
    )

    // Local Tenants List
    private val localTenants = MutableStateFlow<List<Tenant>>(
        listOf(
            Tenant(
                docId = "t1",
                tenantId = "T101",
                tenantName = "Rajesh Kumar",
                phoneNumber = "9876543210",
                houseId = "H101",
                addedAt = "2025-01-15"
            ),
            Tenant(
                docId = "t2",
                tenantId = "T102",
                tenantName = "Anita Sharma",
                phoneNumber = "9123456789",
                houseId = "H102",
                addedAt = "2024-06-01"
            )
        )
    )

    // Local in-memory state for houses
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

    /**
     * Verifies if email is authorized in current user registry.
     * Returns Result.success(AppUser) if authorized, or Result.failure(Exception) if unauthorized.
     */
    fun attemptUserLogin(email: String, name: String): Result<AppUser> {
        val cleanEmail = email.trim().lowercase()
        val allUsers = localUsers.value
        
        val matchedUser = allUsers.find { it.email.trim().lowercase() == cleanEmail }

        if (matchedUser != null) {
            if (matchedUser.status.equals("Inactive", ignoreCase = true)) {
                return Result.failure(Exception("Account ($cleanEmail) has been set to Inactive by Admin."))
            }
            val activeUser = matchedUser.copy(displayName = if (matchedUser.displayName.isNotBlank()) matchedUser.displayName else name)
            currentUserState.value = activeUser
            return Result.success(activeUser)
        }

        // Special case: Default admins gururajan.jayamani@gmail.com and ganapathiraj@gmail.com are always authorized
        if (cleanEmail == "gururajan.jayamani@gmail.com" || cleanEmail == "ganapathiraj@gmail.com") {
            val defaultAdmin = AppUser(
                docId = "u_${cleanEmail.hashCode()}",
                uid = "uid_${cleanEmail.hashCode()}",
                email = cleanEmail,
                displayName = if (name.isNotBlank() && name != "Google User") name else if (cleanEmail.contains("gururajan")) "Gururajan Jayamani" else "Ganapathiraj",
                role = "Admin",
                status = "Active",
                addedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            currentUserState.value = defaultAdmin
            return Result.success(defaultAdmin)
        }

        return Result.failure(
            Exception("Unauthorized Account: Email '$cleanEmail' is not registered as an authorized user. Please contact Administrator (gururajan.jayamani@gmail.com) to request access.")
        )
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
                            // Merge with default admins to ensure gururajan.jayamani@gmail.com and ganapathiraj@gmail.com exist
                            val merged = (localUsers.value + users).distinctBy { it.email.lowercase() }
                            localUsers.value = merged
                        }
                        trySend(localUsers.value)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveUser(user: AppUser) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized. All data must be saved to Cloud Firestore.")
        if (user.docId.isBlank()) {
            firestore.collection("app_users").add(user)
        } else {
            firestore.collection("app_users").document(user.docId).set(user)
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

    // Tenant Management
    fun getTenantsFlow(): Flow<List<Tenant>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            val job = launch {
                localTenants.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
        } else {
            val listener = firestore.collection("tenants")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PropertyRepository", "Firestore tenants listener error", error)
                        trySend(localTenants.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val tenants = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Tenant::class.java)?.copy(docId = doc.id)
                        }
                        if (tenants.isNotEmpty()) {
                            localTenants.value = tenants
                        }
                        trySend(localTenants.value)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveTenant(tenant: Tenant) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized. All data must be saved to Cloud Firestore.")
        if (tenant.docId.isBlank()) {
            firestore.collection("tenants").add(tenant)
        } else {
            firestore.collection("tenants").document(tenant.docId).set(tenant)
        }
    }

    suspend fun deleteTenant(docId: String) {
        val firestore = db ?: return
        firestore.collection("tenants").document(docId).delete()
    }

    fun getHousesFlow(): Flow<List<House>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
        } else {
            val listener = firestore.collection("houses")
                .orderBy("sNo", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PropertyRepository", "Firestore house listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val houses = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(House::class.java)?.copy(docId = doc.id)
                        }
                        trySend(houses)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    fun getCollectionsFlow(): Flow<List<RentCollection>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
        } else {
            val listener = firestore.collection("collections")
                .orderBy("sNo", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PropertyRepository", "Firestore collections listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val collections = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(RentCollection::class.java)?.copy(docId = doc.id)
                        }
                        trySend(collections)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveHouse(house: House) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized. All data must be saved to Cloud Firestore.")
        if (house.docId.isBlank()) {
            firestore.collection("houses").add(house)
        } else {
            firestore.collection("houses").document(house.docId).set(house)
        }
    }

    suspend fun deleteHouse(docId: String) {
        val firestore = db ?: return
        firestore.collection("houses").document(docId).delete()
    }

    suspend fun saveCollection(collection: RentCollection) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized. All data must be saved to Cloud Firestore.")
        if (collection.docId.isBlank()) {
            firestore.collection("collections").add(collection)
        } else {
            firestore.collection("collections").document(collection.docId).set(collection)
        }
    }

    suspend fun deleteCollection(docId: String) {
        val firestore = db ?: return
        firestore.collection("collections").document(docId).delete()
    }
}
