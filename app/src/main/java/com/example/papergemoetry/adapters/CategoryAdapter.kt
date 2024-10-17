package com.example.papergemoetry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.papergemoetry.R
import com.example.papergemoetry.network.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryImage: ImageView = itemView.findViewById(R.id.category_image)
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        val categoryButton: LinearLayout = itemView.findViewById(R.id.category_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.categoryName.text = category.categoria

        // Set image based on category name
        val imageResId = when (category.categoria.lowercase()) {
            "consolas" -> R.drawable.consolas
            "halloween" -> R.drawable.halloween
            "mario" -> R.drawable.mario
            "minecraft" -> R.drawable.minecraft
            "pokemon" -> R.drawable.pokemon
            "star wars" -> R.drawable.star_ward
            else -> R.drawable.placeholder
        }

        holder.categoryImage.setImageResource(imageResId)
        holder.categoryButton.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount() = categories.size
}