package org.zendev.pixafile.filesystem.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Image(
    var uri: Uri,
    var path: String,
    var name: String,
    var size: Long,
    var createDate: String,
    var width: Int,
    var height: Int
) : Parcelable

