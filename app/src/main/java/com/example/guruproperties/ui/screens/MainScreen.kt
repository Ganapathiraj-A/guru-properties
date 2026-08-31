package com.example.guruproperties.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guruproperties.data.model.AppUser
import com.example.guruproperties.data.model.House
import com.example.guruproperties.data.model.RentCollection
import com.example.guruproperties.ui.dialogs.AddEditCollectionDialog
import com.example.guruproperties.ui.dialogs.AddEditHouseDialog
import com.example.guruproperties.ui.dialogs.AddEditUserDialog
import com.example.guruproperties.ui.viewmodel.MainViewModel

private const val APK_URL = "https://github.com/Ganapathiraj-A/guru-properties/releases/download/latest/manageProperties.apk"

enum class CurrentView {
    MAIN,
    SETTINGS,
    USER_MANAGEMENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (currentUser == null) {
        LoginScreen(
            onLoginSuccess = { email, name ->
                viewModel.loginAsDemoUser(email, name)
            }
        )
        return
    }

    var currentView by remember { mutableStateOf(CurrentView.MAIN) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val houses by viewModel.houses.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val users by viewModel.users.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var editingHouse by remember { mutableStateOf<House?>(null) }
    var showAddHouseDialog by remember { mutableStateOf(false) }

    var editingCollection by remember { mutableStateOf<RentCollection?>(null) }
    var showAddCollectionDialog by remember { mutableStateOf(false) }

    var editingUser by remember { mutableStateOf<AppUser?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    when (currentView) {
        CurrentView.SETTINGS -> {
            SettingsScreen(
                currentUser = currentUser,
                onBack = { currentView = CurrentView.MAIN },
                onNavigateToUserManagement = { currentView = CurrentView.USER_MANAGEMENT },
                onSignOut = {
                    viewModel.signOut()
                    currentView = CurrentView.MAIN
                }
            )
        }
        CurrentView.USER_MANAGEMENT -> {
            UserManagementScreen(
                users = users,
                onBack = { currentView = CurrentView.SETTINGS },
                onAddUser = {
                    editingUser = null
                    showAddUserDialog = true
                },
                onEditUser = { user ->
                    editingUser = user
                    showAddUserDialog = true
                },
                onDeleteUser = { docId ->
                    viewModel.deleteUser(docId)
                }
            )
        }
        CurrentView.MAIN -> {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Guru Properties",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Cloud Synced",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "Logged in: ${currentUser?.displayName ?: ""} (${currentUser?.email ?: ""})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { currentView = CurrentView.SETTINGS }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { viewModel.signOut() }) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Sign Out",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search properties or rent records...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // Tabs: Tab 0 = Rent, Tab 1 = Properties
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Rent (${collections.size})") },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Properties (${houses.size})") },
                                icon = { Icon(Icons.Default.HomeWork, contentDescription = null) }
                            )
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text("Check / Download APK Update")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(APK_URL))
                                    Toast.makeText(context, "APK Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy APK URL",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (selectedTab == 0) {
                                editingCollection = null
                                showAddCollectionDialog = true
                            } else {
                                editingHouse = null
                                showAddHouseDialog = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item"
                        )
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        0 -> RentCollectionScreen(
                            collections = collections,
                            onEditCollection = { collection ->
                                editingCollection = collection
                                showAddCollectionDialog = true
                            },
                            onDeleteCollection = { docId ->
                                viewModel.deleteCollection(docId)
                            }
                        )
                        1 -> PropertyListScreen(
                            houses = houses,
                            onEditHouse = { house ->
                                editingHouse = house
                                showAddHouseDialog = true
                            },
                            onDeleteHouse = { docId ->
                                viewModel.deleteHouse(docId)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddHouseDialog) {
        AddEditHouseDialog(
            house = editingHouse,
            onDismiss = { showAddHouseDialog = false },
            onSave = { house ->
                viewModel.saveHouse(house)
                showAddHouseDialog = false
            }
        )
    }

    if (showAddCollectionDialog) {
        AddEditCollectionDialog(
            collection = editingCollection,
            availableHouses = houses,
            onDismiss = { showAddCollectionDialog = false },
            onSave = { collection ->
                viewModel.saveCollection(collection)
                showAddCollectionDialog = false
            }
        )
    }

    if (showAddUserDialog) {
        AddEditUserDialog(
            user = editingUser,
            onDismiss = { showAddUserDialog = false },
            onSave = { user ->
                viewModel.saveUser(user)
                showAddUserDialog = false
            }
        )
    }
}
