package com.example.longboardapp

import Message
import Settings
import Speed
import SpeedFormat
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.longboardapp.ui.theme.LongboardAppTheme
import encodeMessage
import okhttp3.Request
import okhttp3.WebSocketListener
import java.lang.NumberFormatException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

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
                                        id = MessageID.SET_TARGET,
                                        fields = Target(Speed(0))
                                    )
                                )
                                viewModel.webSocket?.send(message)
                            }) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                val message = encodeMessage(
                                    Message(
                                        id = MessageID.SET_TARGET,
                                        fields = Target(
                                            Speed(
                                                offsetY * 30f / size.height,
                                                SpeedFormat.MPH
                                            )
                                        )
                                    )
                                )
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
fun SettingBlock(value: String,
                 onValueChange: (String) -> Unit,
                 label: String,
                 modifier: Modifier = Modifier
                     .fillMaxWidth()
                     .height(67.dp)
                     .padding(4.dp)
                     .wrapContentSize(Alignment.Center),
                 keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)){
    TextField(
        value = value,
        label = { Text(label) },
        onValueChange = onValueChange,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
fun SettingsDialog(onDismissRequest: () -> Unit, onConfirm: (Settings) -> Unit, settings: Settings) {
    var ACC_RPM_PER_SECOND by remember { mutableStateOf(settings.ACC_RPM_PER_SECOND.toString()) }
    var DEC_RPM_PER_SECOND by remember { mutableStateOf(settings.DEC_RPM_PER_SECOND.toString()) }
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
                SettingBlock(
                    value = ACC_RPM_PER_SECOND,
                    onValueChange = {
                        ACC_RPM_PER_SECOND = it
                    },
                    label = "Max Acceleration"
                )
                SettingBlock(
                    value = DEC_RPM_PER_SECOND,
                    onValueChange = {
                        DEC_RPM_PER_SECOND = it
                    },
                    label = "Max Deceleration"
                )
                TextButton(
                    onClick = {
                        try {
                            onConfirm(
                                Settings(
                                    ACC_RPM_PER_SECOND.toFloat(),
                                    DEC_RPM_PER_SECOND.toFloat()
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

@Composable
fun SocketContent(socketViewModel: SocketViewModel = viewModel(), webSocketListener: WebSocketListener) {
    var settings_open by remember { mutableStateOf(false) }

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
            .height(134.dp)) {
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
            Button(
                onClick = { settings_open = true },
                modifier = buttonModifier,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Settings")
            }
        }
        Column(modifier = Modifier
            .padding(0.dp, 2.dp)
            .clip(shape = RoundedCornerShape(16.dp))
            .background(Color.LightGray)
        ) {
            // BATTERY
            Row(modifier = Modifier.padding(0.dp)) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                val percent = clamp(((socketViewModel.values1.charge?.percent ?: 0f) + (socketViewModel.values2.charge?.percent ?: 0f))/2f, 0f, 1f)
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
            // SPEED
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "${rpm_formatter.format((abs(socketViewModel.values1.speed?.mph ?: 0f) + abs(socketViewModel.values2.speed?.mph ?: 0f)) / 2)} MPH",
                    modifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // DISTANCE
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "${tach_formatter.format( ((socketViewModel.values1.distance_abs?.miles ?: 0f) + (socketViewModel.values2.distance_abs?.miles ?: 0f)) / 2)} Miles",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // LABELS
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
            // RPM
            Row(modifier = Modifier.padding(0.dp)) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "${rpm_formatter.format(abs(10 * round(0.1f * (socketViewModel.values1.speed?.rpm ?: 0f))))} RPM",
                    modifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "${rpm_formatter.format(abs(10 * round(0.1 * (socketViewModel.values2.speed?.rpm ?: 0f))))} RPM",
                    modifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // TEMPERATURE
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "${battery_formatter.format(socketViewModel.values1.temp_fet ?: 0f)} °C",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "${battery_formatter.format(socketViewModel.values2.temp_fet ?: 0f)} °C",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // POWER
            Row(
                modifier = Modifier.padding(0.dp)
            ) {
                val dataModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                DataContent(
                    text = "${battery_formatter.format((socketViewModel.values1.charge?.volts ?: 0f) * abs(socketViewModel.values1.avg_motor_current ?: 0f))} Watts",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 16.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "${battery_formatter.format((socketViewModel.values2.charge?.volts ?: 0f) * abs(socketViewModel.values2.avg_motor_current ?: 0f))} Watts",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
        }
    }
    if(settings_open){
        SettingsDialog(
            onDismissRequest =
                {
                    settings_open = false
                },
            onConfirm =
            {
                socketViewModel.updateSettings(it)
                if(socketViewModel.status) {
                    socketViewModel.webSocket?.send(
                        encodeMessage(
                            Message(
                                MessageID.SET_SETTINGS,
                                socketViewModel.settings
                            )
                        )
                    )
                }
            },
            settings = socketViewModel.settings
        )
    }
}