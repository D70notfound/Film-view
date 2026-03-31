package net.nepuview.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.adapter.DownloadAdapter
import net.nepuview.databinding.FragmentDownloadsBinding
import net.nepuview.util.PermissionHelper
import net.nepuview.viewmodel.DownloadViewModel

@AndroidEntryPoint
class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadViewModel by viewModels()
    private lateinit var adapter: DownloadAdapter

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Benachrichtigungen deaktiviert — Download-Fortschritt wird nicht angezeigt",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PermissionHelper.requestNotificationPermission(this, notificationPermissionLauncher)
        setupRecycler()
        setupClearAll()
        observeState()
    }

    private fun setupRecycler() {
        adapter = DownloadAdapter { filmId -> viewModel.removeDownload(filmId) }
        binding.recyclerView.adapter = adapter
    }

    private fun setupClearAll() {
        binding.btnClearAll.setOnClickListener {
            viewModel.clearAll()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.downloads.collect { downloads ->
                        adapter.submitList(downloads)
                        binding.emptyText.isVisible = downloads.isEmpty()
                        binding.btnClearAll.isVisible = downloads.isNotEmpty()
                    }
                }
                launch {
                    viewModel.totalSize.collect { bytes ->
                        val mb = bytes / 1_048_576.0
                        binding.totalSizeText.text = "Gesamt: %.1f MB".format(mb)
                    }
                }
                launch {
                    viewModel.isLowStorage.collect { low ->
                        binding.storageWarning.isVisible = low
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
