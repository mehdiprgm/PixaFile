package org.zendev.pixafile.filesystem.models

import android.net.Uri
import android.os.Environment
import android.provider.MediaStore


class NewFolder(val name: String, val path: String) {

    fun getResolverURI(): Uri {
        return when (path) {
            Environment.DIRECTORY_DOWNLOADS -> MediaStore.Files.getContentUri("external")
            Environment.DIRECTORY_DCIM -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_MUSIC -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_ALARMS -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_MOVIES -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_AUDIOBOOKS -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_DOCUMENTS -> MediaStore.Files.getContentUri("external")
            Environment.DIRECTORY_NOTIFICATIONS -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_PICTURES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_PODCASTS -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_RECORDINGS -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Environment.DIRECTORY_RINGTONES -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
    }

    fun getMimeType(): String {
        return when (path) {
            Environment.DIRECTORY_DOWNLOADS -> "application/octet-stream"
            Environment.DIRECTORY_DCIM -> "image/jpeg"
            Environment.DIRECTORY_MUSIC -> "audio/mpeg"
            Environment.DIRECTORY_ALARMS -> "audio/mpeg"
            Environment.DIRECTORY_MOVIES -> "video/mp4"
            Environment.DIRECTORY_AUDIOBOOKS -> "audio/mpeg"
            Environment.DIRECTORY_DOCUMENTS -> ""
            Environment.DIRECTORY_NOTIFICATIONS -> "audio/mpeg"
            Environment.DIRECTORY_PICTURES -> "image/jpeg"
            Environment.DIRECTORY_PODCASTS -> "audio/mpeg"
            Environment.DIRECTORY_RECORDINGS -> "audio/mpeg"
            Environment.DIRECTORY_RINGTONES -> "audio/mpeg"
            else -> ""
        }
    }
}
