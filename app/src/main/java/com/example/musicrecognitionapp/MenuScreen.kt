package com.example.musicrecognitionapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MenuScreen(navController: NavController, isLoggedIn: Boolean, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top, // Postavlja elemente od vrha
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Menu",
            fontSize = 24.sp,
            color = Color(0xFF89CFF0),  // Baby Blue boja
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp) // Doda razmak od vrha ekrana
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn) {
                Button(onClick = {
                    onLogout()
                    navController.navigate("main") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }) {
                    Text("Log off")
                }
            } else {
                Button(onClick = { navController.navigate("login") }) {
                    Text("Log In")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("register") }) {
                    Text("Register")
                }
            }
        }
    }
}
