package net.nepuview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.nepuview.data.Film
import net.nepuview.databinding.ItemFilmPageBinding

class FilmPagerAdapter(
    private val onClick: (Film) -> Unit
) : ListAdapter<Film, FilmPagerAdapter.FilmViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Film>() {
            override fun areItemsTheSame(a: Film, b: Film) = a.id == b.id
            override fun areContentsTheSame(a: Film, b: Film) = a == b
        }
    }

    inner class FilmViewHolder(private val binding: ItemFilmPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(film: Film) {
            binding.filmTitle.text = film.title
            binding.filmYear.text = film.year
            binding.filmRating.text = if (film.rating > 0) "★ %.1f".format(film.rating) else ""
            Glide.with(binding.root)
                .load(film.posterUrl)
                .centerCrop()
                .into(binding.posterImage)
            binding.root.setOnClickListener { onClick(film) }
            binding.btnFavorite.setOnClickListener {
                animateHeart(binding.btnFavorite)
            }
        }

        private fun animateHeart(view: android.view.View) {
            view.animate()
                .scaleX(1.4f).scaleY(1.4f).setDuration(150)
                .withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilmViewHolder {
        val binding = ItemFilmPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: FilmViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView).clear(holder.itemView)
    }
}
