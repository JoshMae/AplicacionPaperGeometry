package com.example.papergemoetry

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.papergemoetry.adapters.CharacterAdapter
import com.example.papergemoetry.models.Character
import com.example.papergemoetry.network.CharacterResponse
import com.example.papergemoetry.network.RickAndMortyApi
import com.google.android.material.navigation.NavigationView
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CharacterDetailActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private var cartToken: String? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://papergeometry.online/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(RickAndMortyApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_detail)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_character_detail)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout_details)
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()



        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Character Details"

        val navigationView: NavigationView = findViewById(R.id.nav_view_details)
        navigationView.setNavigationItemSelectedListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Agrega el botón "Atrás"
        supportActionBar?.title = "Character Details"

        val character = intent.getParcelableExtra<Character>("character")

        val imageView: ImageView = findViewById(R.id.image_character_detail)
        val nameView: TextView = findViewById(R.id.text_name_detail)
        val statusView: TextView = findViewById(R.id.text_status_detail)
        val speciesView: TextView = findViewById(R.id.text_species_detail)
        val genderView: TextView = findViewById(R.id.text_gender_detail)
        val originView: TextView = findViewById(R.id.text_origin_detail)

        character?.let {
            nameView.text = it.nombre
            statusView.text = "Precio: ${it.precio}"
            speciesView.text = "Detalles: ${it.detalle}"
            genderView.text = "Categoría: ${it.categoria}"

            Picasso.get().load(it.foto).into(imageView)
        }


    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.item_one -> {
                findViewById<LinearLayout>(R.id.search_container).visibility = View.GONE
            }
            R.id.item_two -> {
                findViewById<LinearLayout>(R.id.search_container).visibility = View.VISIBLE
                fetchCharacters()
            }
            R.id.item_three -> {
                findViewById<LinearLayout>(R.id.search_container).visibility = View.GONE
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
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
                Toast.makeText(this@CharacterDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun setupRecyclerView(characters: List<Character>) {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val gridLayoutManager = GridLayoutManager(this, 2) // Número de columnas en la cuadrícula
        recyclerView.layoutManager = gridLayoutManager

        val token = ""

        recyclerView.adapter = CharacterAdapter(characters, { character ->
            // Aquí se maneja el clic en un personaje
            val intent = Intent(this, CharacterDetailActivity::class.java)
            intent.putExtra("character", character)
            startActivity(intent)
        }, token)
    }

    override fun onSupportNavigateUp(): Boolean {
        drawer.openDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

}



