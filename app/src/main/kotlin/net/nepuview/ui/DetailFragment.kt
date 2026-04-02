package net.nepuview.ui

import android.graphics.drawable.GradientDrawable
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
import androidx.navigation.fragment.navArgs
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.data.FavoriteFilm
import net.nepuview.data.Film
import net.nepuview.databinding.FragmentDetailBinding
import net.nepuview.viewmodel.DetailViewModel

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: DetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupClickListeners()
        loadBasicInfo()
        // Kick off scraping once; ViewModel caches the result so returning
        // from the player never triggers a second network request.
        viewModel.loadDetail(args.detailUrl)
        observeFilm()
        observeFavoriteState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupClickListeners() {
        binding.btnWatch.setOnClickListener {
            viewModel.film.value?.let { navigateToPlayer(it) }
        }
        binding.btnFavorite.setOnClickListener {
            viewModel.film.value?.let { toggleFavorite(it) }
        }
    }

    private fun loadBasicInfo() {
        binding.filmTitle.text = args.filmTitle
        if (args.posterUrl.isNotBlank()) {
            Glide.with(this)
                .asBitmap()
                .load(args.posterUrl)
                .transform(RoundedCorners(12))
                .into(binding.posterImage)

            Glide.with(this)
                .asBitmap()
                .load(args.posterUrl)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    override fun onResourceReady(resource: android.graphics.Bitmap, t: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                        Palette.from(resource).generate { palette ->
                            if (_binding == null) return@generate
                            palette?.getDominantColor(0)?.let { color ->
                                val gradient = GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    intArrayOf(color and 0xAAFFFFFF.toInt(), 0xFF040608.toInt())
                                )
                                binding.posterGradient.background = gradient
                            }
                        }
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }
    }

    private fun observeFilm() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.film.collect { film ->
                    film ?: return@collect
                    binding.filmDescription.text = film.description
                    binding.filmYear.text = film.year
                    binding.filmDuration.text = film.duration
                    binding.filmRating.text = "%.1f".format(film.rating)
                    binding.filmGenres.text = film.genre.joinToString(" · ")
                    binding.btnWatch.isEnabled = film.playerUrl.isNotBlank()
                    binding.progressBanner.isVisible = false
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progress.collect { progress ->
                    if (progress != null && progress.progressPercent > 0) {
                        binding.progressBanner.isVisible = true
                        binding.progressBar.progress = progress.progressPercent
                        binding.progressText.text = "${progress.progressPercent}% gesehen"
                    }
                }
            }
        }
    }

    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite(args.filmId).collect { isFav ->
                    binding.btnFavorite.isSelected = isFav
                }
            }
        }
    }

    private fun navigateToPlayer(film: Film) {
        if (film.playerUrl.isBlank()) return
        findNavController().navigate(
            DetailFragmentDirections.actionDetailToPlayer(
                filmId = film.id,
                filmTitle = film.title,
                posterUrl = film.posterUrl,
                playerUrl = film.playerUrl
            )
        )
    }

    private fun toggleFavorite(film: Film) {
        viewModel.toggleFavorite(
            FavoriteFilm(
                filmId = film.id,
                filmTitle = film.title,
                posterUrl = film.posterUrl,
                detailUrl = film.detailUrl,
                rating = film.rating
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
