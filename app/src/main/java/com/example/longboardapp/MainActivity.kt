package com.example.longboardapp

import Message
import Target
import Values
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.longboardapp.ui.theme.LongboardAppTheme
import encodeMessage
import kotlinx.coroutines.*
import okhttp3.Request
import okhttp3.WebSocketListener
import java.math.RoundingMode
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
            LongboardAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SocketContent(socketViewModel = viewModel, webSocketListener = webSocketListener)
                }
            }
        }
    }

}

@Composable
fun SocketContent(socketViewModel: SocketViewModel = viewModel(), webSocketListener: WebSocketListener) {
    val rpm_formatter: DecimalFormat = DecimalFormat.getInstance() as DecimalFormat
    rpm_formatter.minimumFractionDigits = 0
    rpm_formatter.maximumFractionDigits = 0
    rpm_formatter.minimumIntegerDigits = 1
    rpm_formatter.maximumIntegerDigits = 5
    val battery_formatter: DecimalFormat = DecimalFormat.getInstance() as DecimalFormat
    battery_formatter.minimumFractionDigits = 0
    battery_formatter.maximumFractionDigits = 0
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.padding(0.dp)) {
            val dataModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.LightGray)
            Text(
                text = "M1: ${rpm_formatter.format(10*round(0.1*ERPMToRPM(socketViewModel.values1.rpm ?: 0f)))} RPM",
                modifier = dataModifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "M2: ${rpm_formatter.format(10*round(0.1*ERPMToRPM(socketViewModel.values2.rpm ?: 0f)))} RPM",
                modifier = dataModifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Battery: ${battery_formatter.format(socketViewModel.batteryPercent*100)}%",
                modifier = dataModifier
                    .drawBehind {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val battery_percent = socketViewModel.batteryPercent
                        drawRect(
                            color = Color(
                                red = 1 - battery_percent,
                                green = battery_percent,
                                blue = 0f
                            ),
                            size = Size(
                                max(canvasWidth * battery_percent, canvasWidth * 0.1f),
                                canvasHeight
                            )
                        )
                    }
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(modifier = Modifier.padding(0.dp)) {
            val dataModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.LightGray)
            Text(
                text = "M1: ${rpm_formatter.format(RPMToMPH(ERPMToRPM(socketViewModel.values1.rpm ?: 0f)))} MPH",
                modifier = dataModifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "M1: ${rpm_formatter.format(RPMToMPH(ERPMToRPM(socketViewModel.values2.rpm ?: 0f)))} MPH",
                modifier = dataModifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(modifier = Modifier
            .padding(4.dp)
            .height(200.dp)) {
            val buttonModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp)
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
                modifier = buttonModifier
            ) {
                Text(text = if (socketViewModel.status) "Disconnect" else "Connect")
            }
            Button(
                onClick = {
                    val message = encodeMessage(
                        Message(
                            id = "setTarget",
                            fields = Target(RPMTOERPM(MPHToRPM(25f)).toInt())
                        )
                    ) ?: "Hello World!"
                    socketViewModel.webSocket?.send(message)
                },
                enabled = socketViewModel.status,
                modifier = buttonModifier
            ) {
                Text(text = "Start")
            }
            Button(
                onClick = {
                    val message = encodeMessage(Message(id = "setTarget", fields = Target(0)))
                        ?: "Hello World!"
                    socketViewModel.webSocket?.send(message)
                },
                enabled = socketViewModel.status,
                modifier = buttonModifier
            ) {
                Text(text = "Stop")
            }
        }
    }
}