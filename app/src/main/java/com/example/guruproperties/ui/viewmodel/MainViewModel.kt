package com.example.guruproperties.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guruproperties.data.model.AppUser
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.RentCollection
import com.example.guruproperties.data.model.Tenant
import com.example.guruproperties.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PropertyRepository = PropertyRepository()
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val currentUser: StateFlow<AppUser?> = repository.currentUserState

    val users: StateFlow<List<AppUser>> = repository.getUsersFlow()
        .combine(searchQuery) { userList, query ->
            if (query.isBlank()) {
                userList
            } else {
                userList.filter {
                    it.displayName.contains(query, ignoreCase = true) ||
                            it.email.contains(query, ignoreCase = true) ||
                            it.role.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tenants: StateFlow<List<Tenant>> = repository.getTenantsFlow()
        .combine(searchQuery) { tenantList, query ->
            if (query.isBlank()) {
                tenantList
            } else {
                tenantList.filter {
                    it.tenantName.contains(query, ignoreCase = true) ||
                            it.houseId.contains(query, ignoreCase = true) ||
                            it.phoneNumber.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val houses: StateFlow<List<House>> = repository.getHousesFlow()
        .combine(searchQuery) { houseList, query ->
            if (query.isBlank()) {
                houseList
            } else {
                houseList.filter {
                    it.houseId.contains(query, ignoreCase = true) ||
                            it.houseName.contains(query, ignoreCase = true) ||
                            it.tenantName.contains(query, ignoreCase = true) ||
                            it.location.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<RentCollection>> = repository.getCollectionsFlow()
        .combine(searchQuery) { collectionList, query ->
            if (query.isBlank()) {
                collectionList
            } else {
                collectionList.filter {
                    it.houseId.contains(query, ignoreCase = true) ||
                            it.paidBy.contains(query, ignoreCase = true) ||
                            it.paidThru.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun attemptUserLogin(email: String, name: String): Result<AppUser> {
        return repository.attemptUserLogin(email, name)
    }

    fun signOut() {
        repository.signOut()
    }

    fun saveUser(user: AppUser, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.saveUser(user)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to save user")
            }
        }
    }

    fun deleteUser(docId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.deleteUser(docId)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to delete user")
            }
        }
    }

    fun saveTenant(tenant: Tenant, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.saveTenant(tenant)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to save tenant")
            }
        }
    }

    fun deleteTenant(docId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.deleteTenant(docId)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to delete tenant")
            }
        }
    }

    fun saveHouse(house: House, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.saveHouse(house)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to save property")
            }
        }
    }

    fun deleteHouse(docId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.deleteHouse(docId)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to delete property")
            }
        }
    }

    fun saveCollection(collection: RentCollection, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.saveCollection(collection)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to save rent collection")
            }
        }
    }

    fun deleteCollection(docId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.deleteCollection(docId)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to delete rent collection")
            }
        }
    }
}
