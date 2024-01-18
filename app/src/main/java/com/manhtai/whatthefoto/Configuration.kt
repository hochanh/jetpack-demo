package com.manhtai.whatthefoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
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
import android.graphics.Color as Colour
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.Calendar

@Entity
data class Config(
    val defaultImage: String = "https://cdn2.thecatapi.com/images/7_rjG2-pc.jpg",

    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "api_url") val apiURL: String = "https://api.thecatapi.com/v1/images/search?limit=10",
    @ColumnInfo(name = "image_delay_seconds") val imageDelaySeconds: Int = 30,
    @ColumnInfo(name = "image_fade_seconds") val imageFadeSeconds: Int = 3,
    @ColumnInfo(name = "sleep_from_hour") val sleepFromHour: Int = 19,
    @ColumnInfo(name = "sleep_to_hour") val sleepToHour: Int = 8,
    @ColumnInfo(name = "background_color") val backgroundColor: String = "#000000",
)

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config WHERE id = 1")
    fun get(): Config?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conf: Config)
}

@Database(entities = [Config::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "local-database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}


@Composable
fun ConfigurationPopup(
    ctx: Context,
    onDismiss: () -> Unit,
    onSave: (c: Config) -> Unit,
    oldConf: Config,
) {
    var conf by remember { mutableStateOf(oldConf) }
    var apiURL by remember { mutableStateOf(oldConf.apiURL) }
    var delaySeconds by remember { mutableStateOf(oldConf.imageDelaySeconds.toString()) }
    var fadeSeconds by remember { mutableStateOf(oldConf.imageFadeSeconds.toString()) }
    var sleepFrom by remember { mutableStateOf(oldConf.sleepFromHour.toString()) }
    var sleepTo by remember { mutableStateOf(oldConf.sleepToHour.toString()) }
    var bgColor by remember { mutableStateOf(oldConf.backgroundColor) }
    var msg by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
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
                    value = apiURL,
                    onValueChange = { apiURL = it },
                    label = { Text("Image API URL (return [{ url }])") },
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
                    TextField(value = delaySeconds,
                        label = { Text(text = "Image delay (seconds)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            delaySeconds = it
                        })

                    TextField(value = fadeSeconds,
                        label = { Text(text = "Image fade (seconds)") },
                        modifier = Modifier
                            .padding(start = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            fadeSeconds = it
                        })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    TextField(value = sleepFrom,
                        label = { Text(text = "Sleep from hour (0h-24h)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            sleepFrom = it
                        })

                    TextField(value = sleepTo,
                        label = {
                            Text(
                                text = "To hour (0h-24h). Now is " + Calendar.getInstance()
                                    .get(Calendar.HOUR_OF_DAY) + "h."
                            )
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            sleepTo = it
                        })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    TextField(value = bgColor,
                        label = { Text(text = "Background color") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            bgColor = it
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
                        if (
                            apiURL != "" && isValidURL(apiURL) &&
                            delaySeconds != "" &&
                            fadeSeconds != "" &&
                            sleepFrom != "" && sleepFrom.toInt() < 24 &&
                            sleepTo != "" && sleepTo.toInt() < 24 &&
                            bgColor != "" && isValidColor(bgColor)
                        ) {
                            conf = conf.copy(
                                apiURL = apiURL,
                                imageDelaySeconds = delaySeconds.toInt(),
                                imageFadeSeconds = fadeSeconds.toInt(),
                                sleepFromHour = sleepFrom.toInt(),
                                sleepToHour = sleepTo.toInt(),
                                backgroundColor = bgColor
                            )
                            onSave(conf)
                            onDismiss()
                        } else {
                            msg = "Invalid configuration!"
                        }
                    }) {
                        Text("Save")
                    }
                }

                if (msg != "") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(text = msg, color = Color.Red)
                    }
                }
            }
        }
    }
}

fun isValidColor(c: String): Boolean {
    return try {
        Colour.parseColor(c)
        true
    } catch (_: Exception) {
        false
    }
}

fun isValidURL(u: String): Boolean {
    return Patterns.WEB_URL.matcher(u).matches()
}