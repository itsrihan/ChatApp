package com.ri.lag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.ri.lag.data.ChatUser
import com.ri.lag.screens.*
import com.ri.lag.ui.theme.LagTheme
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set up persistence (do this only once at app startup)
        try {
            database.setPersistenceEnabled(true)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) {
            // Persistence might already be enabled
        }
        
        setContent {
            LagTheme {
                var currentScreen by remember { mutableStateOf("splash") }
                var selectedUser by remember { mutableStateOf<ChatUser?>(null) }
                var showExitDialog by remember { mutableStateOf(false) }
                
                // Handle back navigation
                BackHandler(enabled = true) {
                    when (currentScreen) {
                        "home" -> {
                            showExitDialog = true
                        }
                        "chat" -> {
                            currentScreen = "home"
                        }
                        "allUsers" -> {
                            currentScreen = "home"
                        }
                        "register" -> {
                            currentScreen = "login"
                        }
                        "login" -> {
                            showExitDialog = true
                        }
                    }
                }

                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("Exit App") },
                        text = { Text("Are you sure you want to exit?") },
                        confirmButton = {
                            Button(
                                onClick = { 
                                    showExitDialog = false
                                    finish() 
                                }
                            ) {
                                Text("Exit")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showExitDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Check if user is already logged in when splash screen completes
                fun onSplashComplete() {
                    currentScreen = if (auth.currentUser != null) {
                        "home"
                    } else {
                        "login"
                    }
                }
                
                when (currentScreen) {
                    "splash" -> SplashScreen(
                        onSplashComplete = { onSplashComplete() }
                    )
                    "login" -> LoginScreen(
                        onRegisterClick = { currentScreen = "register" },
                        onLoginSuccess = { currentScreen = "home" }
                    )
                    "register" -> RegisterScreen(
                        onLoginClick = { currentScreen = "login" }
                    )
                    "home" -> HomeScreen(
                        onLogout = { 
                            auth.signOut()
                            currentScreen = "login"
                        },
                        onUserSelected = { user ->
                            selectedUser = user
                            currentScreen = "chat"
                        },
                        onAddUserClick = { currentScreen = "allUsers" }
                    )
                    "allUsers" -> AllUsersScreen(
                        onBackClick = { currentScreen = "home" },
                        onUserSelected = { user ->
                            selectedUser = user
                            currentScreen = "chat"
                        }
                    )
                    "chat" -> selectedUser?.let { user ->
                        ChatScreen(
                            currentUserId = auth.currentUser?.uid ?: "",
                            receiverId = user.userId,
                            receiverName = user.name,
                            onBackClick = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}