package com.filemanager.vip.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.filemanager.vip.R
import com.filemanager.vip.model.FileCategory
import com.filemanager.vip.model.FileItem
import com.filemanager.vip.util.FileUtils
import com.filemanager.vip.util.Preferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class FileAdapter(
    private var items: List<FileItem>,
    private val context: Context,
    private var isSelectionMode: Boolean = false,
    private val onItemClick: (FileItem) -> Unit,
    private val onStarClick: ((FileItem) -> Unit)? = null,
    private val onMoreClick: ((FileItem) -> Unit)? = null
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val selected = mutableSetOf<String>()

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val icon: ShapeableImageView = view.findViewById(R.id.iv_icon)
        val name: TextView = view.findViewById(R.id.txt_name)
        val info: TextView = view.findViewById(R.id.txt_info)
        val star: MaterialButton = view.findViewById(R.id.iv_star)
        val more: MaterialButton = view.findViewById(R.id.iv_more)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val item = items[pos]
                if (isSelectionMode) toggleSelection(item) else onItemClick(item)
            }
            view.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(items[pos])
                }
                true
            }
            star.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                onStarClick?.invoke(items[pos])
            }
            more.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                onMoreClick?.invoke(items[pos])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.info.text = buildInfo(item)
        holder.icon.setImageResource(iconResFor(item))
        holder.checkbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkbox.isChecked = selected.contains(item.file.absolutePath)
        val isFav = Preferences.isFavorite(item.file.absolutePath)
        holder.star.setIconResource(if (isFav) R.drawable.ic_star else R.drawable.ic_star_outline)
    }

    override fun getItemCount(): Int = items.size

    private fun buildInfo(item: FileItem): String {
        return if (item.isDirectory) {
            val count = item.file.listFiles()?.size ?: 0
            context.getString(R.string.item_count, count)
        } else {
            FileUtils.formatSize(item.size)
        }
    }

    private fun iconResFor(item: FileItem): Int = when (item.category) {
        FileCategory.FOLDER -> R.drawable.ic_folder
        FileCategory.IMAGE -> R.drawable.ic_image
        FileCategory.VIDEO -> R.drawable.ic_video
        FileCategory.AUDIO -> R.drawable.ic_audio
        FileCategory.DOC -> R.drawable.ic_doc
        FileCategory.APK -> R.drawable.ic_apk
        FileCategory.DOWNLOAD -> R.drawable.ic_download
        FileCategory.FILE -> R.drawable.ic_file
    }

    fun updateItems(newItems: List<FileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectionMode(mode: Boolean) {
        isSelectionMode = mode
        if (!mode) selected.clear()
        notifyDataSetChanged()
    }

    fun isSelectionMode(): Boolean = isSelectionMode

    fun getSelectedItems(): List<FileItem> =
        items.filter { selected.contains(it.file.absolutePath) }

    fun getSelectedCount(): Int = selected.size

    fun selectAll() {
        items.forEach { selected.add(it.file.absolutePath) }
        notifyDataSetChanged()
    }

    private fun toggleSelection(item: FileItem) {
        val path = item.file.absolutePath
        if (selected.contains(path)) selected.remove(path) else selected.add(path)
        notifyDataSetChanged()
    }
}
