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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.R
import net.nepuview.adapter.FilmPagerAdapter
import net.nepuview.data.Film
import net.nepuview.databinding.FragmentHomeBinding
import net.nepuview.viewmodel.HomeUiState
import net.nepuview.viewmodel.HomeViewModel

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val moods = listOf("Alle", "Action", "Chill", "Thriller", "Comedy", "Drama", "Anime")
    private lateinit var pagerAdapter: FilmPagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMoodChips()
        setupViewPager()
        observeState()
    }

    private fun setupMoodChips() {
        moods.forEach { mood ->
            val chip = Chip(requireContext()).apply {
                text = mood
                isCheckable = true
                setOnClickListener { viewModel.selectMood(if (mood == "Alle") null else mood) }
            }
            binding.moodChipGroup.addView(chip)
        }
        (binding.moodChipGroup.getChildAt(0) as? Chip)?.isChecked = true
    }

    private fun setupViewPager() {
        pagerAdapter = FilmPagerAdapter { film -> navigateToDetail(film) }
        binding.viewPager.apply {
            adapter = pagerAdapter
            orientation = ViewPager2.ORIENTATION_VERTICAL
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.shimmerLayout.isVisible = state is HomeUiState.Loading
                    binding.viewPager.isVisible = state is HomeUiState.Success
                    binding.errorText.isVisible = state is HomeUiState.Error
                    if (state is HomeUiState.Success) pagerAdapter.submitList(state.films)
                    if (state is HomeUiState.Error) binding.errorText.text = state.message
                }
            }
        }
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
