package com.ri.lag.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.ri.lag.data.ChatUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onUserSelected: (ChatUser) -> Unit,
    onAddUserClick: () -> Unit
) {
    var activeChats by remember { mutableStateOf<List<ChatUser>>(emptyList()) }
    var showDeleteConfirmation by remember { mutableStateOf<ChatUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val firestore = FirebaseFirestore.getInstance()
    val database = FirebaseDatabase.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val chatsRef = database.reference.child("chats")

    // Load active chats
    LaunchedEffect(Unit) {
        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUid = currentUser?.uid ?: return
                val chatIds = snapshot.children.mapNotNull { it.key }
                
                // Get all chat IDs that involve the current user
                val userChatIds = chatIds.filter { chatId ->
                    chatId.split("-").contains(currentUid)
                }

                // Get the other user IDs from these chats
                val otherUserIds = userChatIds.map { chatId ->
                    chatId.split("-").first { it != currentUid }
                }

                // If no chats exist, show empty state
                if (otherUserIds.isEmpty()) {
                    activeChats = emptyList()
                    isLoading = false
                    return
                }

                // Fetch user details for these IDs
                if (otherUserIds.isNotEmpty()) {
                    firestore.collection("users")
                        .whereIn("userId", otherUserIds)
                        .get()
                        .addOnSuccessListener { documents ->
                            val users = documents.mapNotNull { it.toObject(ChatUser::class.java) }
                            activeChats = users.sortedBy { it.name }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            isLoading = false
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chat App") },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, "Logout")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddUserClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Icon(Icons.Default.Add, "Add User")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    activeChats.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No active chats",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Click + to start a new chat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeChats) { user ->
                                UserItemWithDelete(
                                    user = user,
                                    onClick = { onUserSelected(user) },
                                    onDeleteClick = { showDeleteConfirmation = user }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteConfirmation?.let { user ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Chat") },
                text = { Text("Are you sure you want to delete chat with ${user.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val chatId = getChatId(currentUser?.uid ?: "", user.userId)
                            database.reference.child("chats").child(chatId).removeValue()
                            showDeleteConfirmation = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun UserItemWithDelete(
    user: ChatUser,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Delete Icon with light grey color
            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun getChatId(uid1: String, uid2: String): String {
    return if (uid1 < uid2) "$uid1-$uid2" else "$uid2-$uid1"
} 