package com.example.papergemoetry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.JosherDev22.papergemoetry.R
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class ConfirmPurchaseActivity : AppCompatActivity() {
    private lateinit var cartDetails: List<CartItemDetail>
    private lateinit var cartToken: String

    private val CAMERA_PERMISSION_CODE = 100
    private val TAG = "ConfirmPurchaseActivity"
    private lateinit var cardNumberEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonConfirmPurchase: Button
    private var totalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_confirm_purchase)

            val totalString = intent.getStringExtra("total") ?: "Total: Q0.00"
            totalAmount = extractNumericValue(totalString)
            cartToken = intent.getStringExtra("cart_token") ?: ""
            findViewById<TextView>(R.id.text_total).text = "Total: Q${String.format("%.2f", totalAmount)}"


            cardNumberEditText = findViewById(R.id.edit_text_card_number)
            progressBar = findViewById(R.id.progress_bar)
            buttonConfirmPurchase = findViewById(R.id.button_confirm_purchase)

            val buttonScanQR = findViewById<Button>(R.id.button_scan_qr)
            buttonScanQR.setOnClickListener {
                initializeQRScanner()
            }

            buttonConfirmPurchase.setOnClickListener {
                if (validateFields()) {
                    confirmPurchase()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate de ConfirmPurchaseActivity", e)
            Toast.makeText(this, "Error al cargar la página de confirmación", Toast.LENGTH_LONG).show()
            finish()
        }
        @Suppress("UNCHECKED_CAST")
        cartDetails = intent.getSerializableExtra("cart_details") as? ArrayList<CartItemDetail> ?: emptyList()
        cartToken = intent.getStringExtra("cart_token") ?: ""

        Log.d(TAG, "Token recibido en ConfirmPurchaseActivity: $cartToken")

    }

    private fun extractNumericValue(totalString: String): Double {
        val regex = """[\d.]+""".toRegex()
        val matchResult = regex.find(totalString)
        return matchResult?.value?.toDoubleOrNull() ?: 0.0
    }

    private fun initializeQRScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startQRScanner()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.captureActivity = CaptureActivity::class.java
        integrator.setOrientationLocked(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanea el código QR")
        integrator.initiateScan()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startQRScanner()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                processQRContent(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun processQRContent(content: String) {
        Log.d(TAG, "QR Content: $content")
        val regex = "Cuenta: (\\d+), Usuario: (\\w+)".toRegex()
        val matchResult = regex.find(content)

        if (matchResult != null) {
            val (accountNumber, username) = matchResult.destructured
            cardNumberEditText.setText(accountNumber)
            Toast.makeText(this, "Cuenta: $accountNumber, Usuario: $username", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Formato de QR no válido", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateFields(): Boolean {
        val nombre = findViewById<TextInputEditText>(R.id.edit_text_nombre).text.toString()
        val apellidos = findViewById<TextInputEditText>(R.id.edit_text_apellidos).text.toString()
        val telefono = findViewById<TextInputEditText>(R.id.edit_text_telefono).text.toString()
        val correo = findViewById<TextInputEditText>(R.id.edit_text_correo).text.toString()
        val cardNumber = findViewById<TextInputEditText>(R.id.edit_text_card_number).text.toString()

        if (nombre.isEmpty() || apellidos.isEmpty() || telefono.isEmpty() || correo.isEmpty() || cardNumber.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }

        // Aquí podrías agregar más validaciones si lo deseas

        return true
    }

    private fun confirmPurchase() {
        showLoading(true)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)  // Tiempo de espera de conexión
            .writeTimeout(90, TimeUnit.SECONDS)    // Tiempo de espera para escribir datos
            .readTimeout(90, TimeUnit.SECONDS)     // Tiempo de espera para leer la respuesta
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://desarrollowebumg.somee.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        val service = retrofit.create(ApiService::class.java)
        val request = PurchaseRequest(
            cuenta = cardNumberEditText.text.toString(),
            terminal = 1,
            valor = totalAmount,
            empresa = "Pruebas"
        )

        service.confirmPurchase(request).enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                showLoading(false)
                if (response.isSuccessful && response.body() == 1) {
                    saveCompra()
                } else {
                    showLoading(false)
                    Toast.makeText(this@ConfirmPurchaseActivity, "Compra rechazada", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@ConfirmPurchaseActivity, "Error en la comunicación", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error en la solicitud de API", t)
            }
        })
    }

    private fun saveCompra() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://papergeometry.site/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(PaperGeometryApiService::class.java)
        val request = createCompraRequest()

        service.registrarCompra(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        Toast.makeText(this@ConfirmPurchaseActivity, "Compra Exitosa", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@ConfirmPurchaseActivity, OrderStatusActivity::class.java).apply {
                            putExtra("cart_token", cartToken)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        Log.d("ConfirmPurchaseActivity", "Iniciando OrderStatusActivity con cartToken: $cartToken")
                        startActivity(intent)

                        finish()
                    } else {
                        val errorMessage = apiResponse?.message ?: "Mensaje de error no disponible"
                        Toast.makeText(this@ConfirmPurchaseActivity, "Error al realizar la compra: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error al guardar la compra: $errorMessage")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ConfirmPurchaseActivity, "Error en la respuesta del servidor", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error en la respuesta del servidor. Código: ${response.code()}, Cuerpo: $errorBody")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@ConfirmPurchaseActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error al guardar", t)
            }
        })
    }

    private fun createCompraRequest(): CompraRequest {
        return CompraRequest(
            correo = findViewById<TextInputEditText>(R.id.edit_text_correo).text.toString(),
            nombres = findViewById<TextInputEditText>(R.id.edit_text_nombre).text.toString(),
            apellidos = findViewById<TextInputEditText>(R.id.edit_text_apellidos).text.toString(),
            telefono = findViewById<TextInputEditText>(R.id.edit_text_telefono).text.toString(),
            token = cartToken,
            total = totalAmount,
            detalles = getCartDetails()
        )
    }

    private fun getCartDetails(): List<PedidoDetalle> {
        return cartDetails.map { item ->
            PedidoDetalle(
                idProducto = item.idProducto,
                cantidad = item.cantidad,
                subTotal = item.subTotal
            )
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        buttonConfirmPurchase.isEnabled = !show
    }
}

interface ApiService {
    @POST("api/Principal/CobroPos")
    fun confirmPurchase(@Body request: PurchaseRequest): Call<Int>
}

data class PurchaseRequest(
    val cuenta: String,
    val terminal: Int,
    val valor: Double,
    val empresa: String
)

interface PaperGeometryApiService {
    @POST("api/registrar-compra")
    fun registrarCompra(@Body request: CompraRequest): Call<ApiResponse>
}

data class CompraRequest(
    val correo: String,
    val nombres: String,
    val apellidos: String,
    val telefono: String,
    val token: String,
    val total: Double,
    val detalles: List<PedidoDetalle>
)

data class PedidoDetalle(
    val idProducto: Int,
    val cantidad: Int,
    val subTotal: Double
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)