package com.example.myplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) { App(vm) } } }
    }
}

@Composable
fun App(vm: MainViewModel) {
    var playing by remember { mutableStateOf<Channel?>(null) }
    val ch = playing
    if (ch != null) PlayerScreen(ch) { playing = null }
    else if (!vm.loggedIn) LoginScreen(vm)
    else ListScreen(vm) { playing = it }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل الدخول", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = vm.username, onValueChange = { vm.username = it },
            label = { Text("اسم المستخدم") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.password, onValueChange = { vm.password = it },
            label = { Text("كلمة المرور") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.login() }, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) { Text("دخول") }
        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(vm: MainViewModel, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(vm.section == "live", { vm.setSection("live") }, { Text("البث المباشر") })
            FilterChip(vm.section == "vod", { vm.setSection("vod") }, { Text("الأفلام") })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.refresh() }) { Text("تحديث") }
            TextButton(onClick = { vm.logout() }) { Text("خروج") }
        }
        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
