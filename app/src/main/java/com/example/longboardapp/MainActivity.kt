package com.example.longboardapp

import Message
import Target
import Values
import android.icu.text.DecimalFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.longboardapp.ui.theme.LongboardAppTheme
import encodeMessage
import okhttp3.Request
import okhttp3.WebSocketListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

fun createRequest(): Request {
    val websocketURL = "ws://192.168.0.27:8765"
    return Request.Builder()
        .url(websocketURL)
        .build()
}

class MainActivity : ComponentActivity() {
    private lateinit var webSocketListener: WebSocketListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: SocketViewModel by viewModels()
        webSocketListener = WebSocketListener(viewModel)
        setContent {
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }
            LongboardAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(onDragEnd = {
                                offsetX = 0f
                                offsetY = 0f
                                val message = encodeMessage(
                                    Message(
                                        id = "setTarget",
                                        fields = Target(RPMToERPM(MPHToRPM(0f)).toInt())
                                    )
                                ) ?: "Hello World!"
                                viewModel.webSocket?.send(message)
                            }) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                val message = encodeMessage(
                                    Message(
                                        id = "setTarget",
                                        fields = Target(RPMToERPM(MPHToRPM(offsetY*30f/size.height)).toInt())
                                    )
                                ) ?: "Hello World!"
                                viewModel.webSocket?.send(message)
                            }
                    },
                    color = MaterialTheme.colorScheme.background,

                ) {
                    SocketContent(socketViewModel = viewModel, webSocketListener = webSocketListener)
                }
            }
        }
    }

}

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

@Composable
fun SocketContent(socketViewModel: SocketViewModel = viewModel(), webSocketListener: WebSocketListener) {
    val rpm_formatter: DecimalFormat = DecimalFormat.getInstance() as DecimalFormat
    rpm_formatter.minimumFractionDigits = 0
    rpm_formatter.maximumFractionDigits = 0
    rpm_formatter.minimumIntegerDigits = 1
    rpm_formatter.maximumIntegerDigits = 5
    val tach_formatter: DecimalFormat = DecimalFormat.getInstance() as DecimalFormat
    tach_formatter.minimumSignificantDigits = 3
    tach_formatter.maximumSignificantDigits = 3
    val battery_formatter: DecimalFormat = DecimalFormat.getInstance() as DecimalFormat
    battery_formatter.minimumFractionDigits = 0
    battery_formatter.maximumFractionDigits = 0
    Column(modifier = Modifier.padding(16.dp, 4.dp)) {
        Column(modifier = Modifier
            .padding(0.dp, 2.dp)
            .height(67.dp)) {
            val buttonModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(0.dp, 4.dp)
            Button(
                onClick = {
                    if (socketViewModel.status) {
                        socketViewModel.webSocket?.close(1000, "Closed manually")
                        socketViewModel.updateValues1(Values())
                        socketViewModel.updateValues2(Values())
                        socketViewModel.updateBatteryPercent(0f)
                    } else {
                        socketViewModel.webSocket = socketViewModel.okHttpClient.newWebSocket(
                            createRequest(),
                            webSocketListener
                        )
                    }
                },
                modifier = buttonModifier,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = if (socketViewModel.status) "Disconnect" else "Connect")
            }
        }
        Column(modifier = Modifier
            .padding(0.dp, 2.dp)
            .clip(shape = RoundedCornerShape(16.dp))
            .background(Color.LightGray)
        ) {
            Row(modifier = Modifier.padding(0.dp)) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                val percent = socketViewModel.batteryPercent
                DataFillContent(
                    text = "Battery: ${battery_formatter.format(percent * 100)}%",
                    color = Color(
                        red = 1 - percent,
                        green = percent,
                        blue = 0f
                    ),
                    percent = percent,
                    modifier = dataModifier,
                    shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            Row(modifier = Modifier.padding(0.dp)) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "Motor 1",
                    modifier = dataModifier.background(Color.Gray),
                    innerPaddingValues = PaddingValues(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                DataContent(
                    text = "Motor 2",
                    modifier = dataModifier.background(Color.Gray),
                    innerPaddingValues = PaddingValues(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            Row(modifier = Modifier.padding(0.dp)) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "M1: ${rpm_formatter.format(abs(10 * round(0.1 * ERPMToRPM(socketViewModel.values1.rpm ?: 0f))))} RPM",
                    modifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "M2: ${rpm_formatter.format(abs(10 * round(0.1 * ERPMToRPM(socketViewModel.values2.rpm ?: 0f))))} RPM",
                    modifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "M1: ${battery_formatter.format(socketViewModel.values1.temp_fet ?: 0f)} °C",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "M2: ${battery_formatter.format(socketViewModel.values2.temp_fet ?: 0f)} °C",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "M1: ${tach_formatter.format(ERevsToMiles( socketViewModel.values1.tachometer_abs ?: 0f))} Miles",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "M2: ${tach_formatter.format(ERevsToMiles( socketViewModel.values2.tachometer_abs ?: 0f))} Miles",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "M1: ${rpm_formatter.format(abs(RPMToMPH(ERPMToRPM(socketViewModel.values1.rpm ?: 0f))))} MPH",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 16.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "M2: ${rpm_formatter.format(abs(RPMToMPH(ERPMToRPM(socketViewModel.values2.rpm ?: 0f))))} MPH",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
        }
    }
}