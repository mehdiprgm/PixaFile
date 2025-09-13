package org.zendev.pixafile

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.method.PasswordTransformationMethod
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zendev.pixafile.filesystem.models.NewFolder
import org.zendev.pixafile.tools.getStackTrace
import org.zendev.pixafile.tools.startDialogAnimation
import kotlin.coroutines.resume
import kotlin.math.E

class Dialog {
    companion object {

        private fun createDialog(context: Context, layoutFile: Int): Dialog {
            val dialog = Dialog(context)
            dialog.setContentView(layoutFile)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            dialog.setCancelable(false)
            return dialog
        }

        private fun getFolderPathBySpinnerIndex(spinner: Spinner): String {
            return when (spinner.selectedItemPosition) {
                0 -> Environment.DIRECTORY_DOWNLOADS
                1 -> Environment.DIRECTORY_DCIM
                2 -> Environment.DIRECTORY_MUSIC
                3 -> Environment.DIRECTORY_ALARMS
                4 -> Environment.DIRECTORY_MOVIES
                5 -> Environment.DIRECTORY_AUDIOBOOKS
                6 -> Environment.DIRECTORY_DOCUMENTS
                7 -> Environment.DIRECTORY_NOTIFICATIONS
                8 -> Environment.DIRECTORY_PICTURES
                9 -> Environment.DIRECTORY_PODCASTS
                10 -> Environment.DIRECTORY_RECORDINGS
                11 -> Environment.DIRECTORY_RINGTONES
                else -> ""
            }
        }

        fun aboutPixaFile(context: Context) {
            val dialog = createDialog(
                context, R.layout.dialog_about_pixafile
            )

            dialog.setCancelable(true)
            startDialogAnimation(dialog.findViewById(R.id.main))

            val imgGmail = dialog.findViewById<ImageView>(R.id.imgGmail)
            val imgTelegram = dialog.findViewById<ImageView>(R.id.imgTelegram)
            val imgInstagram = dialog.findViewById<ImageView>(R.id.imgInstagram)
            val imgGithub = dialog.findViewById<ImageView>(R.id.imgGithub)

            /* bug here */
            imgGmail.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:mfcrisis2016@gmail.com".toUri()
                    putExtra(Intent.EXTRA_SUBJECT, "")
                }

                /* optional: restrict to Gmail app if installed */
                context.startActivity(intent)
                dialog.dismiss()
            }

            imgTelegram.setOnClickListener {
                val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://t.me/zenDEv2".toUri()/* optional: limit to Telegram app only */
                    setPackage("org.telegram.messenger")
                }

                if (telegramIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(telegramIntent)
                } else {/* fallback: open in browser if Telegram is not installed */
                    val browserIntent = Intent(Intent.ACTION_VIEW, "https://t.me/zenDEv2".toUri())
                    context.startActivity(browserIntent)
                }

                dialog.dismiss()
            }

            imgInstagram.setOnClickListener {
                val uri = "http://instagram.com/_u/mehdi.la.79".toUri()
                val instagramIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.instagram.android")
                }

                if (instagramIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(instagramIntent)
                } else {/* fallback to browser if Instagram app isn't installed */
                    val webIntent = Intent(
                        Intent.ACTION_VIEW, "http://instagram.com/mehdi.la.79".toUri()
                    )
                    context.startActivity(webIntent)
                }

                dialog.dismiss()
            }

            imgGithub.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/mehdiprgm".toUri())
                context.startActivity(intent)
            }

            dialog.show()
        }

        fun confirm(context: Context, icon: Int, title: String, message: String) {
            val dialog = createDialog(context, R.layout.dialog_confirm)
            startDialogAnimation(dialog.findViewById(R.id.main))

            val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
            val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)

            tvTitle.text = title
            tvMessage.text = message

            dialog.findViewById<ImageView>(R.id.imgLogo).setImageDrawable(
                ContextCompat.getDrawable(context, icon)
            )

            dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        fun exception(context: Context, exception: Exception) {
            confirm(context, R.drawable.ic_error, exception.message!!, getStackTrace(exception))
        }

        suspend fun ask(context: Context, icon: Int, title: String, message: String): Boolean =
            suspendCancellableCoroutine { continuation ->
                val dialog = createDialog(
                    context, R.layout.dialog_ask
                )
                startDialogAnimation(dialog.findViewById(R.id.main))

                val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
                val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)

                tvTitle.text = title
                tvMessage.text = message

                dialog.findViewById<ImageView>(R.id.imgIcon).setImageDrawable(
                    ContextCompat.getDrawable(context, icon)
                )

                dialog.findViewById<Button>(R.id.btnNo).setOnClickListener {
                    continuation.resume(false)
                    dialog.dismiss()
                }

                dialog.findViewById<Button>(R.id.btnYes).setOnClickListener {
                    continuation.resume(true)
                    dialog.dismiss()
                }

                dialog.setOnCancelListener {
                    continuation.resume(false)
                }

                dialog.show()
                continuation.invokeOnCancellation {
                    dialog.dismiss()
                }
            }

        suspend fun textInput(
            context: Context,
            title: String,
            message: String,
            hint: String,
            defaultText: String = "",
            isPassword: Boolean = false,
            isNumber: Boolean
        ): String = suspendCancellableCoroutine { continuation ->
            val dialog = createDialog(
                context, R.layout.dialog_text_input
            )
            startDialogAnimation(dialog.findViewById(R.id.main))

            val txtInput = dialog.findViewById<EditText>(R.id.txtInput)

            dialog.findViewById<TextView>(R.id.tvTitle).text = title
            dialog.findViewById<TextView>(R.id.tvMessage).text = message

            txtInput.hint = hint
            txtInput.setText(defaultText)

            if (isPassword) {
                if (isNumber) {
                    txtInput.inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_VARIATION_PASSWORD
                } else {
                    txtInput.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                }

                txtInput.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                if (isNumber) {
                    txtInput.inputType = TYPE_CLASS_NUMBER
                } else {
                    txtInput.inputType = TYPE_CLASS_TEXT
                }
            }

            dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                val text = txtInput.text.toString()
                continuation.resume(text)
                dialog.dismiss()
            }

            dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                continuation.resume("")
                dialog.dismiss()
            }

            dialog.setOnCancelListener {
                continuation.resume("")
            }

            continuation.invokeOnCancellation {
                dialog.dismiss()
            }

            dialog.show()
        }

        suspend fun newFolder(context: Context): NewFolder =
            suspendCancellableCoroutine { continuation ->
                val dialog = createDialog(
                    context, R.layout.dialog_new_folder
                )

                dialog.setCancelable(true)
                startDialogAnimation(dialog.findViewById(R.id.main))

                val txtInput = dialog.findViewById<EditText>(R.id.txtInput)
                val spinnerFolder = dialog.findViewById<Spinner>(R.id.spinnerFolder)
                val items = arrayOf(
                    "Downloads",
                    "DCIM",
                    "Music",
                    "Alarms",
                    "Movies",
                    "Audio books",
                    "Documents",
                    "Notifications",
                    "Pictures",
                    "Podcasts",
                    "Recordings",
                    "Ringtones"
                )

                val adapter = ArrayAdapter(context, R.layout.spinner_folder, items)

                txtInput.hint = "My New Folder"
                txtInput.setText("")
                txtInput.inputType = TYPE_CLASS_TEXT

                adapter.setDropDownViewResource(R.layout.spinner_folder)
                spinnerFolder.adapter = adapter

                dialog.findViewById<TextView>(R.id.tvTitle).text = "New folder"
                dialog.findViewById<TextView>(R.id.tvMessage).text = "Select folder and name"
                dialog.findViewById<ImageView>(R.id.imgIcon).setImageResource(R.drawable.ic_add)

                dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                    val text = txtInput.text.toString()
                    continuation.resume(NewFolder(text, getFolderPathBySpinnerIndex(spinnerFolder)))
                    dialog.dismiss()
                }

                dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    continuation.resume(NewFolder("", ""))
                    dialog.dismiss()
                }

                dialog.setOnCancelListener {
                    continuation.resume(NewFolder("", ""))
                }

                continuation.invokeOnCancellation {
                    dialog.dismiss()
                }

                dialog.show()
            }
    }
}