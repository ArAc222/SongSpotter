package com.example.musicrecognitionapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun HistoryScreen() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("songs")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            songs = querySnapshot.documents.mapNotNull { document ->
                val song = document.getString("song")
                val timestamp = document.getLong("timestamp")
                if (song != null && timestamp != null) {
                    Song(song, timestamp)
                } else {
                    null
                }
            }.sortedByDescending { it.timestamp }
            loading = false
        } else {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "History",
            fontSize = 24.sp,
            color = Color(0xFF89CFF0),  // Baby Blue boja
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Text("Loading...")
        } else {
            songs.forEach { song ->
                Text(text = song.name)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class Song(val name: String, val timestamp: Long)
