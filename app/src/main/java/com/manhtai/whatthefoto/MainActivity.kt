package com.manhtai.whatthefoto

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
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
    var apiUrl by remember { mutableStateOf("https://api.thecatapi.com/v1/images/search?limit=10") }
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
