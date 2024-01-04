package com.example.longboardapp

import Data
import Message
import Target
import Values
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import encodeMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import parseMessage
import okhttp3.WebSocketListener
import kotlin.math.PI

class WebSocketListener(
    private val viewModel: SocketViewModel
): WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        viewModel.updateStatus(true)
        //webSocket.send("Android Device Connected")
        println("onOpen:")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        val msg = (parseMessage(text))!!
        if ((msg.id ?: "") == "COMM_GET_VALUES") {
            val can = (msg.fields as Values).app_controller_id ?: 0
            if (can == 57) {
                viewModel.updateValues1(msg.fields)
            } else if (can == 124) {
                viewModel.updateValues2(msg.fields)
            }
        }
        println("onMessage: $text")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        println("onClosing: $code $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        viewModel.updateStatus(false)
        println("onClosed: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("onFailure: ${t.message} $response")
        super.onFailure(webSocket, t, response)
    }
}

fun ERPMToRPM(erpm: Float): Float {
    return erpm*16f/36f/7f
}

fun RPMToMPH(rpm: Float): Float {
    return rpm*(PI.toFloat()*0.09f*60f/1000f*0.621371f)
}

fun MPHToRPM(mph: Float): Float {
    return mph/(PI.toFloat()*0.09f*60f/1000f*0.621371f)
}

fun RPMTOERPM(rpm: Float): Float {
    return rpm*36f*7f/16f
}

class SocketViewModel : ViewModel() {
    var values1 by mutableStateOf(Values())
        private set

    var values2 by mutableStateOf(Values())
        private set

    var status by mutableStateOf(false)
        private set

    var batteryPercent by mutableStateOf(0f)
        private set

    fun updateBatteryPercent(percent: Float){
        batteryPercent = percent
    }

    val smoothing_factor = 0.01f

    fun updateValues1(values: Values){
        values1 = values
        val pv = ((values.v_in ?: 0f)-44.4f)/6f
        batteryPercent = clamp(batteryPercent*(1-smoothing_factor)+pv*smoothing_factor, 0f, 1f)
    }

    fun updateValues2(values: Values){
        values2 = values
        val pv = ((values.v_in ?: 0f)-44.4f)/6f
        batteryPercent = clamp(batteryPercent*(1-smoothing_factor)+pv*smoothing_factor, 0f, 1f)
    }

    fun updateStatus(_status: Boolean){
        status = _status
    }

    val okHttpClient = OkHttpClient()
    var webSocket: WebSocket? = null

    /*suspend fun DefaultClientWebSocketSession.writeMessages() {
        while (true) {
            val message2 = encodeMessage(Message(id="COMM_GET_VALUES", fields=Data())) ?: "Hello World!"
            val message = encodeMessage(Message(id="setTarget", fields=Target(5000))) ?: "Hello World!"
            //val message2 = encodeMessage(Message(id="getTarget", fields=Data())) ?: "Hello World!"
            try {
                send(message)
                send(message2)
            } catch (e: Exception) {
                println("Error while sending: " + e.localizedMessage)
                return
            }
            Thread.sleep(50)
        }
    }*/

    /*suspend fun DefaultClientWebSocketSession.readMessages() {
        try {
            var count = 0
            while(true){
                val message = async { incoming.receive() }.await()
                launch {
                    message as Frame.Text
                    val msg = (parseMessage(message.readText()))!!
                    if ((msg.id ?: "") == "COMM_GET_VALUES") {

                        val can = (msg.fields as Values).app_controller_id ?: 0
                        if (can == 57) {
                            updateValues1(msg.fields)
                        } else if (can == 124) {
                            updateValues2(msg.fields)
                        }
                    }
                    println(count)
                    count += 1
                }
            }
        } catch (e: Exception) {
            println("Error while receiving: " + e.localizedMessage)
        }
    }*/

    /*fun runSocket() {
        val client = HttpClient {
            install(WebSockets)
        }
        viewModelScope.launch(Dispatchers.IO) {
            client.webSocket("ws://192.168.0.27:8765") {
                val messageOutputRoutine = launch { readMessages() }
                val userInputRoutine = launch { writeMessages() }
                userInputRoutine.join() // Wait for completion; either "exit" or error
                messageOutputRoutine.cancelAndJoin()
            }
            client.close()
            println("Connection closed. Goodbye!")
        }
    }*/
}