package com.filemanager.vip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.filemanager.vip.R
import com.filemanager.vip.model.FileItem
import com.filemanager.vip.ui.adapters.FileAdapter
import com.filemanager.vip.util.Preferences
import java.io.File

class FavoritesFragment : Fragment() {

    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var adapter: FileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        recyclerFavorites = view.findViewById(R.id.recycler_favorites)
        txtEmpty = view.findViewById(R.id.txt_empty_fav)
        recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val paths = Preferences.getFavorites()
        val items = paths.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) FileItem(f) else null
        }
        txtEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        adapter = FileAdapter(
            items = items,
            context = requireContext(),
            isSelectionMode = false,
            onItemClick = { item ->
                FilesFragment.openFileWithIntent(requireContext(), item.file)
            },
            onStarClick = { item ->
                Preferences.removeFavorite(item.file.absolutePath)
                loadFavorites()
            },
            onMoreClick = { item ->
                FilesFragment.openFileWithIntent(requireContext(), item.file)
            }
        )
        recyclerFavorites.adapter = adapter
    }

    companion object {
        fun newInstance() = FavoritesFragment()
    }
}
