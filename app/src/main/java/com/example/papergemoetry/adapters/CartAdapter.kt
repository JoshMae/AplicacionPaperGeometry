package com.example.papergemoetry.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.papergemoetry.R
import com.example.papergemoetry.network.CartItem
import com.example.papergemoetry.network.CartResponse
import com.example.papergemoetry.network.RickAndMortyApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val token: String,
    private val updateTotal: () -> Unit,
    private val removeItem: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        holder.nameTextView.text = item.producto.nombreP
        holder.priceTextView.text = "Precio: $${item.producto.precio}"
        holder.subtotalTextView.text = "Subtotal: $${item.subtotal}"
        holder.quantityTextView.text = item.cantidad.toString()

        Glide.with(holder.itemView.context)
            .load(item.producto.foto)
            .into(holder.imageView)

        holder.increaseButton.setOnClickListener {
            updateItemQuantity(item.idCarrito, item.cantidad + 1, position)
        }

        holder.decreaseButton.setOnClickListener {
            if (item.cantidad > 1) {
                updateItemQuantity(item.idCarrito, item.cantidad - 1, position)
            } else {
                removeItem(position) // Elimina el producto si la cantidad es 0
            }
        }

        holder.removeImageView.setOnClickListener {
            removeItem(position)
        }
    }

    private fun updateItemQuantity(idCarrito: Int, nuevaCantidad: Int, position: Int) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://papergeometry.online")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(RickAndMortyApi::class.java)
        val updateCall = apiService.updateQuantity(idCarrito, nuevaCantidad)

        updateCall.enqueue(object : Callback<CartItem> {
            override fun onResponse(call: Call<CartItem>, response: Response<CartItem>) {
                if (response.isSuccessful) {
                    // Después de actualizar la cantidad, vuelve a cargar el carrito
                    loadCartItems()
                } else {
                    Log.e("CartAdapter", "Error al actualizar la cantidad: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<CartItem>, t: Throwable) {
                Log.e("CartAdapter", "Fallo en la actualización de cantidad: ${t.message}")
            }
        })
    }

    private fun loadCartItems() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://papergeometry.online")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(RickAndMortyApi::class.java)
        val call = service.getCart(token)

        call.enqueue(object : Callback<CartResponse> {
            override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val updatedCartItems = response.body()!!.carrito

                    // Actualiza el carrito con la información más reciente
                    cartItems.clear()
                    cartItems.addAll(updatedCartItems)
                    notifyDataSetChanged()
                    updateTotal() // Actualiza el total en el carrito
                } else {
                    Log.e("CartAdapter", "Error al cargar el carrito: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                Log.e("CartAdapter", "Fallo al cargar el carrito: ${t.message}")
            }
        })
    }

    override fun getItemCount(): Int = cartItems.size

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_product)
        val nameTextView: TextView = view.findViewById(R.id.text_product_name)
        val priceTextView: TextView = view.findViewById(R.id.text_product_price)
        val subtotalTextView: TextView = view.findViewById(R.id.text_product_subtotal)
        val quantityTextView: TextView = view.findViewById(R.id.text_product_quantity)
        val increaseButton: Button = view.findViewById(R.id.button_increase_quantity)
        val decreaseButton: Button = view.findViewById(R.id.button_decrease_quantity)
        val removeImageView: ImageView = view.findViewById(R.id.image_remove_product)
    }
}
