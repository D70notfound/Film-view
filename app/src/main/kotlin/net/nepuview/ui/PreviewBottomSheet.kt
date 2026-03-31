package net.nepuview.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.nepuview.data.Film
import net.nepuview.databinding.BottomSheetPreviewBinding

class PreviewBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPreviewBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_FILM_ID = "film_id"
        private const val ARG_TITLE = "title"
        private const val ARG_POSTER = "poster"
        private const val ARG_DETAIL_URL = "detail_url"

        fun newInstance(film: Film) = PreviewBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_FILM_ID, film.id)
                putString(ARG_TITLE, film.title)
                putString(ARG_POSTER, film.posterUrl)
                putString(ARG_DETAIL_URL, film.detailUrl)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val filmId = arguments?.getString(ARG_FILM_ID) ?: return
        val title = arguments?.getString(ARG_TITLE) ?: return
        val poster = arguments?.getString(ARG_POSTER) ?: ""
        val detailUrl = arguments?.getString(ARG_DETAIL_URL) ?: return

        binding.filmTitle.text = title
        Glide.with(this).load(poster).into(binding.posterImage)

        binding.btnDetails.setOnClickListener {
            dismiss()
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToDetail(
                    filmId = filmId,
                    filmTitle = title,
                    posterUrl = poster,
                    detailUrl = detailUrl
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
