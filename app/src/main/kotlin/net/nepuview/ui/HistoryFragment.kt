package net.nepuview.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.adapter.FilmListAdapter
import net.nepuview.data.Film
import net.nepuview.data.WatchHistory
import net.nepuview.databinding.FragmentHistoryBinding
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var repo: FilmRepository
    private lateinit var adapter: FilmListAdapter
    private var historyList: List<WatchHistory> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupClearAll()
        observeHistory()
    }

    private fun setupRecycler() {
        adapter = FilmListAdapter { film -> navigateToPlayer(film) }
        binding.recyclerView.adapter = adapter

        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val h = historyList.getOrNull(vh.bindingAdapterPosition) ?: return
                viewLifecycleOwner.lifecycleScope.launch { repo.deleteHistoryItem(h.id) }
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupClearAll() {
        binding.btnClearAll.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { repo.clearHistory() }
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.observeHistory().collect { history ->
                    historyList = history
                    binding.emptyText.isVisible = history.isEmpty()
                    binding.btnClearAll.isVisible = history.isNotEmpty()
                    adapter.submitList(history.map { h ->
                        Film(
                            id = h.filmId,
                            title = h.filmTitle,
                            posterUrl = h.posterUrl,
                            playerUrl = h.playerUrl
                        )
                    })
                }
            }
        }
    }

    private fun navigateToPlayer(film: Film) {
        findNavController().navigate(
            HistoryFragmentDirections.actionHistoryToPlayer(
                filmId = film.id,
                filmTitle = film.title,
                posterUrl = film.posterUrl,
                playerUrl = film.playerUrl
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
