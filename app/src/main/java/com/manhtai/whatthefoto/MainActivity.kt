package com.manhtai.whatthefoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.startActivity
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Main()
        }
    }
}

@Composable
fun Main(photoAPI: PhotoApiService = PhotoApi.service) {
    var apiUrl by remember { mutableStateOf("https://api.thecatapi.com/v1/images/search?limit=10") }
    var imageUrl by remember { mutableStateOf("https://cdn2.thecatapi.com/images/7_rjG2-pc.jpg") }
    var loading by remember { mutableStateOf(true) }
    var isConfigPopupVisible by remember { mutableStateOf(false) }
    var imageLoopSeconds by remember { mutableStateOf(30L) }

    val TAG = "Main"
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    // Start periodic updates
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                loading = true
                try {
                    val response = photoAPI.getPhotos(apiUrl).execute().body()
                    if (response != null) {
                        for (photo in response) {
                            imageUrl = photo.url
                            delay(imageLoopSeconds * 1000)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
                loading = false
                Log.i(TAG, imageUrl)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = imageUrl, label = "Main Image", animationSpec = tween(1000)
        ) { imgUrl ->
            AsyncImage(model = imgUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isConfigPopupVisible = true
                        }
                    })
        }


        // Show Configuration Popup
        if (isConfigPopupVisible) {
            ConfigurationPopup(
                context,
                onDismiss = { isConfigPopupVisible = false },
                onSave = { newApiUrl, seconds ->
                    apiUrl = newApiUrl
                    imageLoopSeconds = seconds
                },
                apiUrl,
                imageLoopSeconds
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Main()
}


@Composable
fun ConfigurationPopup(
    ctx: Context,
    onDismiss: () -> Unit, onSave: (String, Long) -> Unit, oldUrl: String, oldDelay: Long
) {
    var apiUrl by remember { mutableStateOf(oldUrl) }
    var delaySeconds by remember { mutableLongStateOf(oldDelay) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        intent.putExtra("only_access_points", true);
                        intent.putExtra("extra_prefs_show_button_bar", true);
                        intent.putExtra("wifi_enable_next_on_connect", true);
                        startActivity(ctx, intent, Bundle())
                    }) {
                        Text("WiFi Settings")
                    }
                }

                TextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API URL that return: [{url: image_url}]") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                )

                TextField(value = delaySeconds.toString(),
                    label = { Text(text = "Each image delay in seconds") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    onValueChange = { delaySeconds = it.toLong() })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        onSave(apiUrl, delaySeconds)
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
