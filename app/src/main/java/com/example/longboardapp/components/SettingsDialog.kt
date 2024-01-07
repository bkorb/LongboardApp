package com.example.longboardapp.components

import Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.lang.NumberFormatException

@Composable
fun SettingsDialog(onDismissRequest: () -> Unit, onConfirm: (Settings) -> Unit, settings: Settings) {
    val map by remember { mutableStateOf(settings.mapValues { (key, value) -> value.toString() }.toMutableMap()) }
    Dialog(
        onDismissRequest = { onDismissRequest() },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                map.forEach { (key, value) ->
                    SettingBlock(
                        value = value,
                        onValueChange = {
                            map[key] = it
                        },
                        label = key
                    )
                }
                TextButton(
                    onClick = {
                        try {
                            onConfirm(
                                Settings(
                                    map.mapValues { (key, value) ->
                                        try{
                                            value.toFloat()
                                        }catch (e: NumberFormatException){
                                            settings[key] ?: 0f
                                        }
                                    }.toMutableMap()
                                )
                            )
                        }catch(e: NumberFormatException){
                            println("invalid entry")
                        }
                        onDismissRequest()
                    }
                ) {
                    Text(text = "Confirm")
                }
            }
        }
    }
}