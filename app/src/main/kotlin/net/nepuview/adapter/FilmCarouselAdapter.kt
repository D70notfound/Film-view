package net.nepuview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.nepuview.data.Film
import net.nepuview.databinding.ItemFilmCardBinding

class FilmCarouselAdapter(
    private val onClick: (Film) -> Unit
) : ListAdapter<Film, FilmCarouselAdapter.CardViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Film>() {
            override fun areItemsTheSame(a: Film, b: Film) = a.id == b.id
            override fun areContentsTheSame(a: Film, b: Film) = a == b
        }
    }

    inner class CardViewHolder(private val binding: ItemFilmCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(film: Film) {
            binding.cardTitle.text = film.title
            Glide.with(binding.root)
                .load(film.posterUrl)
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(binding.cardPoster)
            binding.root.setOnClickListener { onClick(film) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemFilmCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: CardViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView).clear(holder.itemView)
    }
}
