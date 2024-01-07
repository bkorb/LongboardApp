package com.example.longboardapp

import SetSettings
import SetTarget
import Speed
import SpeedFormat
import Values
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.longboardapp.components.DataContent
import com.example.longboardapp.components.DataFillContent
import com.example.longboardapp.components.SettingsDialog
import com.example.longboardapp.ui.theme.LongboardAppTheme
import encodeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.WebSocketListener
import java.net.InetAddress
import kotlin.math.abs
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
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            while(true){
                if(!viewModel.status){
                    val conn = InetAddress.getByName("longboard.local").isReachable(5000)
                    Log.println(Log.DEBUG, "PINGER", conn.toString())
                    viewModel.updateConnectable(conn)
                }
                Thread.sleep(2000)
            }
        }
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
                                    SetTarget(0)
                                )
                                viewModel.webSocket?.send(message)
                            }) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                val message = encodeMessage(
                                    SetTarget(
                                        Speed(
                                            offsetY * 30f / size.height,
                                            SpeedFormat.MPH
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
                enabled = socketViewModel.status || socketViewModel.connectable,
                modifier = buttonModifier,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = if (socketViewModel.status) "Disconnect" else "Connect")
            }
            Button(
                onClick = { settings_open = true },
                enabled = socketViewModel.status,
                modifier = buttonModifier,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Settings")
            }
        }
        Column(modifier = Modifier
            .fillMaxHeight()
            .padding(0.dp, 2.dp)
            .clip(shape = RoundedCornerShape(16.dp))
            .background(Color.LightGray)
        ) {
            // BATTERY
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                val percent = clamp((socketViewModel.values1.charge.percent + socketViewModel.values2.charge.percent)/2f, 0f, 1f)
                DataFillContent(
                    text = "Battery: ${battery_formatter.format(percent * 100)}%",
                    color = Color(
                        red = 1 - percent,
                        green = percent,
                        blue = 0f
                    ),
                    percent = percent,
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // SPEED
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                DataContent(
                    text = "${rpm_formatter.format((abs(socketViewModel.values1.speed.mph) + abs(socketViewModel.values2.speed.mph)) / 2)} MPH",
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // DISTANCE
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                DataContent(
                    text = "${tach_formatter.format( (socketViewModel.values1.distance_abs.miles + socketViewModel.values2.distance_abs.miles) / 2)} Miles",
                    modifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    textModifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // LABELS
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                DataContent(
                    text = "Motor 1",
                    modifier = dataModifier.background(Color.Gray),
                    innerPaddingValues = PaddingValues(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textModifier = dataModifier,
                    textAlign = TextAlign.Center
                )
                DataContent(
                    text = "Motor 2",
                    modifier = dataModifier.background(Color.Gray),
                    innerPaddingValues = PaddingValues(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textModifier = dataModifier,
                    textAlign = TextAlign.Center
                )
            }
            // RPM
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                DataContent(
                    text = "${rpm_formatter.format(abs(10 * round(0.1f * socketViewModel.values1.speed.rpm)))} RPM",
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "${rpm_formatter.format(abs(10 * round(0.1 * socketViewModel.values2.speed.rpm)))} RPM",
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // TEMPERATURE
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                DataContent(
                    text = "${battery_formatter.format(socketViewModel.values1.temp_fet)} °C",
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "${battery_formatter.format(socketViewModel.values2.temp_fet)} °C",
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
            }
            // POWER
            Row(
                modifier = Modifier.padding(0.dp).weight(1f)
            ) {
                val dataModifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                DataContent(
                    text = "${battery_formatter.format(socketViewModel.values1.charge.volts * abs(socketViewModel.values1.avg_motor_current))} Watts",
                    modifier = dataModifier,
                    textModifier = dataModifier,
                    shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 16.dp),
                    innerPaddingValues = PaddingValues(16.dp),
                )
                DataContent(
                    text = "${battery_formatter.format(socketViewModel.values2.charge.volts * abs(socketViewModel.values2.avg_motor_current))} Watts",
                    modifier = dataModifier,
                    textModifier = dataModifier,
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
                //socketViewModel.updateSettings(it)
                if(socketViewModel.status) {
                    socketViewModel.webSocket?.send(
                        encodeMessage(
                            SetSettings(
                                it
                            )
                        )
                    )
                }
            },
            settings = socketViewModel.settings
        )
    }
}