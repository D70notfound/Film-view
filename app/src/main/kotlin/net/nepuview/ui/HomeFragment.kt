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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.adapter.HomeCategoryAdapter
import net.nepuview.data.Film
import net.nepuview.databinding.FragmentHomeBinding
import net.nepuview.viewmodel.HomeUiState
import net.nepuview.viewmodel.HomeViewModel

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var categoryAdapter: HomeCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryList()
        observeState()
    }

    private fun setupCategoryList() {
        categoryAdapter = HomeCategoryAdapter { film -> navigateToDetail(film) }
        binding.recyclerCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.shimmerLayout.isVisible = state is HomeUiState.Loading
                    binding.contentScroll.isVisible = state is HomeUiState.Success
                    binding.errorText.isVisible = state is HomeUiState.Error

                    when (state) {
                        is HomeUiState.Success -> bindSuccess(state)
                        is HomeUiState.Error -> binding.errorText.text = state.message
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun bindSuccess(state: HomeUiState.Success) {
        val hero = state.heroFilm

        // Hero banner
        Glide.with(this)
            .load(hero.posterUrl)
            .centerCrop()
            .into(binding.posterHero)

        binding.heroTitle.text = hero.title
        binding.heroGenre.text = hero.genre.take(3).joinToString(" · ")
            .ifBlank { hero.type.name.lowercase().replaceFirstChar { it.uppercaseChar() } }

        binding.btnPlay.setOnClickListener { navigateToDetail(hero) }
        binding.btnMyList.setOnClickListener { viewModel.addToWatchlist(hero) }

        // Long-press on hero → preview sheet
        binding.heroContainer.setOnLongClickListener {
            PreviewBottomSheet.newInstance(hero)
                .show(childFragmentManager, "preview")
            true
        }

        // Carousels
        categoryAdapter.submitCategories(state.categories)
    }

    private fun navigateToDetail(film: Film) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToDetail(
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
