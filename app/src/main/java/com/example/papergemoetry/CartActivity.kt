package com.example.papergemoetry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.papergemoetry.adapters.CartAdapter
import com.example.papergemoetry.network.CartItem
import com.example.papergemoetry.network.CartResponse
import com.example.papergemoetry.network.RickAndMortyApi
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.Serializable

class CartActivity : AppCompatActivity() {

    private val TAG = "CartActivity"

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalTextView: TextView
    private lateinit var confirmButton: Button

    private lateinit var cartAdapter: CartAdapter
    private var cartItems: MutableList<CartItem> = mutableListOf()  // Aquí guardarás los items del carrito

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView = findViewById(R.id.recycler_cart_items)
        totalTextView = findViewById(R.id.text_total)
        confirmButton = findViewById(R.id.button_confirm_purchase)

        val token = getToken() ?: ""
        cartAdapter = CartAdapter(cartItems, token, ::updateTotal, ::removeItem)
        recyclerView.adapter = cartAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Cargar los productos del carrito desde la API
        loadCartItems()

        // Confirmar compra
        confirmButton.setOnClickListener {
            val intent = Intent(this, ConfirmPurchaseActivity::class.java)
            intent.putExtra("total", totalTextView.text.toString())
            startActivity(intent)
        }
    }

    private fun loadCartItems() {
        val token = getToken() ?: ""
        if (token.isEmpty()) {
            Toast.makeText(this, "No se encontró el token", Toast.LENGTH_SHORT).show()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://papergeometry.site")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(RickAndMortyApi::class.java)
        val call = service.getCart(token)

        call.enqueue(object : Callback<CartResponse> {
            override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Imprimir la respuesta completa para depurar
                    Log.d(TAG, "Respuesta de la API: ${response.body()}")

                    val items = response.body()?.carrito ?: emptyList()

                    if (items.isEmpty()) {
                        Toast.makeText(this@CartActivity, "El carrito está vacío, agregue productos.", Toast.LENGTH_SHORT).show()
                    } else {
                        cartItems.clear()
                        cartItems.addAll(items)
                        Log.d(TAG, "Items agregados al carrito: $cartItems")
                        cartAdapter.notifyDataSetChanged()
                        updateTotal()
                    }

                } else {
                    Log.e(TAG, "Error en la respuesta: ${response.errorBody()?.string()}")
                    Toast.makeText(this@CartActivity, "Error al cargar el carrito", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                Log.e(TAG, "Fallo al cargar el carrito: ${t.message}")
                Toast.makeText(this@CartActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun updateTotal() {
        val total = cartItems.sumOf { it.subtotal }
        totalTextView.text = "Total: $${"%.2f".format(total)}"
    }

    private fun removeItem(position: Int) {
        val itemToRemove = cartItems[position]
        val idCarrito = itemToRemove.idCarrito
        val token = getToken() ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "No se encontró el token", Toast.LENGTH_SHORT).show()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://papergeometry.site")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(RickAndMortyApi::class.java)
        val call = service.removeFromCart(token, idCarrito)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Eliminar el producto del carrito localmente
                    cartItems.removeAt(position)
                    cartAdapter.notifyItemRemoved(position)
                    updateTotal()

                    Toast.makeText(this@CartActivity, "Producto eliminado del carrito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CartActivity, "Error al eliminar el producto", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getToken(): String? {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("cart_token", null)
    }

    private fun confirmPurchase() {
        val intent = Intent(this, ConfirmPurchaseActivity::class.java)
        val token = getToken() ?: ""
        intent.putExtra("total", totalTextView.text.toString())
        intent.putExtra("cart_token", token)

        // Convertir los detalles del carrito a un formato serializable
        val cartDetails = cartItems.map { item ->
            CartItemDetail(
                idProducto = item.producto.idProducto,
                cantidad = item.cantidad,
                subTotal = item.subtotal
            )
        }
        intent.putExtra("cart_details", ArrayList(cartDetails))
        Log.d(TAG, "Token enviado a ConfirmPurchaseActivity: $token")
        startActivity(intent)
    }
}

data class CartItemDetail(
    val idProducto: Int,
    val cantidad: Int,
    val subTotal: Double
) : Serializable



