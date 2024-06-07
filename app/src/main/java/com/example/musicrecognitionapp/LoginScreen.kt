package com.example.musicrecognitionapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: NavController, onLoginSuccess: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() == 1) {
                        val email = documents.first().getString("email")
                        if (email != null) {
                            FirebaseAuth.getInstance()
                                .signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onLoginSuccess()
                                        navController.navigate("main") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = true
                                            }
                                        }
                                    } else {
                                        errorMessage = task.exception?.message
                                    }
                                }
                        } else {
                            errorMessage = "Email not found for the provided nickname"
                        }
                    } else {
                        errorMessage = "User not found"
                    }
                }
                .addOnFailureListener {
                    errorMessage = it.message
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
