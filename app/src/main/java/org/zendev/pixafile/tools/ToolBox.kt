package org.zendev.pixafile.tools

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.commons.io.FilenameUtils
import org.zendev.pixafile.R
import org.zendev.pixafile.filesystem.models.ZFile
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val selectedItems = mutableMapOf<ZFile, MaterialCheckBox>()

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(label, text)

    clipboardManager.setPrimaryClip(clipData)
}

fun getAllViews(view: View, includeViewGroup: Boolean): MutableList<View> {
    val result = mutableListOf<View>()

    if (view is ViewGroup) {
        if (includeViewGroup) {
            result += view
        }

        for (i in 0 until view.childCount) {
            result += getAllViews(view.getChildAt(i), includeViewGroup)
        }
    } else {
        result += view
    }

    return result
}

fun startDialogAnimation(view: View) {
    var animationDuration = 75L
    val views = getAllViews(view, true)

    views.forEachIndexed { _, v ->
        animationDuration += 15

        val animator = ObjectAnimator.ofFloat(v, "translationY", 100f, 0f).apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        animator.start()
    }
}

fun convertSize(size: Double): String {
    var i = 0
    var tmp = size

    if (size <= 0) {
        return size.toString()
    } else if (size < 1024) {
        return "$size B"
    }

    while (tmp > 1024) {
        tmp /= 1024.0
        i++
    }

    val dotPos = tmp.toString().indexOf(".")
    var real = tmp.toString().substring(0, dotPos)

    real = if ((dotPos + 3) > tmp.toString().length) {
        real + tmp.toString().substring(dotPos)
    } else {
        real + tmp.toString().substring(dotPos, dotPos + 3)
    }

    return when (i) {
        1 -> "$real KB"
        2 -> "$real MB"
        3 -> "$real GB"
        4 -> "$real TB"
        else -> "ERR"
    }
}

fun disableScreenPadding(view: View) {
    ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
        v.setPadding(0, 0, 0, 0)
        insets
    }
}

fun isActivityDestroyed(context: Context): Boolean {
    return if (context is Activity) {
        context.isFinishing || context.isDestroyed
    } else {
        false
    }
}

fun getDeviceStoragePath(): String {
    return Environment.getExternalStorageDirectory().absolutePath
}

suspend fun loadImageAsync(context: Context, imageView: ImageView, file: File) =
    suspendCancellableCoroutine { cont ->
        if (!isActivityDestroyed(context)) {
            Glide.with(context).load(file).centerInside().thumbnail(0.5f)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (!cont.isCompleted) {
                            cont.resumeWithException(e ?: Exception("Load failed"))
                        }

                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {

                        if (!cont.isCompleted) {
                            cont.resume(Unit)
                        }

                        return false
                    }
                }).into(imageView)
        }
    }

fun formatCreateDate(file: File): String {
    val dateInMillis: Long? =
        try {
            val path = file.toPath()
            val attr = Files.readAttributes(path, BasicFileAttributes::class.java)
            attr.creationTime()?.toMillis()
        } catch (e: Exception) {
            null
        }

    val finalDate =
        dateInMillis ?: file.lastModified()  // fallback to lastModified if creation not available

    val sdf = SimpleDateFormat("YY / MM / dd", Locale.getDefault())
    return sdf.format(Date(finalDate))
}

fun isImage(file: File): Boolean {
    val imageExtensions = listOf(
        "jpg", "jpeg", "jfif", "webp", "exif", "bmp", "png", "svg", "fig", "tiff",
        "gif"
    )

    return imageExtensions.contains(FilenameUtils.getExtension(file.absolutePath).lowercase())
}

fun isVideo(file: File): Boolean {
    val videoExtensions = listOf(
        "mp4",
        "avi",
        "mkv",
        "flv",
        "3gp",
        "nsv",
        "webm",
        "vob",
        "gifv",
        "mov",
        "qt",
        "wmv",
        "viv",
        "amv",
        "m4p",
        "m4v",
        "mpg",
        "mp2",
        "mpv",
        "svi",
        "3g2",
        "f4v",
        "f4p",
        "f4a",
        "f4b",
        "mpeg",
        "mov"
    )

    return videoExtensions.contains(FilenameUtils.getExtension(file.absolutePath).lowercase())
}

fun isAudio(file: File): Boolean {
    val audioExtensions = listOf(
        "mp3", "wav", "ogg", "mpa", "aac", "au", "m4a", "m4b", "mpc", "oga", "tta",
        "wma", "wv"
    )

    return audioExtensions.contains(FilenameUtils.getExtension(file.absolutePath).lowercase())
}

fun getStackTrace(e: Throwable): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)

    e.printStackTrace(pw)
    return sw.toString()
}

fun resizeTextViewDrawable(context: Context, textView: TextView, drawableIcon: Int, size: Int) {
    val density = context.resources.displayMetrics.density
    val desiredWidthInPx = (size * density).toInt()
    val desiredHeightInPx = (size * density).toInt()

    val drawable = ContextCompat.getDrawable(context, drawableIcon)
    drawable?.setBounds(0, 0, desiredWidthInPx, desiredHeightInPx)
    textView.setCompoundDrawables(drawable, null, null, null)
}