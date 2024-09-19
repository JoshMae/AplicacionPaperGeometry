
package com.example.papergemoetry.network

import com.example.papergemoetry.models.Character

import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RickAndMortyApi {
    //api Catalogo
    @GET("/api/producto")
    fun getCharacters(): Call<List<Character>>

    //api para generar token
    @POST("api/carrito/token")
    fun getCartToken(): Call<CartTokenResponse>

    //api para agregar producto a carrito
    @POST("api/carrito/agregar")
    fun addToCart(
        @Header("Cart-Token") cartToken: String,
        @Query("idProducto") idProducto: Int,
        @Query("cantidad") cantidad: Int
    ): Call<Unit>

    //cargar carrito
    @GET("api/carrito")
    fun getCart(@Header("Cart-Token") token: String): Call<CartResponse>

    //actualizar cantidad de carrito
    @FormUrlEncoded
    @PUT("api/carrito/{idCarrito}")
    fun updateQuantity(
        @Path("idCarrito") idCarrito: Int,
        @Field("cantidad") cantidad: Int
    ): Call<CartItem>

    @DELETE("api/carrito/eliminar/{idCarrito}")
    fun removeFromCart(
        @Header("Cart-Token") cartToken: String,
        @Path("idCarrito") idCarrito: Int
    ): Call<Void>
}

data class CartTokenResponse(
    val cart_token: String
)

data class CharacterResponse(
    val results: List<Character>
)

data class CartResponse(
    val success: Boolean,
    val carrito: List<CartItem>,
    val total: Double
)

data class CartItem(
    val idCarrito: Int,
    var cantidad: Int,
    val producto: Producto,
    var subtotal: Double
)

data class Producto(
    val idProducto: Int,
    val nombreP: String,
    val precio: Double,
    val foto: String
)
