package org.zendev.pixafile.activity

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import org.zendev.pixafile.Dialog
import org.zendev.pixafile.R
import org.zendev.pixafile.adapter.FileAdapter
import org.zendev.pixafile.databinding.ActivityMainBinding
import org.zendev.pixafile.filesystem.ItemType
import org.zendev.pixafile.filesystem.ViewModel
import org.zendev.pixafile.filesystem.models.ZFile
import org.zendev.pixafile.tools.disableScreenPadding
import org.zendev.pixafile.tools.getAllViews
import org.zendev.pixafile.tools.getDeviceStoragePath
import org.zendev.pixafile.tools.resizeTextViewDrawable
import org.zendev.pixafile.tools.selectedItems
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener,
    NavigationView.OnNavigationItemSelectedListener {
    private lateinit var b: ActivityMainBinding

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var viewModel: ViewModel

    private lateinit var fileAdapter: FileAdapter

    private var currentFile: ZFile = ZFile(path = getDeviceStoragePath())
    private var isSelectionModeActivated = false

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }

        if (granted) {
            loadFiles(currentFile)
        } else {
            Dialog.confirm(
                this,
                R.drawable.ic_lock,
                "Permission Denied",
                "The application doesn't have enough permissions to access device files."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        disableScreenPadding(b.root)

        loadBundle(savedInstanceState)

        setupNavigationDrawer()
        setupViewModel()
        setOnBackPressedListener()

        requestStoragePermission()

        b.btnMenu.setOnClickListener(this)
        b.btnSettings.setOnClickListener(this)

        b.tvRefresh.setOnClickListener(this)
        b.tvNewFolder.setOnClickListener(this)

        resizeTextViews()
        loadFiles(currentFile)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("CurrentFile", currentFile)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnSettings -> {
                setOptionsButtonsVisibility(!b.tvRefresh.isVisible)
            }

            R.id.tvRefresh -> {
                setOptionsButtonsVisibility(false)
                loadFiles(currentFile)
            }

            R.id.tvNewFolder -> {
                setOptionsButtonsVisibility(false)

                lifecycleScope.launch {
                    try {
                        val newFolder = Dialog.newFolder(this@MainActivity)
                        if (newFolder.name.isNotEmpty()) {
                            val uri = contentResolver.insert(
                                newFolder.getResolverURI(), ContentValues().apply {
                                    put(
                                        MediaStore.MediaColumns.RELATIVE_PATH,
                                        newFolder.path + "/" + newFolder.name
                                    )

                                    put(MediaStore.MediaColumns.DISPLAY_NAME, newFolder.name)
                                    put(MediaStore.MediaColumns.MIME_TYPE, newFolder.getMimeType())
                                })

                            if (uri == null) {
                                Dialog.confirm(
                                    this@MainActivity,
                                    R.drawable.ic_error,
                                    "Creation failed",
                                    "Failed to create new folder"
                                )
                            } else {
                                /* Only update the folder content if we create new folder on the same path */
                                if (newFolder.path == currentFile.path) {
                                    loadFiles(currentFile)
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        Dialog.exception(this@MainActivity, ex)
                    }
                }
            }

            R.id.btnMenu -> {
                if (isSelectionModeActivated) {
                    disableSelectionMode()
                } else {
                    b.main.openDrawer(GravityCompat.START)
                }
            }
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menuAboutPixaFile -> {
                b.main.closeDrawer(GravityCompat.START)
                Dialog.aboutPixaFile(this)
            }
        }

        return true
    }

    private fun loadBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            currentFile = ZFile(
                path = getDeviceStoragePath(),
                name = "Internal Storage",
                createDate = "",
                type = ItemType.Folder,
                isFolder = true
            )
        } else {
            var file = savedInstanceState.getParcelable("CurrentFile", ZFile::class.java)

            if (file == null) {
                file = ZFile(
                    path = getDeviceStoragePath(),
                    name = "Internal Storage",
                    createDate = "",
                    type = ItemType.Folder,
                    isFolder = true
                )
            }

            currentFile = file
        }
    }

    private fun setupNavigationDrawer() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b.navMain.setNavigationItemSelectedListener(this)
    }

    /* This function help to reduce code to access resource color */
    private fun getResourceColor(colorResource: Int): Int {
        return ContextCompat.getColor(this, colorResource)
    }

    private fun setOptionsButtonsVisibility(visible: Boolean) {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val rotate180Reverse = AnimationUtils.loadAnimation(this, R.anim.rotate_180_reverse)

        fadeInAnimation.duration = 300
        fadeOutAnimation.duration = 300
        rotate180Reverse.duration = 300

        b.btnSettings.animation = rotate180Reverse

        if (visible) {
            b.tvRefresh.apply {
                visibility = View.VISIBLE
                animation = fadeInAnimation
                isClickable = true
            }

            b.tvNewFolder.apply {
                visibility = View.VISIBLE
                animation = fadeInAnimation
                isClickable = true
            }
        } else {
            b.tvRefresh.apply {
                visibility = View.GONE
                animation = fadeOutAnimation
                isClickable = false
            }

            b.tvNewFolder.apply {
                visibility = View.GONE
                animation = fadeOutAnimation
                isClickable = false
            }
        }
    }

    private fun showEmptyList(visible: Boolean) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.bounce)
        animation.duration = 500

        if (visible) {
            b.lottieEmpty.visibility = View.VISIBLE
            b.tvEmpty.visibility = View.VISIBLE
            b.tvAdd.visibility = View.VISIBLE

            b.lottieEmpty.animation = animation
            b.tvEmpty.animation = animation
            b.tvAdd.animation = animation
        } else {
            b.lottieEmpty.animation = null
            b.tvEmpty.animation = null
            b.tvAdd.animation = null

            b.lottieEmpty.visibility = View.GONE
            b.tvEmpty.visibility = View.GONE
            b.tvAdd.visibility = View.GONE
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ViewModel::class.java]
    }

    private fun requestStoragePermission() {
        val permissions = mutableListOf<String>()

        permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(
                this, it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            loadFiles(currentFile)
        } else {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun loadFiles(zFile: ZFile) {
        try {
            val paths = zFile.path.replace(getDeviceStoragePath(), "Device Storage").split("/")
            fileAdapter = FileAdapter(this)

            b.rcMain.adapter = fileAdapter
            b.rcMain.layoutManager = LinearLayoutManager(this)

            viewModel.getFiles(zFile.path).observe(this) {
                fileAdapter.files = it
                showEmptyList(it.isEmpty())
            }

            // Set the click listeners once
            fileAdapter.setOnItemClickListener(object : FileAdapter.OnItemClickListener {
                override fun onItemClick(checkBox: MaterialCheckBox, zFile: ZFile) {
                    if (isSelectionModeActivated) {
                        if (!removeSelectedItem(checkBox, zFile)) {
                            addNewSelectedItem(checkBox, zFile)
                        }
                    } else {
                        if (zFile.isFolder) {
                            loadFiles(zFile)
                        } else {/* Open file */
                        }
                    }
                }

                override fun onItemLongClick(
                    checkBox: MaterialCheckBox,
                    zFile: ZFile
                ) {/* Open SnackBar */
                    if (!isSelectionModeActivated) {
                        enableSelectionMode()
                        addNewSelectedItem(checkBox, zFile)
                    }
                }
            })

            currentFile = zFile
            updatePathButtons(paths)
        } catch (ex: Exception) {
            Dialog.exception(this, ex)
        }
    }

    private fun setOnBackPressedListener() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {/* We are in the root (/) path, no longer can go back */
                if (isSelectionModeActivated) {
                    disableSelectionMode()
                } else {
                    if (currentFile.path == getDeviceStoragePath()) {
                        finish()
                    } else {
                        val parentPath = File(currentFile.path).parent

                        if (parentPath == null) {
                            Dialog.confirm(
                                this@MainActivity,
                                R.drawable.ic_error,
                                "Path error",
                                "Unable to access previous folder."
                            )
                        } else {
                            currentFile = ZFile(path = parentPath)
                            loadFiles(currentFile)
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            this, onBackPressedCallback
        )
    }

    private fun resizeTextViews() {
        resizeTextViewDrawable(
            this, b.tvRefresh, R.drawable.ic_refresh, 18
        )

        resizeTextViewDrawable(
            this, b.tvNewFolder, R.drawable.ic_add, 18
        )
    }

    private fun enableSelectionMode() {
        isSelectionModeActivated = true
        fileAdapter.setShowCheckboxes(true)

        b.btnMenu.setImageResource(R.drawable.ic_close)
    }

    private fun disableSelectionMode() {
        isSelectionModeActivated = false
        fileAdapter.setShowCheckboxes(false)

        for (item in selectedItems) {
            item.value.isChecked = false
        }

        selectedItems.clear()

        b.btnMenu.setImageResource(R.drawable.ic_menu2)
        updateTitleTextView()
    }

    /**
     * we don't want duplicate items in the list, so first check the list
     */
    private fun addNewSelectedItem(checkBox: MaterialCheckBox, zFile: ZFile) {
        checkBox.isChecked = true

        selectedItems.put(zFile, checkBox)
        updateTitleTextView()
    }

    /**
     * removes the selected item, if item exist in the list return true else returns false
     */
    private fun removeSelectedItem(checkBox: MaterialCheckBox, zFile: ZFile): Boolean {
        if (selectedItems.contains(zFile)) {
            checkBox.isChecked = false

            selectedItems.remove(zFile)
            updateTitleTextView()

            return true
        }

        return false
    }

    private fun updateTitleTextView() {
        if (selectedItems.isEmpty()) {
            b.tvAppTitle.text = resources.getString(R.string.app_name)
        } else {
            b.tvAppTitle.text = "${selectedItems.size} Selected"
        }
    }

    private fun updatePathButtons(buttons: List<String>) {
        b.layPathButtonsContainer.removeAllViews()

        for (button in buttons) {
            val newButton = Button(this, null, 0, R.style.TextSmall).apply {
                text = button
                isAllCaps = false
                setTypeface(null, Typeface.BOLD)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 20, 0)
                    setPadding(20)
                }
            }

            // Create a new GradientDrawable to define the button's shape
            val roundedCornerDrawable = GradientDrawable()

            // Set the color for the button's background
            roundedCornerDrawable.setColor(ContextCompat.getColor(this, R.color.gray))

            // Set the corner radius in pixels.
            // We use TypedValue.applyDimension to convert 20dp to pixels,
            // which ensures it looks correct on all screen densities.
            val cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40F,
                resources.displayMetrics
            )

            roundedCornerDrawable.cornerRadius = cornerRadius
            newButton.background = roundedCornerDrawable

            b.layPathButtonsContainer.addView(newButton)
        }
    }
}