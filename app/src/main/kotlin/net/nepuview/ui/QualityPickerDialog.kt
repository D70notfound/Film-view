package net.nepuview.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.nepuview.viewmodel.DownloadViewModel

@AndroidEntryPoint
class QualityPickerDialog : DialogFragment() {

    private val downloadViewModel: DownloadViewModel by viewModels({ requireParentFragment() })

    companion object {
        private const val ARG_FILM_ID = "film_id"
        private const val ARG_TITLE = "title"
        private const val ARG_POSTER = "poster"
        private const val ARG_M3U8 = "m3u8"

        fun newInstance(filmId: String, filmTitle: String, posterUrl: String, m3u8Url: String) =
            QualityPickerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILM_ID, filmId)
                    putString(ARG_TITLE, filmTitle)
                    putString(ARG_POSTER, posterUrl)
                    putString(ARG_M3U8, m3u8Url)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val filmId = arguments?.getString(ARG_FILM_ID) ?: ""
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val poster = arguments?.getString(ARG_POSTER) ?: ""
        val m3u8 = arguments?.getString(ARG_M3U8) ?: ""

        val qualities = arrayOf("Auto (Beste)", "480p", "720p", "1080p")
        return AlertDialog.Builder(requireContext())
            .setTitle("Qualität wählen")
            .setItems(qualities) { _, which ->
                val quality = when (which) {
                    1 -> "480p"
                    2 -> "720p"
                    3 -> "1080p"
                    else -> "auto"
                }
                downloadViewModel.startDownload(filmId, title, poster, m3u8, quality)
                dismiss()
            }
            .setNegativeButton("Abbrechen", null)
            .create()
    }
}
