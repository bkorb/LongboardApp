package com.example.longboardapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DataContent(text: String,
                modifier: Modifier,
                textColor: Color = Color.Black,
                style: TextStyle = MaterialTheme.typography.bodyMedium,
                textModifier: Modifier = Modifier,
                shape: Shape = RectangleShape,
                borderStroke: BorderStroke = BorderStroke(1.dp, Color.Black),
                innerPaddingValues: PaddingValues = PaddingValues(0.dp),
                outerPaddingValues: PaddingValues = PaddingValues(0.dp),
                textAlign: TextAlign? = null,
){
    Box(modifier = modifier.padding(outerPaddingValues)) {
        Text(
            text = text,
            modifier = textModifier
                .fillMaxWidth()
                .clip(shape)
                .border(borderStroke, shape)
                .padding(innerPaddingValues),
            style = style,
            color = textColor,
            textAlign = textAlign
        )
    }
}