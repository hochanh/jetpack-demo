package com.example.jetpack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.jetpack.ui.theme.JetpackTheme
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
    var apiUrl by remember { mutableStateOf("https://jsonplaceholder.typicode.com/photos") }
    var imageUrl by remember { mutableStateOf("https://flodesk.com/flodesk.png") }
    var loading by remember { mutableStateOf(true) }
    var isConfigPopupVisible by remember { mutableStateOf(false) }

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    // Start periodic updates
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                loading = true
                try {
                    val response = photoAPI.getPhotos(apiUrl).execute().body()
                    if (response != null) {
                        imageUrl = response.first().url
                    }
                } catch (e: Exception) {
                    Log.e("Main", e.toString())
                }
                loading = false
                Log.i("Main", imageUrl)

                delay(3000)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures {
                        isConfigPopupVisible = true
                    }
                }
        )


        // Show Configuration Popup
        if (isConfigPopupVisible) {
            ConfigurationPopup(
                onDismiss = { isConfigPopupVisible = false },
                onSave = { newApiUrl ->
                    apiUrl = newApiUrl
                },
                apiUrl
            )
        }
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JetpackTheme {
        Main()
    }
}


@Composable
fun ConfigurationPopup(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    oldUrl: String,
) {
    var apiUrl by remember { mutableStateOf(oldUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("Enter API URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
            )

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

                TextButton(onClick = {
                    onSave(apiUrl)
                    onDismiss()
                }) {
                    Text("Save")
                }
            }
        }
    }
}
