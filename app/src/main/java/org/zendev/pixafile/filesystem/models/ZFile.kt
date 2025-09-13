package org.zendev.pixafile.filesystem.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.zendev.pixafile.filesystem.ItemType

@Parcelize
data class ZFile(
    var path: String = "",
    var name: String = "",
    var createDate: String = "ERROR",
    var size: String = "0B",
    var type : ItemType = ItemType.Folder,
    var isFolder: Boolean = false
) : Parcelable