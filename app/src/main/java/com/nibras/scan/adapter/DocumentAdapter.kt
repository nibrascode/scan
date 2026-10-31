package com.nibras.scan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nibras.scan.R
import com.nibras.scan.databinding.ItemDocumentBinding
import com.nibras.scan.model.Document
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentAdapter(
    private val onClick: (Document) -> Unit,
    private val onMoreClick: (Document, android.view.View) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    private val items = mutableListOf<Document>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun submitList(newItems: List<Document>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val doc = items[position]
        val context = holder.itemView.context
        holder.binding.tvDocName.text = doc.name
        holder.binding.tvDocMeta.text =
            context.getString(R.string.page_count_format, doc.pageCount) +
                " · " + dateFormat.format(doc.createdAt) +
                (doc.folder?.let { " · 📁 $it" } ?: "")
        val thumb = doc.thumb
        if (thumb != null && !thumb.isRecycled) {
            holder.binding.imgThumb.setImageBitmap(thumb)
        } else {
            holder.binding.imgThumb.setImageResource(R.drawable.ic_document)
        }
        holder.binding.root.setOnClickListener { onClick(doc) }
        holder.binding.btnMore.setOnClickListener { onMoreClick(doc, it) }
        holder.binding.btnMore.contentDescription = context.getString(R.string.share)
    }

    override fun getItemCount(): Int = items.size

    class DocumentViewHolder(val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root)
}
