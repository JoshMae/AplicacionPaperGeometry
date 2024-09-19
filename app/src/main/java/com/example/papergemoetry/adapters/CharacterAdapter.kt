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
import com.example.papergemoetry.R
import com.example.papergemoetry.models.Character
import com.example.papergemoetry.network.RickAndMortyApi
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CharacterAdapter(
    private val characters: List<Character>,
    private val onCharacterClick: (Character) -> Unit,
    private val token: String
) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_character, parent, false)
        return CharacterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = characters[position]
        holder.nameTextView.text = character.nombre
        Picasso.get().load(character.foto).into(holder.imageView)

        holder.itemView.setOnClickListener {
            onCharacterClick(character)
        }

        holder.addToCartButton.setOnClickListener {
            addToCart(character.idProducto, holder.itemView.context)
        }
    }

    override fun getItemCount(): Int = characters.size

    private fun addToCart(idProducto: Int, context: Context) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://papergeometry.online")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(RickAndMortyApi::class.java)

        // Usa el token recibido en el constructor
        val call = service.addToCart(token, idProducto, 1)
        call.enqueue(object : retrofit2.Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: retrofit2.Response<Unit>) {
                if (response.isSuccessful) {
                    // Mostrar un mensaje de éxito
                    Toast.makeText(context, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                } else {
                    // Mostrar un mensaje de error
                    val errorBody = response.errorBody()?.string() ?: "No se proporcionaron detalles del error"
                    Log.e("AddToCartError", "Error al agregar al carrito: $errorBody")
                    Toast.makeText(context, "$errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                // Mostrar un mensaje de error
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()

            }
        })
    }



    class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_character)
        val nameTextView: TextView = itemView.findViewById(R.id.text_name)
        val addToCartButton: Button = itemView.findViewById(R.id.button_add_to_cart)
    }
}

