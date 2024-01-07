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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun DataFillContent(text: String,
                    color: Color,
                    percent: Float,
                    modifier: Modifier,
                    textModifier: Modifier = Modifier,
                    textColor: Color = Color.Black,
                    style: TextStyle = MaterialTheme.typography.bodyMedium,
                    innerPaddingValues: PaddingValues = PaddingValues(0.dp),
                    outerPaddingValues: PaddingValues = PaddingValues(0.dp),
                    shape: Shape = RectangleShape,
                    borderStroke: BorderStroke = BorderStroke(1.dp, Color.Black),
                    textAlign: TextAlign? = null,
){
    Box(modifier = modifier.padding(outerPaddingValues)) {
        Text(
            text = text,
            color = textColor,
            modifier = textModifier
                .fillMaxWidth()
                .clip(shape)
                .drawBehind {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    drawRect(
                        color = color,
                        size = Size(
                            max(canvasWidth * percent, canvasWidth * 0.1f),
                            canvasHeight
                        )
                    )
                }
                .border(borderStroke, shape)
                .padding(innerPaddingValues),
            style = style,
            textAlign = textAlign
        )
    }
}