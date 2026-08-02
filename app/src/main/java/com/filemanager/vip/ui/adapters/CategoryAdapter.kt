package com.filemanager.vip.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.filemanager.vip.R
import com.filemanager.vip.model.FileCategory

class CategoryAdapter(
    private var items: List<Pair<FileCategory, Int>>,
    private val context: Context,
    private val onClick: (FileCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val counts = mutableMapOf<FileCategory, Int>()

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_cat_icon)
        val name: TextView = view.findViewById(R.id.txt_cat_name)
        val count: TextView = view.findViewById(R.id.txt_cat_count)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(items[pos].first)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val (category, iconRes) = items[position]
        holder.icon.setImageResource(iconRes)
        holder.name.text = context.getString(category.labelRes)
        holder.count.text = context.getString(R.string.item_count, counts[category] ?: 0)
    }

    override fun getItemCount(): Int = items.size

    fun updateCounts(newCounts: Map<FileCategory, Int>) {
        counts.clear()
        counts.putAll(newCounts)
        notifyDataSetChanged()
    }
}
