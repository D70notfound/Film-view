package net.nepuview.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.adapter.FilmListAdapter
import net.nepuview.data.FavoriteFilm
import net.nepuview.data.Film
import net.nepuview.data.WatchlistFilm
import net.nepuview.databinding.FragmentFavoritesBinding
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var repo: FilmRepository
    private lateinit var adapter: FilmListAdapter
    private var currentFavorites: List<FavoriteFilm> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        observeFavorites()
    }

    private fun setupRecycler() {
        adapter = FilmListAdapter { film -> navigateToDetail(film) }
        binding.recyclerView.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                val fav = currentFavorites.getOrNull(pos) ?: return
                viewLifecycleOwner.lifecycleScope.launch {
                    if (direction == ItemTouchHelper.RIGHT) {
                        repo.addToWatchlist(
                            WatchlistFilm(
                                filmId = fav.filmId,
                                filmTitle = fav.filmTitle,
                                posterUrl = fav.posterUrl,
                                detailUrl = fav.detailUrl
                            )
                        )
                    } else {
                        repo.removeFavorite(fav.filmId)
                    }
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.observeFavorites().collect { favorites ->
                    currentFavorites = favorites
                    binding.emptyText.isVisible = favorites.isEmpty()
                    adapter.submitList(favorites.map { fav ->
                        Film(
                            id = fav.filmId,
                            title = fav.filmTitle,
                            posterUrl = fav.posterUrl,
                            detailUrl = fav.detailUrl,
                            rating = fav.rating
                        )
                    })
                }
            }
        }
    }

    private fun navigateToDetail(film: Film) {
        findNavController().navigate(
            FavoritesFragmentDirections.actionFavoritesToDetail(
                filmId = film.id,
                filmTitle = film.title,
                posterUrl = film.posterUrl,
                detailUrl = film.detailUrl
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
