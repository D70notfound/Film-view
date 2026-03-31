package net.nepuview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.nepuview.data.Film
import net.nepuview.databinding.ItemFilmListBinding

class FilmListAdapter(
    private val onClick: (Film) -> Unit
) : ListAdapter<Film, FilmListAdapter.FilmViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Film>() {
            override fun areItemsTheSame(a: Film, b: Film) = a.id == b.id
            override fun areContentsTheSame(a: Film, b: Film) = a == b
        }
    }

    inner class FilmViewHolder(private val binding: ItemFilmListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(film: Film) {
            binding.filmTitle.text = film.title
            binding.filmYear.text = film.year
            binding.filmRating.text = if (film.rating > 0) "★ %.1f".format(film.rating) else ""
            Glide.with(binding.root)
                .load(film.posterUrl)
                .placeholder(android.R.color.darker_gray)
                .into(binding.posterImage)
            binding.root.setOnClickListener { onClick(film) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilmViewHolder {
        val binding = ItemFilmListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
