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
import coil.executeBlocking
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
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
    val TAG = "Main"
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    var conf by remember { mutableStateOf(Config()) }
    var imageUrl by remember { mutableStateOf(conf.defaultImage) }
    var isConfigPopupVisible by remember { mutableStateOf(false) }
    var isScreenOn by remember { mutableStateOf(true) }

    // Start periodic updates
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context = context)
            val dbConf = db.configDao().get()
            if (dbConf == null) {
               db.configDao().insert(conf)
            } else {
                conf = dbConf
            }

            while (true) {
                // Sleep
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                isScreenOn = if (conf.sleepFromHour < conf.sleepToHour) {
                    hour < conf.sleepFromHour || hour > conf.sleepToHour
                } else {
                    hour in conf.sleepToHour..conf.sleepFromHour
                }

                Log.i(TAG, "Screen is" + if (isScreenOn) " ON." else " OFF.")
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
                try {
                    val response = photoAPI.getPhotos(conf.apiURL).execute().body()
                    if (response != null) {
                        for (photo in response) {
                            val req = ImageRequest.Builder(context = context)
                                .data(photo.url)
                                .size(Size.ORIGINAL)
                                .build()

                            val res = context.imageLoader.executeBlocking(req)
                            if (res is SuccessResult) {
                                imageUrl = photo.url
                                delay(conf.imageDelaySeconds.seconds)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
                Log.i(TAG, imageUrl)
            }
        }
    }

    // Display images
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = imageUrl,
            label = "Main Image",
            animationSpec = tween(conf.imageFadeSeconds * 1000)
        ) { imgUrl ->
            AsyncImage(model = ImageRequest.Builder(context = context)
                .data(imgUrl)
                .size(Size.ORIGINAL)
                .build(),
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
                onSave = { c ->
                    conf = c
                    coroutineScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(context = context)
                        db.configDao().insert(conf)
                    }
                },
                conf
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
