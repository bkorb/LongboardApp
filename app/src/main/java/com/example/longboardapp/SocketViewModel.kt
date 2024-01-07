package com.example.longboardapp

import GetSettings
import GetValues
import SetSettings
import Settings
import Values
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import encodeMessage
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import parseMessage
import okhttp3.WebSocketListener
import java.net.InetAddress

class WebSocketListener(
    private val viewModel: SocketViewModel
): WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        viewModel.updateStatus(true)
        webSocket.send(encodeMessage(GetSettings()))
        println("onOpen:")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        try {
            when(val msg = (parseMessage(text))){
                is GetValues -> {
                    msg.fields?.let {
                        val can = it.app_controller_id
                        if (can == 57) {
                            viewModel.updateValues1(it)
                        } else if (can == 124) {
                            viewModel.updateValues2(it)
                        }
                    }
                }
                is GetSettings -> {
                    msg.fields?.let {
                        viewModel.updateSettings(it)
                    }
                }
                is SetSettings -> {
                    msg.fields.let {
                        viewModel.updateSettings(it)
                    }
                }
                else -> {
                    throw IllegalArgumentException("Message Type Not Used: ${msg.javaClass.name}")
                }
            }
        }catch (e: IllegalArgumentException){
            println(e)
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

class SocketViewModel : ViewModel() {
    var values1 by mutableStateOf(Values())
        private set

    var connectable by mutableStateOf(false)
        private set

    var values2 by mutableStateOf(Values())
        private set

    var settings by mutableStateOf(Settings())
        private set

    var status by mutableStateOf(false)
        private set

    fun updateValues1(values: Values){
        values1 = values
    }

    fun updateValues2(values: Values){
        values2 = values
    }

    fun updateConnectable(_connectable: Boolean){
        connectable = _connectable
    }

    fun updateSettings(_settings: Settings){
        settings = _settings
    }

    fun updateStatus(_status: Boolean){
        status = _status
        if(!status){
            values1 = Values()
            values2 = Values()
            settings = Settings()
        }
    }


    val okHttpClient = OkHttpClient()
    var webSocket: WebSocket? = null
}