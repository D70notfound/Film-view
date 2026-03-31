package net.nepuview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.nepuview.data.DownloadedFilm
import net.nepuview.databinding.ItemDownloadBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadAdapter(
    private val onDelete: (String) -> Unit
) : ListAdapter<DownloadedFilm, DownloadAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DownloadedFilm>() {
            override fun areItemsTheSame(a: DownloadedFilm, b: DownloadedFilm) = a.filmId == b.filmId
            override fun areContentsTheSame(a: DownloadedFilm, b: DownloadedFilm) = a == b
        }
        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadedFilm) {
            binding.filmTitle.text = item.filmTitle
            binding.filmQuality.text = item.quality
            binding.filmSize.text = "%.1f MB".format(item.fileSizeBytes / 1_048_576.0)
            binding.filmDate.text = DATE_FORMAT.format(Date(item.downloadedAt))
            Glide.with(binding.root).load(item.posterUrl).into(binding.posterImage)
            binding.btnDelete.setOnClickListener { onDelete(item.filmId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
