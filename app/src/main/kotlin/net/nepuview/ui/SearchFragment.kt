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
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.adapter.FilmListAdapter
import net.nepuview.data.Film
import net.nepuview.databinding.FragmentSearchBinding
import net.nepuview.viewmodel.SearchViewModel

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: FilmListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchBar()
        setupRecycler()
        observeState()
    }

    private fun setupSearchBar() {
        binding.searchBar.setOnEditorActionListener { _, _, _ ->
            viewModel.search(binding.searchBar.text.toString())
            true
        }
        binding.searchBar.setOnFocusChangeListener { _, hasFocus ->
            binding.historyGroup.isVisible = hasFocus && viewModel.history.value.isNotEmpty()
        }
    }

    private fun setupRecycler() {
        adapter = FilmListAdapter { film -> navigateToDetail(film) }
        binding.recyclerView.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.results.collect { films ->
                        adapter.submitList(films)
                        binding.emptyText.isVisible = films.isEmpty() && viewModel.query.value.isNotBlank()
                    }
                }
                launch {
                    viewModel.isLoading.collect { binding.progress.isVisible = it }
                }
                launch {
                    viewModel.history.collect { history ->
                        binding.historyGroup.removeAllViews()
                        history.forEach { term ->
                            val chip = Chip(requireContext()).apply {
                                text = term
                                isCloseIconVisible = true
                                setOnClickListener { viewModel.search(term) }
                                setOnCloseIconClickListener { viewModel.removeHistory(term) }
                            }
                            binding.historyGroup.addView(chip)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDetail(film: Film) {
        findNavController().navigate(
            SearchFragmentDirections.actionSearchToDetail(
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
