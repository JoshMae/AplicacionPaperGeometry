package com.example.papergemoetry

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.JosherDev22.papergemoetry.R
import com.example.papergemoetry.adapters.CategoryAdapter
import com.example.papergemoetry.adapters.CharacterAdapter
import com.example.papergemoetry.network.CharacterResponse
import com.example.papergemoetry.network.RickAndMortyApi
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.papergemoetry.models.Character
import com.example.papergemoetry.network.CartTokenResponse
import com.example.papergemoetry.network.Category
import com.example.papergemoetry.network.OrderCheckService
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "MainActivity"

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private var cartToken: String? = null

    private var regreso= 1;
    // Variables para contenido Catalogo
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://papergeometry.site")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(RickAndMortyApi::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        fetchCartToken()

        val searchInput: EditText = findViewById(R.id.search_input)
        val searchIcon: ImageView = findViewById(R.id.search_icon)

        searchIcon.setOnClickListener {
            filterCharacters(searchInput.text.toString())
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCharacters(s.toString())
            }
        })
        val buttonBack: ImageButton = findViewById(R.id.button_back)
        buttonBack.setOnClickListener {
            if(regreso==1){
                showCategories()
                showProductList()
            }
            else if(regreso==2){
                fetchCharacters()
                showProductList()
            }

        }

        findViewById<ImageButton>(R.id.button_cart).setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        showCategories()

    }

    private fun fetchCartToken() {
        val existingToken = getToken()
        if (!existingToken.isNullOrEmpty()) {
            Log.d(TAG, "Using existing token: $existingToken")
            cartToken = existingToken
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://papergeometry.site")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(RickAndMortyApi::class.java)
        val call = service.getCartToken()

        call.enqueue(object : retrofit2.Callback<CartTokenResponse> {
            override fun onResponse(call: Call<CartTokenResponse>, response: retrofit2.Response<CartTokenResponse>) {
                if (response.isSuccessful) {
                    cartToken = response.body()?.cart_token
                    saveToken(cartToken)
                    Log.e(TAG,"Token inicial: $cartToken")
                } else {
                    Toast.makeText(this@MainActivity, "Error al obtener el token", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CartTokenResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveToken(token: String?) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("cart_token", token).apply()
    }

    private fun getToken(): String? {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("cart_token", null)
    }

    private fun filterCharacters(query: String) {
        val filteredList = characters.filter {
            it.nombre.contains(query, ignoreCase = true)
        }
        setupRecyclerView(filteredList)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_one -> {
                regreso=1
                findViewById<ImageView>(R.id.header_image).visibility = View.VISIBLE
                findViewById<View>(R.id.search_container).visibility = View.GONE
                showCategories()
                showProductList()
            }
            R.id.item_two -> {
                regreso=2
                findViewById<ImageView>(R.id.header_image).visibility = View.GONE
                findViewById<View>(R.id.search_container).visibility = View.VISIBLE
                fetchCharacters()
                showProductList()
            }
            R.id.item_three -> {
                checkActiveOrder()
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showCategories() {
        findViewById<ImageView>(R.id.header_image).visibility = View.VISIBLE
        findViewById<View>(R.id.search_container).visibility = View.GONE

        service.getCategories().enqueue(object : retrofit2.Callback<List<Category>> {
            override fun onResponse(call: Call<List<Category>>, response: retrofit2.Response<List<Category>>) {
                if (response.isSuccessful) {
                    val categories = response.body() ?: emptyList()
                    setupCategoryGrid(categories)
                }
            }

            override fun onFailure(call: Call<List<Category>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun setupCategoryGrid(categories: List<Category>) {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = CategoryAdapter(categories) { category ->
            fetchCharactersBySpecies(category.categoria)
            findViewById<ImageButton>(R.id.button_back).visibility = View.VISIBLE
        }
    }

    private fun fetchCharactersBySpecies(categoria: String) {
        service.getCharacters().enqueue(object : retrofit2.Callback<List<Character>> {
            override fun onResponse(call: Call<List<Character>>, response: retrofit2.Response<List<Character>>) {
                if (response.isSuccessful) {
                    val allCharacters = response.body() ?: emptyList()
                    val filteredCharacters = allCharacters.filter { it.categoria == categoria }
                    setupRecyclerView(filteredCharacters)
                }
            }

            override fun onFailure(call: Call<List<Character>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private lateinit var characters: List<Character>

    private fun fetchCharacters() {
        service.getCharacters().enqueue(object : retrofit2.Callback<List<Character>> {
            override fun onResponse(call: Call<List<Character>>, response: retrofit2.Response<List<Character>>) {
                if (response.isSuccessful) {
                    characters = response.body() ?: emptyList()
                    setupRecyclerView(characters)
                }
            }

            override fun onFailure(call: Call<List<Character>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView(characters: List<Character>) {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val gridLayoutManager = GridLayoutManager(this, 2) // Número de columnas en la cuadrícula
           recyclerView.layoutManager = gridLayoutManager

        val token = getToken() ?: ""

        recyclerView.adapter = CharacterAdapter(characters, { character ->
            showProductDetails(character)
        }, token)
    }

    private fun showProductDetails(character: Character) {
        findViewById<RecyclerView>(R.id.recycler_view).visibility = View.GONE
        findViewById<View>(R.id.product_detail_container).visibility = View.VISIBLE
        findViewById<ImageButton>(R.id.button_back).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.header_image).visibility = View.GONE
        findViewById<View>(R.id.search_container).visibility = View.GONE

        // Poblar los detalles del producto
        val productImage: ImageView = findViewById(R.id.product_image)
        val productName: TextView = findViewById(R.id.product_name)
        val productPrice: TextView = findViewById(R.id.product_price)
        val productCategory: TextView = findViewById(R.id.product_category)
        val productDetails: TextView = findViewById(R.id.product_details)
        val addToCartButton: Button = findViewById(R.id.add_to_cart_button)

        Picasso.get().load(character.foto).into(productImage)
        productName.text = character.nombre
        productPrice.text = "Precio: ${character.precio}"
        productCategory.text = "Categoría: ${character.categoria}"
        productDetails.text = "Detalles: ${character.detalle}"

        addToCartButton.setOnClickListener {
            addToCart(character.idProducto)
        }
    }

    private fun showProductList() {
        findViewById<RecyclerView>(R.id.recycler_view).visibility = View.VISIBLE
        findViewById<View>(R.id.product_detail_container).visibility = View.GONE
        findViewById<ImageButton>(R.id.button_back).visibility = View.GONE
    }

    private fun addToCart(idProducto: Int) {
        val token = cartToken ?: ""
        val call = service.addToCart(token, idProducto, 1)
        call.enqueue(object : retrofit2.Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: retrofit2.Response<Unit>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No se proporcionaron detalles del error"
                    Toast.makeText(this@MainActivity, errorBody, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkActiveOrder() {
        val token = getToken() ?: ""
        Log.e(TAG,"Token de Pedido: $token")
        if (token == null) {
            Toast.makeText(this, "Error: No se encontró un token válido", Toast.LENGTH_SHORT).show()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://papergeometry.site/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(OrderCheckService::class.java)
        val request = OrderStatusRequest(token)

        service.checkOrderStatus(request).enqueue(object : retrofit2.Callback<OrderStatusResponse> {
            override fun onResponse(
                call: Call<OrderStatusResponse>,
                response: retrofit2.Response<OrderStatusResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    // Si hay un pedido activo, abre OrderStatusActivity
                    val intent = Intent(this@MainActivity, OrderStatusActivity::class.java)
                    intent.putExtra("cart_token", token)
                    startActivity(intent)
                } else {
                    // Si no hay pedido activo
                    Toast.makeText(
                        this@MainActivity,
                        "No tienes pedidos pendientes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<OrderStatusResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al verificar el estado del pedido: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onBackPressed() {
        when {
            findViewById<View>(R.id.product_detail_container).visibility == View.VISIBLE -> showProductList()
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
            else -> super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
