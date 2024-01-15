package com.manhtai.whatthefoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import java.util.Calendar

data class Config(
    var defaultImage: String = "https://cdn2.thecatapi.com/images/7_rjG2-pc.jpg",
    var apiURL: String = "https://api.thecatapi.com/v1/images/search?limit=10",
    var imageDelaySeconds: Int = 30,
    var imageFadeSeconds: Int = 3,
    var sleepFromHour: Int = 19,
    var sleepToHour: Int = 8,
)

@Composable
fun ConfigurationPopup(
    ctx: Context,
    onDismiss: () -> Unit,
    onSave: (c: Config) -> Unit,
    oldConf: Config,
) {
    var conf by remember { mutableStateOf(oldConf) }

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
                        ContextCompat.startActivity(ctx, intent, Bundle())
                    }) {
                        Text("WiFi Settings")
                    }
                }

                TextField(
                    value = conf.apiURL,
                    onValueChange = { conf = conf.copy(apiURL = it) },
                    label = { Text("API (return [{url: image}])") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    TextField(value = conf.imageDelaySeconds.toString(),
                        label = { Text(text = "Image delay (seconds)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            conf =
                                conf.copy(imageDelaySeconds = if (it != "" && it.toInt() > 0) it.toInt() else 1)
                        })

                    TextField(value = conf.imageFadeSeconds.toString(),
                        label = { Text(text = "Image fade (seconds)") },
                        modifier = Modifier
                            .padding(start = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            conf = conf.copy(imageFadeSeconds = if (it != "") it.toInt() else 0)
                        })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    TextField(value = conf.sleepFromHour.toString(),
                        label = { Text(text = "Sleep from hour (0h-24h)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            conf = conf.copy(
                                sleepFromHour =
                                if (it != "" && it.toInt() in 0..24) it.toInt() else 0
                            )
                        })

                    TextField(value = conf.sleepToHour.toString(),
                        label = {
                            Text(
                                text = "To hour (0h-24h). Now is " + Calendar.getInstance()
                                    .get(Calendar.HOUR_OF_DAY) + "h."
                            )
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            conf = conf.copy(
                                sleepToHour =
                                if (it != "" && it.toInt() in 0..24) it.toInt() else 0
                            )
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
                        onSave(conf)
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
