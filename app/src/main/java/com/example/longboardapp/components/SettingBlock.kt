package com.example.longboardapp.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingBlock(value: String,
                 onValueChange: (String) -> Unit,
                 label: String,
                 modifier: Modifier = Modifier
                     .fillMaxWidth()
                     .height(67.dp)
                     .padding(4.dp)
                     .wrapContentSize(Alignment.Center),
                 keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
){
    TextField(
        value = value,
        label = { Text(label) },
        onValueChange = onValueChange,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
    )
}