package org.zendev.pixafile.filesystem

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import org.zendev.pixafile.filesystem.models.ZFile
import org.zendev.pixafile.tools.convertSize
import org.zendev.pixafile.tools.formatCreateDate
import org.zendev.pixafile.tools.isAudio
import org.zendev.pixafile.tools.isImage
import org.zendev.pixafile.tools.isVideo
import java.io.File

class DeviceRepository(private val context: Context) {

    private var fileObserver: ContentObserver? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    // This LiveData holds the list of files and will be observed by the UI.
    private val itemsLiveData = MutableLiveData<MutableList<ZFile>>()

    // A private function to load the files from the specified path.
    // This now runs on a background thread.
    private fun loadFiles(path: String) {
        val files = mutableListOf<ZFile>()
        val folders = mutableListOf<ZFile>()
        val items = mutableListOf<ZFile>()

        val directory = File(path)

        directory.listFiles()?.forEach {
            if (!it.isHidden) {
                if (it.isDirectory) {
                    folders.add(
                        ZFile(
                            path = it.absolutePath,
                            name = it.name,
                            createDate = formatCreateDate(it),
                            "Folder",
                            type = ItemType.Folder,
                            isFolder = true
                        )
                    )
                } else {
                    var type = ItemType.Folder

                    if (isImage(it)) {
                        type = ItemType.Image
                    } else if (isVideo(it)) {
                        type = ItemType.Video
                    } else if (isAudio(it)) {
                        type = ItemType.Audio
                    }

                    files.add(
                        ZFile(
                            path = it.absolutePath,
                            name = it.name,
                            createDate = formatCreateDate(it),
                            size = convertSize(it.length().toDouble()),
                            type = type,
                            isFolder = false
                        )
                    )
                }
            }
        }

        val sortedFiles = files.sortedWith(compareBy { it.name.lowercase() })
        val sortedFolders = folders.sortedWith(compareBy { it.name.lowercase() })

        items.addAll(sortedFolders)
        items.addAll(sortedFiles)

        // Use postValue to safely update LiveData from a background thread.
        itemsLiveData.postValue(items)
    }

    /* Starts a ContentObserver to watch for changes in the MediaStore */
    private fun startWatching(path: String) {
        // Stop watching the previous directory to prevent memory leaks.
        stopWatching()

        // Create and start a HandlerThread for background operations.
        handlerThread = HandlerThread("FileObserverThread")
        handlerThread?.start()
        handler = Handler(handlerThread!!.looper)

        // Define the URI to watch. This watches for all external media changes.
        val uri = MediaStore.Files.getContentUri("external")

        fileObserver = object : ContentObserver(handler) {
            // This method is called when a change is detected.
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Reload the files from the current path on the background thread.
                handler?.post {
                    loadFiles(path)
                }
            }
        }

        // Register the ContentObserver with the ContentResolver.
        context.contentResolver.registerContentObserver(uri, true, fileObserver!!)
    }

    /* Unregisters the ContentObserver and stops the background thread. */
    fun stopWatching() {
        if (fileObserver != null) {
            context.contentResolver.unregisterContentObserver(fileObserver!!)
            fileObserver = null
        }

        handlerThread?.quitSafely()
        handlerThread = null
    }

    fun getFiles(path: String): MutableLiveData<MutableList<ZFile>> {
        // Start watching for changes in the filesystem.
        startWatching(path)

        // Load the initial set of files.
        handler?.post {
            loadFiles(path)
        }

        // Return the LiveData to the ViewModel or UI.
        return itemsLiveData
    }
}
