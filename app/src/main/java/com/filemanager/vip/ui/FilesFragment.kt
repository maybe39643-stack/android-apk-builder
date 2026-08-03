package com.filemanager.vip.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.filemanager.vip.R
import com.filemanager.vip.model.FileCategory
import com.filemanager.vip.model.FileItem
import com.filemanager.vip.ui.adapters.FileAdapter
import com.filemanager.vip.util.FileUtils
import com.filemanager.vip.util.Preferences
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class FilesFragment : Fragment() {

    private var currentDir: File = FileUtils.getPrimaryRoot()
    private var filterCategory: FileCategory? = null
    private var allItems: List<FileItem> = emptyList()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchInput: TextInputEditText
    private lateinit var selectionBar: LinearLayout
    private lateinit var txtSelectionCount: TextView
    private lateinit var txtPath: TextView
    private lateinit var pasteBar: LinearLayout
    private lateinit var txtPasteInfo: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerFiles: RecyclerView
    private lateinit var fabNewFolder: FloatingActionButton
    private lateinit var adapter: FileAdapter

    private val clipboard = mutableListOf<File>()
    private var clipboardIsCopy = true

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_CATEGORY)?.let { name ->
            filterCategory = FileCategory.valueOf(name)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_files, container, false)
        bindViews(view)
        setupToolbar()
        setupListeners()
        if (filterCategory == null) {
            currentDir = File(Preferences.getLastDir()).takeIf { it.exists() } ?: FileUtils.getPrimaryRoot()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (ensurePermission()) refresh() else swipeRefresh.isRefreshing = false
    }

    private fun bindViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        searchInput = view.findViewById(R.id.search_input)
        selectionBar = view.findViewById(R.id.selection_bar)
        txtSelectionCount = view.findViewById(R.id.txt_selection_count)
        txtPath = view.findViewById(R.id.txt_path)
        pasteBar = view.findViewById(R.id.paste_bar)
        txtPasteInfo = view.findViewById(R.id.txt_paste_info)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        recyclerFiles = view.findViewById(R.id.recycler_files)
        fabNewFolder = view.findViewById(R.id.fab_new_folder)
    }

    private fun setupToolbar() {
        toolbar.title = getString(R.string.files)
        if (filterCategory != null) {
            toolbar.subtitle = getString(filterCategory!!.labelRes)
        }
        updateToolbar()
    }

    private fun setupListeners() {
        recyclerFiles.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileAdapter(
            items = emptyList(),
            context = requireContext(),
            isSelectionMode = false,
            onItemClick = { item -> onFileClick(item) },
            onStarClick = { item -> toggleFavorite(item) },
            onMoreClick = { item -> showActionsDialog(item) }
        )
        recyclerFiles.adapter = adapter

        swipeRefresh.setOnRefreshListener { refresh() }
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary, R.color.accent)

        fabNewFolder.setOnClickListener { showNewFolderDialog() }

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                applySearch(s?.toString().orEmpty())
            }
        })

        view?.findViewById<MaterialButton>(R.id.btn_close_selection)?.setOnClickListener {
            adapter.setSelectionMode(false)
            updateSelectionBar()
        }
        view?.findViewById<MaterialButton>(R.id.btn_select_all)?.setOnClickListener {
            adapter.selectAll()
            updateSelectionBar()
        }
        view?.findViewById<MaterialButton>(R.id.btn_copy)?.setOnClickListener {
            copySelected()
        }
        view?.findViewById<MaterialButton>(R.id.btn_move)?.setOnClickListener {
            moveSelected()
        }
        view?.findViewById<MaterialButton>(R.id.btn_delete)?.setOnClickListener {
            deleteSelected()
        }
        view?.findViewById<MaterialButton>(R.id.btn_paste)?.setOnClickListener {
            pasteClipboard()
        }
    }

    private fun ensurePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                true
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.storage_permission_title)
                    .setMessage(R.string.permission_needed)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        try {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${requireContext().packageName}")
                                )
                            )
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                false
            }
        } else {
            val perms = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            val needed = perms.filter {
                ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isEmpty()) true
            else {
                permissionLauncher.launch(needed.toTypedArray())
                false
            }
        }
    }

    private fun refresh() {
        val items = if (filterCategory != null) {
            FileUtils.listFiles(FileUtils.getPrimaryRoot())
                .map { FileItem(it) }
                .filter { it.category == filterCategory }
        } else {
            FileUtils.listFiles(currentDir).map { FileItem(it) }
        }
        allItems = items
        adapter.updateItems(items)
        txtPath.text = if (filterCategory != null) getString(R.string.files) else currentDir.absolutePath
        swipeRefresh.isRefreshing = false
        if (filterCategory == null) {
            Preferences.setLastDir(currentDir.absolutePath)
        }
        updateToolbar()
    }

    private fun applySearch(query: String) {
        val q = query.trim().lowercase()
        adapter.updateItems(
            if (q.isEmpty()) allItems else allItems.filter { it.name.lowercase().contains(q) }
        )
    }

    private fun updateToolbar() {
        val canGoUp = filterCategory == null &&
            currentDir.absolutePath != FileUtils.getPrimaryRoot().absolutePath
        if (canGoUp) {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron)
            if (drawable != null) {
                val rotated = android.graphics.drawable.RotateDrawable().apply {
                    this.drawable = drawable
                    fromDegrees = 0f
                    toDegrees = 180f
                    level = 10000
                }
                toolbar.navigationIcon = rotated
            }
            toolbar.setNavigationOnClickListener {
                currentDir = currentDir.parentFile ?: FileUtils.getPrimaryRoot()
                searchInput.setText("")
                refresh()
            }
        } else {
            toolbar.navigationIcon = null
        }
    }

    private fun onFileClick(item: FileItem) {
        if (adapter.isSelectionMode()) return
        if (item.isDirectory) {
            if (filterCategory != null) {
                currentDir = item.file
                filterCategory = null
                toolbar.subtitle = null
            } else {
                currentDir = item.file
            }
            searchInput.setText("")
            refresh()
        } else {
            openFileWithIntent(requireContext(), item.file)
        }
    }

    private fun toggleFavorite(item: FileItem) {
        val path = item.file.absolutePath
        if (Preferences.isFavorite(path)) {
            Preferences.removeFavorite(path)
        } else {
            Preferences.addFavorite(path)
        }
        adapter.updateItems(allItems)
    }

    private fun updateSelectionBar() {
        val inSelection = adapter.isSelectionMode()
        selectionBar.visibility = if (inSelection) View.VISIBLE else View.GONE
        txtSelectionCount.text = adapter.getSelectedCount().toString()
        if (!inSelection) {
            adapter.updateItems(allItems)
        }
    }

    private fun copySelected() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return
        clipboard.clear()
        clipboard.addAll(selected.map { it.file })
        clipboardIsCopy = true
        adapter.setSelectionMode(false)
        updateSelectionBar()
        showPasteBar()
    }

    private fun moveSelected() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return
        clipboard.clear()
        clipboard.addAll(selected.map { it.file })
        clipboardIsCopy = false
        adapter.setSelectionMode(false)
        updateSelectionBar()
        showPasteBar()
    }

    private fun deleteSelected() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return
        confirmDelete(selected.map { it.file })
    }

    private fun confirmDelete(files: List<File>) {
        val names = files.joinToString(", ") { it.name }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_confirm, names))
            .setPositiveButton(R.string.delete) { _, _ ->
                files.forEach { FileUtils.delete(it) }
                adapter.setSelectionMode(false)
                updateSelectionBar()
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPasteBar() {
        pasteBar.visibility = View.VISIBLE
        val action = if (clipboardIsCopy) getString(R.string.copy) else getString(R.string.move)
        txtPasteInfo.text = "$action (${clipboard.size}) → ${currentDir.name}"
    }

    private fun pasteClipboard() {
        if (clipboard.isEmpty()) {
            pasteBar.visibility = View.GONE
            return
        }
        clipboard.forEach { f ->
            if (clipboardIsCopy) FileUtils.copy(f, currentDir) else FileUtils.move(f, currentDir)
        }
        clipboard.clear()
        pasteBar.visibility = View.GONE
        refresh()
        Toast.makeText(requireContext(), R.string.done, Toast.LENGTH_SHORT).show()
    }

    private fun showNewFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_folder, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.input_name)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {
            val name = input.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.invalid_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newDir = File(currentDir, name)
            if (newDir.exists()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.already_exists, name),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (newDir.mkdirs()) {
                dialog.dismiss()
                refresh()
            } else {
                Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun showActionsDialog(item: FileItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_actions, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val actionOpen = dialogView.findViewById<TextView>(R.id.action_open)
        val actionRename = dialogView.findViewById<TextView>(R.id.action_rename)
        val actionCopy = dialogView.findViewById<TextView>(R.id.action_copy)
        val actionMove = dialogView.findViewById<TextView>(R.id.action_move)
        val actionShare = dialogView.findViewById<TextView>(R.id.action_share)
        val actionProperties = dialogView.findViewById<TextView>(R.id.action_properties)
        val actionDelete = dialogView.findViewById<TextView>(R.id.action_delete)

        title.text = item.name
        if (item.isDirectory) {
            actionOpen.visibility = View.GONE
            actionShare.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        actionOpen.setOnClickListener {
            dialog.dismiss()
            openFileWithIntent(requireContext(), item.file)
        }
        actionRename.setOnClickListener {
            dialog.dismiss()
            showRenameDialog(item)
        }
        actionCopy.setOnClickListener {
            dialog.dismiss()
            clipboard.clear()
            clipboard.add(item.file)
            clipboardIsCopy = true
            showPasteBar()
        }
        actionMove.setOnClickListener {
            dialog.dismiss()
            clipboard.clear()
            clipboard.add(item.file)
            clipboardIsCopy = false
            showPasteBar()
        }
        actionShare.setOnClickListener {
            dialog.dismiss()
            shareFile(item.file)
        }
        actionProperties.setOnClickListener {
            dialog.dismiss()
            showPropertiesDialog(item)
        }
        actionDelete.setOnClickListener {
            dialog.dismiss()
            confirmDelete(listOf(item.file))
        }
        dialog.show()
    }

    private fun showRenameDialog(item: FileItem) {
        val input = EditText(requireContext()).apply {
            setText(item.name)
            selectAll()
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_to)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) return@setPositiveButton
                val newFile = File(item.file.parentFile, newName)
                if (newFile.exists()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.already_exists, newName),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                if (item.file.renameTo(newFile)) {
                    refresh()
                } else {
                    Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPropertiesDialog(item: FileItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_properties, null)
        val propName = dialogView.findViewById<TextView>(R.id.prop_name)
        val propPath = dialogView.findViewById<TextView>(R.id.prop_path)
        val propSize = dialogView.findViewById<TextView>(R.id.prop_size)
        val propModified = dialogView.findViewById<TextView>(R.id.prop_modified)
        val propItems = dialogView.findViewById<TextView>(R.id.prop_items)
        val btnClose = dialogView.findViewById<Button>(R.id.prop_btn_close)

        propName.text = item.name
        propPath.text = getString(R.string.path) + ": " + item.file.absolutePath
        propModified.text = getString(R.string.modified) + ": " + FileUtils.formatDate(item.lastModified)
        if (item.isDirectory) {
            val (count, _) = FileUtils.countFiles(item.file)
            propSize.visibility = View.GONE
            propItems.text = getString(R.string.items) + ": $count"
        } else {
            propSize.text = getString(R.string.size) + ": " + FileUtils.formatSize(item.size)
            propItems.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: FileCategory? = null): FilesFragment {
            val fragment = FilesFragment()
            if (category != null) {
                fragment.arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category.name)
                }
            }
            return fragment
        }

        fun openFileWithIntent(context: Context, file: File) {
            if (!file.exists()) return
            try {
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                } else {
                    Uri.fromFile(file)
                }
                val mime = context.contentResolver.getType(uri) ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.open)))
            } catch (e: Exception) {
                Toast.makeText(context, R.string.cannot_open, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
