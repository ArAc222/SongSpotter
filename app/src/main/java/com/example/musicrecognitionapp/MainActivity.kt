package com.example.musicrecognitionapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicrecognitionapp.ui.theme.MusicRecognitionAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private lateinit var musicRecognition: MusicRecognition
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

        musicRecognition = MusicRecognition(this)

        setContent {
            val navController = rememberNavController()
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
            var nickname by remember { mutableStateOf("") }

            LaunchedEffect(auth.currentUser) {
                auth.currentUser?.let { user ->
                    FirebaseFirestore.getInstance().collection("users")
                        .document(user.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            nickname = document.getString("nickname") ?: ""
                        }
                }
            }

            MusicRecognitionAppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigationBar(navController)
                    }
                ) { padding ->
                    SetupNavGraph(
                        navController = navController,
                        musicRecognition = musicRecognition,
                        requestMicrophonePermission = { requestMicrophonePermission() },
                        isLoggedIn = isLoggedIn,
                        nickname = nickname,
                        paddingValues = padding,
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                            nickname = ""
                            navController.navigate("main") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        },
                        onLoginSuccess = { newNickname ->
                            isLoggedIn = true
                            nickname = newNickname
                            navController.navigate("main") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                musicRecognition.startRecognition { /* No action needed */ }
                requestNotificationPermission("Recording started")
            } else {
                // Permission is denied. Handle accordingly
            }
        }

    private fun requestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                musicRecognition.startRecognition { /* No action needed */ }
                requestNotificationPermission("Recording started")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showNotification("Recording started")
            }
        }

    private fun requestNotificationPermission(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showNotification(message)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            showNotification(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicRecognition.release()
        cancelNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Recognition"
            val descriptionText = "Notification for music recognition"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("music_recognition_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: String) {
        val builder = NotificationCompat.Builder(this, "music_recognition_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Music Recognition")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1, builder.build())
            }
        }
    }

    private fun cancelNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(1)
        }
    }
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    musicRecognition: MusicRecognition,
    requestMicrophonePermission: () -> Unit,
    isLoggedIn: Boolean,
    nickname: String,
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.padding(paddingValues)
    ) {
        composable("login") { LoginScreen(navController, onLoginSuccess) }
        composable("register") { RegisterScreen(navController) }
        composable("main") {
            MainScreen(
                musicRecognition = musicRecognition,
                requestPermission = requestMicrophonePermission,
                isLoggedIn = isLoggedIn,
                nickname = nickname,
                navController = navController
            )
        }
        composable("history") { HistoryScreen() }
        composable("menu") {
            MenuScreen(
                navController = navController,
                isLoggedIn = isLoggedIn,
                onLogout = onLogout
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("history", Icons.Default.History, "History"),
        BottomNavItem("main", Icons.Default.Home, "Home"),
        BottomNavItem("menu", Icons.Default.Menu, "Menu")
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    musicRecognition: MusicRecognition,
    requestPermission: () -> Unit,
    isLoggedIn: Boolean,
    nickname: String,
    navController: NavController
) {
    var result by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var manualStop by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("Press the button to start recognition") }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(isLoggedIn) {
        musicRecognition.initACRCloud { recognizedResult, success ->
            if (!manualStop) {
                result = if (success) {
                    if (isLoggedIn) {
                        saveToHistory(recognizedResult)
                    }
                    recognizedResult
                } else {
                    "Error recognizing song"
                }
            }
            musicRecognition.stopRecognition()
            isListening = false
            cancelNotification(context)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoggedIn) {
            Text(text = "Hi, $nickname", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = resultText, modifier = Modifier.padding(bottom = 20.dp))
        Box(contentAlignment = Alignment.Center) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(scale)
                        .background(Color(0xFF89CFF0), CircleShape)
                )
            }
            Button(onClick = {
                if (!isListening) {
                    requestPermission()
                    resultText = "Listening..."
                    musicRecognition.startRecognition { /* No action needed */ }
                    isListening = true
                    manualStop = false
                } else {
                    isListening = false
                    manualStop = true
                    musicRecognition.stopRecognition()
                    cancelNotification(context)
                    resultText = "Press the button to start recognition"
                    showNotification(context, "Recording stopped")
                }
            }) {
                Text(text = if (isListening) "Stop Listening" else "Start Listening")
            }
        }
        result?.let {
            if (!manualStop) {
                ShowResultDialog(it) {
                    result = null
                    resultText = "Press the button to start recognition"
                }
            }
        }
    }
}

private fun saveToHistory(song: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    currentUser?.let { user ->
        val firestore = FirebaseFirestore.getInstance()
        val userId = user.uid

        firestore.collection("songs")
            .add(mapOf("userId" to userId, "song" to song, "timestamp" to System.currentTimeMillis()))
    }
}

private fun showNotification(context: Context, message: String) {
    val builder = NotificationCompat.Builder(context, "music_recognition_channel")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Music Recognition")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(context)) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notify(1, builder.build())
        }
    }
}

private fun cancelNotification(context: Context) {
    with(NotificationManagerCompat.from(context)) {
        cancel(1)
    }
}

@Composable
fun ShowResultDialog(result: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    // Play notification sound
    LaunchedEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.notification_sound) // Replace with your sound file
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer.start()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Recognition Result")
        },
        text = {
            Text(text = result)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val navController = rememberNavController()
    MusicRecognitionAppTheme {
        SetupNavGraph(
            navController = navController,
            musicRecognition = MusicRecognition(LocalContext.current),
            requestMicrophonePermission = {},
            isLoggedIn = false,
            nickname = "Preview",
            paddingValues = PaddingValues(),
            onLogout = {},
            onLoginSuccess = {}
        )
    }
}

@Composable
fun LoginScreen(navController: NavController, onLoginSuccess: (String) -> Unit) {
    // Your existing login screen code
    // After successful login:
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: ""
                onLoginSuccess(nickname)
            }
    }
}
