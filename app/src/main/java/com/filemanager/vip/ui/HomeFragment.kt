package com.filemanager.vip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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

    private lateinit var rootView: View
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var recyclerRecent: RecyclerView
    private lateinit var storageProgress: ProgressBar
    private lateinit var txtUsed: TextView
    private lateinit var txtFree: TextView
    private lateinit var txtPercent: TextView
    private lateinit var txtGreeting: TextView
    private lateinit var vipBanner: View

    private var categoryAdapter: CategoryAdapter? = null
    private var recentAdapter: FileAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            val view = inflater.inflate(R.layout.fragment_home, container, false)
            rootView = view
            bindViews(view)
            if (::recyclerCategories.isInitialized && ::recyclerRecent.isInitialized) {
                setupCategories()
            }
            view
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Inflation failed: ${e.message}")
            inflater.inflate(R.layout.fragment_home, container, false)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::recyclerCategories.isInitialized && ::recyclerRecent.isInitialized &&
                ::storageProgress.isInitialized && ::txtUsed.isInitialized &&
                ::txtFree.isInitialized && ::txtPercent.isInitialized
            ) {
                updateStorageInfo()
                loadRecentFiles()
                updateVipBanner()
                updateCategoryCounts()
            }
        } catch (e: Exception) {
            // Don't crash on any home screen error
            android.util.Log.e("HomeFragment", "onResume error: ${e.message}")
        }
    }

    private fun bindViews(view: View) {
        try {
            recyclerCategories = view.findViewById(R.id.recycler_categories)
            recyclerRecent = view.findViewById(R.id.recycler_recent)
            storageProgress = view.findViewById(R.id.storage_progress)
            txtUsed = view.findViewById(R.id.txt_used)
            txtFree = view.findViewById(R.id.txt_free)
            txtPercent = view.findViewById(R.id.txt_storage_percent)
            txtGreeting = view.findViewById(R.id.txt_greeting)
            vipBanner = view.findViewById(R.id.vip_banner)

            vipBanner.setOnClickListener {
                try {
                    val act = activity
                    if (act is com.filemanager.vip.MainActivity) {
                        act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                            R.id.bottom_nav
                        ).selectedItemId = R.id.nav_settings
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "bindViews error: ${e.message}")
        }
    }

    private fun setupCategories() {
        try {
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
            val layoutManager = GridLayoutManager(requireContext(), 4)
            recyclerCategories.layoutManager = layoutManager

            categoryAdapter = CategoryAdapter(categories, requireContext()) { cat ->
                try {
                    openCategory(cat)
                } catch (e: Exception) { /* ignore */ }
            }
            recyclerCategories.adapter = categoryAdapter
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "setupCategories error: ${e.message}")
        }
    }

    private fun updateCategoryCounts() {
        try {
            val adapter = categoryAdapter ?: return
            val root = FileUtils.getPrimaryRoot()
            val counts = mutableMapOf<FileCategory, Int>()

            try {
                FileUtils.listFiles(root).forEach { f ->
                    val cat = FileItem(f).category
                    counts[cat] = (counts[cat] ?: 0) + 1
                }
            } catch (e: SecurityException) {
                // No permission - skip counts
            } catch (e: Exception) {
                // Any other error - skip counts
            }

            adapter.updateCounts(counts)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "updateCategoryCounts error: ${e.message}")
        }
    }

    private fun openCategory(category: FileCategory) {
        try {
            val fm = activity?.supportFragmentManager ?: return
            val filesFragment = FilesFragment.newInstance(category = category)
            fm.beginTransaction()
                .replace(R.id.container, filesFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun updateStorageInfo() {
        try {
            val root = FileUtils.getPrimaryRoot()
            val (total, used, free) = FileUtils.getStorageInfo(root)
            if (total > 0) {
                val percent = (used * 100 / total).toInt().coerceIn(0, 100)
                storageProgress.max = 100
                storageProgress.progress = percent
                txtPercent.text = "$percent%"
                txtUsed.text = getString(R.string.used_format, FileUtils.formatSize(used))
                txtFree.text = getString(R.string.free_format, FileUtils.formatSize(free))
            }
        } catch (e: Exception) {
            // Ignore storage info errors
        }
    }

    private fun loadRecentFiles() {
        try {
            val root = FileUtils.getPrimaryRoot()
            val files = try {
                FileUtils.listFiles(root).filter { it.isFile }.take(8)
            } catch (e: Exception) {
                emptyList()
            }
            val items = files.map { FileItem(it) }

            if (recyclerRecent.layoutManager == null) {
                recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
            }

            recentAdapter = FileAdapter(
                items = items,
                context = requireContext(),
                isSelectionMode = false,
                onItemClick = { item -> openFile(item) }
            )
            recyclerRecent.adapter = recentAdapter
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "loadRecentFiles error: ${e.message}")
        }
    }

    private fun openFile(item: FileItem) {
        try {
            FilesFragment.openFileWithIntent(requireContext(), item.file)
        } catch (e: Exception) { /* ignore */ }
    }

    private fun updateVipBanner() {
        try {
            val isVip = Preferences.isVip()
            vipBanner.visibility = if (isVip) View.GONE else View.VISIBLE
            txtGreeting.text = getString(if (isVip) R.string.welcome_vip else R.string.app_name)
        } catch (e: Exception) { /* ignore */ }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
