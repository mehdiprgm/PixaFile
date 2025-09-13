package org.zendev.pixafile.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import org.zendev.pixafile.R
import org.zendev.pixafile.databinding.FileLayoutBinding
import org.zendev.pixafile.filesystem.ItemType
import org.zendev.pixafile.filesystem.models.ZFile
import org.zendev.pixafile.tools.getAllViews
import org.zendev.pixafile.tools.selectedItems

class FileAdapter(private val context: Context) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private var showCheckBoxes = false

    var files = emptyList<ZFile>()
        @SuppressLint("NotifyDataSetChanged") set(value) {
            field = value
            notifyDataSetChanged()
        }

    interface OnItemClickListener {
        fun onItemClick(checkBox: MaterialCheckBox, file: ZFile)
        fun onItemLongClick(checkBox: MaterialCheckBox, file: ZFile)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): FileViewHolder {
        val binding = FileLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FileViewHolder, position: Int
    ) {
        val file = files[position]
        val b = holder.binding

        val popInAnim = AnimationUtils.loadAnimation(context, R.anim.pop_in)
        popInAnim.duration = 10

        getAllViews(b.layFile, true).forEach {
            popInAnim.duration += 2
            it.animation = popInAnim
        }

        if (file.isFolder) {
            b.imgIcon.setImageResource(R.drawable.ic_folder2)
        } else {
            when (file.type) {
                ItemType.Image -> {
                    b.imgIcon.setImageResource(R.drawable.ic_picture)
                }

                ItemType.Video -> {
                    b.imgIcon.setImageResource(R.drawable.ic_youtube)
                }

                ItemType.Audio -> {
                    b.imgIcon.setImageResource(R.drawable.ic_music)
                }

                else -> {
                    b.imgIcon.setImageResource(R.drawable.ic_file)
                }
            }
        }

        b.tvName.text = file.name
        b.tvInformation.text = "${file.size}    ${file.createDate}"

        b.chkSelected.isVisible = showCheckBoxes
        b.chkSelected.isChecked = selectedItems.containsKey(file)

        b.layFile.setOnClickListener {
            itemClickListener?.onItemClick(b.chkSelected, file)
        }

        b.layFile.setOnLongClickListener {
            itemClickListener?.onItemLongClick(b.chkSelected, file)
            true
        }
    }

    override fun getItemCount(): Int {
        return files.size;
    }

    class FileViewHolder(val binding: FileLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    fun setShowCheckboxes(show: Boolean) {
        showCheckBoxes = show
        notifyDataSetChanged()
    }
}