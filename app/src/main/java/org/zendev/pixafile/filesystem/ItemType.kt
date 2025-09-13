package org.zendev.pixafile.filesystem

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ItemType : Parcelable {
    Image,
    Video,
    Audio,
    Document,
    Folder
}