package com.filemanager.vip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.filemanager.vip.R
import com.filemanager.vip.model.FileCategory
import com.filemanager.vip.model.FileItem
import com.filemanager.vip.ui.adapters.CategoryAdapter
import com.filemanager.vip.ui.adapters.FileAdapter
import com.filemanager.vip.util.FileUtils
import com.filemanager.vip.util.Preferences
import java.io.File

class HomeFragment : Fragment() {

    private lateinit var recyclerCategories: RecyclerView
    private lateinit var recyclerRecent: RecyclerView
    private lateinit var storageProgress: android.widget.ProgressBar
    private lateinit var txtUsed: TextView
    private lateinit var txtFree: TextView
    private lateinit var txtPercent: TextView
    private lateinit var txtGreeting: TextView
    private lateinit var vipBanner: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        bindViews(view)
        setupCategories()
        return view
    }

    override fun onResume() {
        super.onResume()
        updateStorageInfo()
        loadRecentFiles()
        updateVipBanner()
    }

    private fun bindViews(view: View) {
        recyclerCategories = view.findViewById(R.id.recycler_categories)
        recyclerRecent = view.findViewById(R.id.recycler_recent)
        storageProgress = view.findViewById(R.id.storage_progress)
        txtUsed = view.findViewById(R.id.txt_used)
        txtFree = view.findViewById(R.id.txt_free)
        txtPercent = view.findViewById(R.id.txt_storage_percent)
        txtGreeting = view.findViewById(R.id.txt_greeting)
        vipBanner = view.findViewById(R.id.vip_banner)

        vipBanner.setOnClickListener {
            // Open settings tab via activity bottom nav
            (activity as? com.filemanager.vip.MainActivity)?.let { act ->
                act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                    R.id.bottom_nav
                )?.selectedItemId = R.id.nav_settings
            }
        }
    }

    private fun setupCategories() {
        val categories = listOf(
            FileCategory.FOLDER to R.drawable.ic_folder,
            FileCategory.IMAGE to R.drawable.ic_image,
            FileCategory.VIDEO to R.drawable.ic_video,
            FileCategory.AUDIO to R.drawable.ic_audio,
            FileCategory.DOC to R.drawable.ic_doc,
            FileCategory.APK to R.drawable.ic_apk,
            FileCategory.DOWNLOAD to R.drawable.ic_download,
            FileCategory.FILE to R.drawable.ic_file
        )
        recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 4)
        recyclerCategories.adapter = CategoryAdapter(categories, requireContext()) { cat ->
            openCategory(cat)
        }
        updateCategoryCounts()
    }

    private fun updateCategoryCounts() {
        val adapter = recyclerCategories.adapter as? CategoryAdapter ?: return
        val root = FileUtils.getPrimaryRoot()
        val counts = mutableMapOf<FileCategory, Int>()
        FileUtils.listFiles(root).forEach { f ->
            val cat = FileItem(f).category
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        adapter.updateCounts(counts)
    }

    private fun openCategory(category: FileCategory) {
        val filesFragment = FilesFragment.newInstance(category = category)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.container, filesFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateStorageInfo() {
        val root = FileUtils.getPrimaryRoot()
        val (total, used, free) = FileUtils.getStorageInfo(root)
        if (total > 0) {
            val percent = (used * 100 / total).toInt()
            storageProgress.max = 100
            storageProgress.progress = percent
            txtPercent.text = "$percent%"
            txtUsed.text = getString(R.string.used_format, FileUtils.formatSize(used))
            txtFree.text = getString(R.string.free_format, FileUtils.formatSize(free))
        }
    }

    private fun loadRecentFiles() {
        val root = FileUtils.getPrimaryRoot()
        val files = FileUtils.listFiles(root).filter { it.isFile }.take(8)
        val items = files.map { FileItem(it) }
        recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecent.adapter = FileAdapter(
            items = items,
            context = requireContext(),
            isSelectionMode = false,
            onItemClick = { item -> openFile(item) }
        )
    }

    private fun openFile(item: FileItem) {
        FilesFragment.openFileWithIntent(requireContext(), item.file)
    }

    private fun updateVipBanner() {
        val isVip = Preferences.isVip()
        vipBanner.visibility = if (isVip) View.GONE else View.VISIBLE
        txtGreeting.text = getString(if (isVip) R.string.welcome_vip else R.string.app_name)
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
