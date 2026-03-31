package net.nepuview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.nepuview.data.Film
import net.nepuview.data.FilmCategory
import net.nepuview.databinding.ItemHomeCategoryBinding

class HomeCategoryAdapter(
    private val onClick: (Film) -> Unit
) : RecyclerView.Adapter<HomeCategoryAdapter.CategoryViewHolder>() {

    private var categories: List<FilmCategory> = emptyList()
    private val sharedPool = RecyclerView.RecycledViewPool()

    fun submitCategories(list: List<FilmCategory>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = categories.size
            override fun getNewListSize() = list.size
            override fun areItemsTheSame(o: Int, n: Int) = categories[o].label == list[n].label
            override fun areContentsTheSame(o: Int, n: Int) = categories[o] == list[n]
        })
        categories = list
        diff.dispatchUpdatesTo(this)
    }

    inner class CategoryViewHolder(private val binding: ItemHomeCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val carouselAdapter = FilmCarouselAdapter(onClick)

        init {
            binding.carouselRecycler.apply {
                layoutManager = LinearLayoutManager(
                    binding.root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = carouselAdapter
                setRecycledViewPool(sharedPool)
                isNestedScrollingEnabled = false
            }
        }

        fun bind(category: FilmCategory) {
            binding.categoryLabel.text = category.label
            carouselAdapter.submitList(category.films)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
}
