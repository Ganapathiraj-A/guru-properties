package com.example.guruproperties.data.repository

import android.util.Log
import com.example.guruproperties.data.model.AppUser
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.RentCollection
import com.example.guruproperties.data.model.Tenant
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
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
            Log.e("PropertyRepository", "FirebaseFirestore failed to initialize", e)
            null
        }
    }

    // Default Logged In User State starts as null so user must authenticate
    val currentUserState = MutableStateFlow<AppUser?>(null)

    // Cached live state from Cloud Firestore
    private val cachedUsers = MutableStateFlow<List<AppUser>>(emptyList())

    init {
        // Start live real-time listener for app_users immediately upon app start
        try {
            db?.collection("app_users")?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PropertyRepository", "Error in app_users listener", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AppUser::class.java)?.copy(docId = doc.id)
                    }
                    cachedUsers.value = users
                }
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Error setting up Cloud Firestore app_users listener", e)
        }
    }

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
     * Authenticates user against Cloud Firestore 'app_users' collection in real time.
     */
    fun attemptUserLogin(email: String, name: String): Result<AppUser> {
        val cleanEmail = email.trim().lowercase()

        // 1. Check in memory cache from active Firestore listener
        var matchedUser = cachedUsers.value.find { it.email.trim().lowercase() == cleanEmail }

        // 2. If not yet in cache, fetch directly from Cloud Firestore app_users collection
        if (matchedUser == null && db != null) {
            try {
                val task = db!!.collection("app_users")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                val snapshot = Tasks.await(task)
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    matchedUser = doc.toObject(AppUser::class.java)?.copy(docId = doc.id)
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Cloud Firestore login lookup error: ${e.message}", e)
            }
        }

        // 3. Fallback for primary default admins if cloud query is connecting
        val defaultAdminEmails = listOf(
            "gururajan.jayamani@gmail.com",
            "ganapathiraj@gmail.com",
            "sctvanithathiyagarajan747@gmail.com",
            "haarishgururajan@gmail.com",
            "vaniguru.spareuse@gmail.com"
        )
        if (matchedUser == null && defaultAdminEmails.contains(cleanEmail)) {
            val defaultName = when (cleanEmail) {
                "gururajan.jayamani@gmail.com" -> "Gururajan Jayamani"
                "ganapathiraj@gmail.com" -> "Ganapathiraj"
                "sctvanithathiyagarajan747@gmail.com" -> "Sctvanitha Thiyagarajan"
                "haarishgururajan@gmail.com" -> "Haarish Gururajan"
                "vaniguru.spareuse@gmail.com" -> "Vanitha"
                else -> "Admin"
            }
            matchedUser = AppUser(
                docId = "u_${cleanEmail.hashCode()}",
                uid = "uid_${cleanEmail.hashCode()}",
                email = cleanEmail,
                displayName = if (name.isNotBlank() && name != "Google User") name else defaultName,
                role = "Admin",
                status = "Active",
                addedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
        }

        if (matchedUser != null) {
            if (matchedUser.status.equals("Inactive", ignoreCase = true)) {
                return Result.failure(Exception("Account ($cleanEmail) has been set to Inactive by Admin."))
            }
            val activeUser = matchedUser.copy(displayName = if (matchedUser.displayName.isNotBlank()) matchedUser.displayName else name)
            currentUserState.value = activeUser
            return Result.success(activeUser)
        }

        return Result.failure(
            Exception("Unauthorized Account: Email '$cleanEmail' is not registered in Cloud Firestore. Please contact an Administrator to add your account.")
        )
    }

    fun getUsersFlow(): Flow<List<AppUser>> = callbackFlow {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        val listener = firestore.collection("app_users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PropertyRepository", "Firestore users listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AppUser::class.java)?.copy(docId = doc.id)
                    }
                    cachedUsers.value = users
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveUser(user: AppUser) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        val formattedDate = if (user.addedAt.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else user.addedAt
        val userToSave = user.copy(addedAt = formattedDate)
        if (userToSave.docId.isBlank()) {
            firestore.collection("app_users").add(userToSave)
        } else {
            firestore.collection("app_users").document(userToSave.docId).set(userToSave)
        }
    }

    suspend fun deleteUser(docId: String) {
        val firestore = db ?: return
        firestore.collection("app_users").document(docId).delete()
    }

    // Tenant Management (Cloud Firestore)
    fun getTenantsFlow(): Flow<List<Tenant>> = callbackFlow {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        val listener = firestore.collection("tenants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PropertyRepository", "Firestore tenants listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tenants = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Tenant::class.java)?.copy(docId = doc.id)
                    }
                    trySend(tenants)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveTenant(tenant: Tenant) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        val formattedDate = if (tenant.addedAt.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else tenant.addedAt
        val tenantToSave = tenant.copy(addedAt = formattedDate)
        if (tenantToSave.docId.isBlank()) {
            firestore.collection("tenants").add(tenantToSave)
        } else {
            firestore.collection("tenants").document(tenantToSave.docId).set(tenantToSave)
        }
    }

    suspend fun deleteTenant(docId: String) {
        val firestore = db ?: return
        firestore.collection("tenants").document(docId).delete()
    }

    // Properties Management (Cloud Firestore)
    fun getHousesFlow(): Flow<List<House>> = callbackFlow {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        val listener = firestore.collection("houses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PropertyRepository", "Firestore houses listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val houses = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(House::class.java)?.copy(docId = doc.id)
                    }.sortedWith(compareBy({ if (it.sNo > 0) it.sNo else Int.MAX_VALUE }, { it.houseName }))
                    trySend(houses)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveHouse(house: House) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        try {
            val houseToSave = house.copy(houseId = house.houseName)
            if (houseToSave.docId.isBlank()) {
                val addedDoc = Tasks.await(firestore.collection("houses").add(houseToSave))
                Log.d("PropertyRepository", "House added with ID: ${addedDoc.id}")
            } else {
                Tasks.await(firestore.collection("houses").document(houseToSave.docId).set(houseToSave))
                Log.d("PropertyRepository", "House updated for ID: ${houseToSave.docId}")
            }

            // Auto-create tenant in cloud if occupied
            if (houseToSave.tenantName.isNotBlank()) {
                try {
                    val existing = firestore.collection("tenants").whereEqualTo("tenantName", houseToSave.tenantName).get()
                    val snapshot = Tasks.await(existing)
                    if (snapshot.isEmpty) {
                        Tasks.await(
                            firestore.collection("tenants").add(
                                Tenant(
                                    tenantName = houseToSave.tenantName,
                                    phoneNumber = houseToSave.phoneNumber,
                                    houseId = houseToSave.houseName,
                                    addedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("PropertyRepository", "Error auto-creating tenant: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Error saving house to Firestore: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteHouse(docId: String) {
        val firestore = db ?: return
        try {
            Tasks.await(firestore.collection("houses").document(docId).delete())
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Error deleting house: ${e.message}", e)
            throw e
        }
    }

    // Rent Collection Management (Cloud Firestore)
    fun getCollectionsFlow(): Flow<List<RentCollection>> = callbackFlow {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        val listener = firestore.collection("collections")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PropertyRepository", "Firestore collections listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val collections = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RentCollection::class.java)?.copy(docId = doc.id)
                    }.sortedWith(compareByDescending<RentCollection> { it.paidDT }
                        .thenBy { if (it.sNo > 0) it.sNo else Int.MAX_VALUE })
                    trySend(collections)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCollection(collection: RentCollection) {
        val firestore = db ?: throw IllegalStateException("Cloud Firestore is not initialized.")
        try {
            val formattedDate = if (collection.paidDT.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
            } else collection.paidDT

            val collectionToSave = collection.copy(paidDT = formattedDate)
            if (collectionToSave.docId.isBlank()) {
                val addedDoc = Tasks.await(firestore.collection("collections").add(collectionToSave))
                Log.d("PropertyRepository", "Rent collection added with ID: ${addedDoc.id}")
            } else {
                Tasks.await(firestore.collection("collections").document(collectionToSave.docId).set(collectionToSave))
                Log.d("PropertyRepository", "Rent collection updated for ID: ${collectionToSave.docId}")
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Error saving rent collection: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteCollection(docId: String) {
        val firestore = db ?: return
        try {
            Tasks.await(firestore.collection("collections").document(docId).delete())
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Error deleting collection: ${e.message}", e)
            throw e
        }
    }
}
