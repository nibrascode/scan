package com.nibras.scan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nibras.scan.databinding.ItemPageFullBinding
import com.nibras.scan.model.Page
import com.nibras.scan.util.PreviewCache
import com.nibras.scan.util.RenderJobs

class PagePagerAdapter(private val pages: MutableList<Page>) :
    RecyclerView.Adapter<PagePagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageFullBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        val request = ++holder.requestId
        val path = page.originalFile.absolutePath
        val rotation = page.rotationDegrees
        val filter = page.filter
        holder.binding.imgPageFull.setImageDrawable(null)
        RenderJobs.executor.execute {
            val bmp = PreviewCache.render(path, rotation, filter, 1100)
            holder.itemView.post {
                if (holder.requestId != request) return@post
                if (bmp != null) holder.binding.imgPageFull.setImageBitmap(bmp)
            }
        }
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        holder.requestId++
        holder.binding.imgPageFull.setImageDrawable(null)
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(val binding: ItemPageFullBinding) : RecyclerView.ViewHolder(binding.root) {
        var requestId: Int = 0
    }
}
