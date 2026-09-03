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
                houseId = "Guru Villa 1A",
                addedAt = "2025-01-15"
            ),
            Tenant(
                docId = "t2",
                tenantId = "T102",
                tenantName = "Anita Sharma",
                phoneNumber = "9123456789",
                houseId = "Guru Residency 2B",
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
                houseId = "Guru Villa 1A",
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
                houseId = "Guru Residency 2B",
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
                houseId = "Guru Villa 1A",
                pendingAmt = 0.0,
                paidAmt = 15000.0,
                paidDT = "2026-08-05 10:30 AM",
                paidBy = "Rajesh Kumar",
                paidThru = "UPI"
            ),
            RentCollection(
                docId = "c2",
                sNo = 2,
                houseId = "Guru Residency 2B",
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
        val firestore = db
        saveUserLocally(user)
        if (firestore != null) {
            try {
                if (user.docId.isBlank()) {
                    firestore.collection("app_users").add(user)
                } else {
                    firestore.collection("app_users").document(user.docId).set(user)
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error saving user to Firestore", e)
            }
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
        val firestore = db
        saveTenantLocally(tenant)
        if (firestore != null) {
            try {
                if (tenant.docId.isBlank()) {
                    firestore.collection("tenants").add(tenant)
                } else {
                    firestore.collection("tenants").document(tenant.docId).set(tenant)
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error saving tenant to Firestore", e)
            }
        }
    }

    private fun saveTenantLocally(tenant: Tenant) {
        val currentList = localTenants.value.toMutableList()
        val index = currentList.indexOfFirst { it.docId == tenant.docId || (it.tenantName.equals(tenant.tenantName, ignoreCase = true) && tenant.tenantName.isNotBlank()) }
        val formattedDate = if (tenant.addedAt.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else tenant.addedAt
        if (index >= 0) {
            currentList[index] = tenant.copy(addedAt = formattedDate)
        } else {
            val nextId = "T${100 + currentList.size + 1}"
            val newTenant = tenant.copy(
                docId = if (tenant.docId.isBlank()) "t_${System.currentTimeMillis()}" else tenant.docId,
                tenantId = if (tenant.tenantId.isBlank()) nextId else tenant.tenantId,
                addedAt = formattedDate
            )
            currentList.add(newTenant)
        }
        localTenants.value = currentList
    }

    suspend fun deleteTenant(docId: String) {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("tenants").document(docId).delete()
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Error deleting tenant", e)
            }
        }
        localTenants.value = localTenants.value.filterNot { it.docId == docId }
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
        saveHouseLocally(house)
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
            }
        }
    }

    private fun saveHouseLocally(house: House) {
        val currentList = localHouses.value.toMutableList()
        val index = currentList.indexOfFirst { it.docId == house.docId || (it.houseName.equals(house.houseName, ignoreCase = true) && house.houseName.isNotBlank()) }
        if (index >= 0) {
            currentList[index] = house
        } else {
            val nextSNo = (currentList.maxOfOrNull { it.sNo } ?: 0) + 1
            val newHouse = house.copy(
                docId = if (house.docId.isBlank()) "h_${System.currentTimeMillis()}" else house.docId,
                houseId = house.houseName,
                sNo = if (house.sNo <= 0) nextSNo else house.sNo
            )
            currentList.add(newHouse)
        }
        localHouses.value = currentList

        // Auto-add tenant to tenant list if not present
        if (house.tenantName.isNotBlank()) {
            val existingTenants = localTenants.value
            if (!existingTenants.any { it.tenantName.equals(house.tenantName, ignoreCase = true) }) {
                saveTenantLocally(
                    Tenant(
                        tenantId = "T${100 + existingTenants.size + 1}",
                        tenantName = house.tenantName,
                        phoneNumber = house.phoneNumber,
                        houseId = house.houseName
                    )
                )
            }
        }
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
        saveCollectionLocally(collection)
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
            }
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

        // Auto-add paidBy as a tenant if not present
        if (collection.paidBy.isNotBlank()) {
            val existingTenants = localTenants.value
            if (!existingTenants.any { it.tenantName.equals(collection.paidBy, ignoreCase = true) }) {
                saveTenantLocally(
                    Tenant(
                        tenantId = "T${100 + existingTenants.size + 1}",
                        tenantName = collection.paidBy,
                        houseId = collection.houseId
                    )
                )
            }
        }
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
