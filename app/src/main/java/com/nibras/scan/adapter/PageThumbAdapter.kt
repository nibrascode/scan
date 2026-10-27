package com.nibras.scan.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nibras.scan.databinding.ItemPageThumbBinding
import com.nibras.scan.model.Page
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.PreviewCache
import com.nibras.scan.util.RenderJobs

class PageThumbAdapter(
    private val pages: MutableList<Page>,
    private val onThumbClick: (Int) -> Unit
) : RecyclerView.Adapter<PageThumbAdapter.ThumbViewHolder>() {

    var selectedPosition = 0
        set(value) {
            val old = field
            field = value
            if (old != value) {
                if (old in 0 until itemCount) notifyItemChanged(old)
                if (value in 0 until itemCount) notifyItemChanged(value)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val binding = ItemPageThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        val page = pages[position]
        val request = ++holder.requestId
        val path = page.originalFile.absolutePath
        val rotation = page.rotationDegrees
        val filter = page.filter
        holder.binding.imgThumbPage.setImageDrawable(null)
        holder.binding.viewThumbSelected.visibility =
            if (position == selectedPosition) View.VISIBLE else View.INVISIBLE
        holder.binding.root.setOnClickListener { onThumbClick(holder.bindingAdapterPosition) }
        RenderJobs.executor.execute {
            val bmp = PreviewCache.render(path, rotation, filter, 280)
            holder.itemView.post {
                if (holder.requestId != request) return@post
                if (bmp != null) holder.binding.imgThumbPage.setImageBitmap(bmp)
            }
        }
    }

    override fun onViewRecycled(holder: ThumbViewHolder) {
        holder.requestId++
        holder.binding.imgThumbPage.setImageDrawable(null)
    }

    override fun getItemCount(): Int = pages.size

    fun moveItem(from: Int, to: Int) {
        if (from !in pages.indices || to !in pages.indices) return
        val page = pages.removeAt(from)
        pages.add(to, page)
        notifyItemMoved(from, to)
        ScanSession.touch()
    }

    class ThumbViewHolder(val binding: ItemPageThumbBinding) : RecyclerView.ViewHolder(binding.root) {
        var requestId: Int = 0
    }
}
