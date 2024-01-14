package com.manhtai.whatthefoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
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
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    var apiUrl by remember { mutableStateOf("https://api.thecatapi.com/v1/images/search?limit=1") }
    var imageUrl by remember { mutableStateOf("https://cdn2.thecatapi.com/images/7_rjG2-pc.jpg") }
    var loading by remember { mutableStateOf(true) }
    var isConfigPopupVisible by remember { mutableStateOf(false) }
    var imageLoopSeconds by remember { mutableLongStateOf(30) }
    var sleepFromHour by remember { mutableLongStateOf(19) }
    var sleepToHour by remember { mutableLongStateOf(8) }
    var isScreenOn by remember { mutableStateOf(true) }

    val TAG = "Main"
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    // Start periodic updates
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                // Sleep
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                isScreenOn = if (sleepFromHour < sleepToHour) {
                    hour < sleepFromHour || hour > sleepToHour
                } else {
                    hour in sleepToHour..sleepFromHour
                }

                Log.i(TAG, isScreenOn.toString())
                if (!isScreenOn) {
                    withContext(Dispatchers.Main) {
                        setBrightness(context, 0f)
                    }
                    delay(1.minutes)
                    continue
                } else {
                    withContext(Dispatchers.Main) {
                        setBrightness(context, -1f)
                    }
                }

                // Load image
                loading = true
                try {
                    val response = photoAPI.getPhotos(apiUrl).execute().body()
                    if (response != null) {
                        for (photo in response) {
                            imageUrl = photo.url
                            delay(imageLoopSeconds.seconds)
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

    // Display images
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
                onSave = { newApiUrl, seconds, fromHour, toHour ->
                    apiUrl = newApiUrl
                    imageLoopSeconds = seconds
                    sleepFromHour = fromHour
                    sleepToHour = toHour
                },
                apiUrl,
                imageLoopSeconds,
                sleepFromHour,
                sleepToHour
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Main()
}

fun setBrightness(context: Context, brightness: Float) {
    val window = (context as? ComponentActivity)?.window
    val layoutParams = window?.attributes
    layoutParams?.flags = layoutParams?.flags?.or(FLAG_KEEP_SCREEN_ON)
    layoutParams?.screenBrightness = brightness
    window?.attributes = layoutParams
}

@Composable
fun ConfigurationPopup(
    ctx: Context,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, Long) -> Unit,
    oldUrl: String,
    oldDelay: Long,
    oldFrom: Long,
    oldTo: Long
) {
    var apiUrl by remember { mutableStateOf(oldUrl) }
    var delaySeconds by remember { mutableLongStateOf(oldDelay) }
    var sleepFrom by remember { mutableLongStateOf(oldFrom) }
    var sleepTo by remember { mutableLongStateOf(oldTo) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
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
                    label = { Text("API (return [{url: image}])") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                )

                TextField(value = delaySeconds.toString(),
                    label = { Text(text = "Image delay (seconds)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    onValueChange = { if (it != "") delaySeconds = it.toLong() })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    TextField(value = sleepFrom.toString(),
                        label = { Text(text = "Sleep from hour (0h-24h)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            if (it != "" && it.toLong() in 0..24) sleepFrom = it.toLong()
                        })

                    TextField(value = sleepTo.toString(),
                        label = { Text(text = "To hour (0h-24h). Now is " + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + "h.") },
                        modifier = Modifier.padding(start = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            if (it != "" && it.toLong() in 0..24) sleepTo = it.toLong()
                        })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        onSave(apiUrl, delaySeconds, sleepFrom, sleepTo)
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
