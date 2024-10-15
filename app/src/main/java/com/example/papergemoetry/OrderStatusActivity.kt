package com.example.papergemoetry

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class OrderStatusActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var cartToken: String
    private var orderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_status)

        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)

        cartToken = intent.getStringExtra("cart_token") ?: ""

        if (cartToken.isNotEmpty()) {
            getOrderStatus(cartToken)
            setupPusher()
        } else {
            Log.e("OrderStatusActivity", "Token no proporcionado")
            finish()
        }
    }

    private fun getOrderStatus(token: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://papergeometry.site/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(OrderStatusService::class.java)
        val request = OrderStatusRequest(token)

        service.getOrderStatus(request).enqueue(object : Callback<OrderStatusResponse> {
            override fun onResponse(call: Call<OrderStatusResponse>, response: Response<OrderStatusResponse>) {
                if (response.isSuccessful) {
                    val orderStatus = response.body()
                    orderStatus?.let {
                        orderId = it.idPedido
                        updateUI("Recibido")
                    }
                } else {
                    Log.e("OrderStatusActivity", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OrderStatusResponse>, t: Throwable) {
                Log.e("OrderStatusActivity", "Error", t)
            }
        })
    }

    private fun setupPusher() {
        val options = PusherOptions()
        options.setCluster("us2")

        val pusher = Pusher("73f643c127e3ce2a5820", options)

        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.d("Pusher", "State changed from ${change.previousState} to ${change.currentState}")
            }

            override fun onError(message: String, code: String?, e: Exception?) {
                Log.e("Pusher", "There was a problem connecting! code ($code), message ($message), exception($e)")
            }
        }, ConnectionState.ALL)

        val channel: Channel = pusher.subscribe("pedidos")

        channel.bind("pedido.estado.actualizado") { event: PusherEvent ->
            val jsonData = JSONObject(event.data)
            val eventOrderId = jsonData.getInt("idPedido")
            if (eventOrderId == orderId) {
                val newStatus = jsonData.getString("nuevo_estado")
                runOnUiThread {
                    updateUI(newStatus)
                }
            }
        }
    }

    private fun updateUI(status: String) {
        val (statusText, progress, color) = when (status) {
            "Recibido" -> Triple("Recibido", 20, R.color.red) // Estado inicial en rojo
            "Imprimiendo" -> Triple("Imprimiendo", 40, R.color.orange)
            "Recortando" -> Triple("Recortando", 60, R.color.yellow)
            "Armando" -> Triple("Armando", 80, R.color.light_green)
            "Finalizado" -> Triple("Finalizado", 100, R.color.green) // Estado final en verde
            else -> Triple("Desconocido", 0, R.color.gray) // Color por defecto
        }

        statusTextView.text = statusText
        progressBar.progress = progress

        // Cambiar el color de la barra de progreso
        progressBar.progressTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, color)
        )
    }
}

interface OrderStatusService {
    @POST("api/pedido/token")
    fun getOrderStatus(@Body request: OrderStatusRequest): Call<OrderStatusResponse>
}

data class OrderStatusRequest(val token: String)

data class OrderStatusResponse(
    val idPedido: Int,
    val cart_token: String,
    val idCliente: Int,
    val idestado_pedido: Int,
    val total: String,
    val created_at: String,
    val updated_at: String
)