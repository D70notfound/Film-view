package net.nepuview.ui

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.*
import android.webkit.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.databinding.FragmentPlayerBinding
import net.nepuview.viewmodel.PlayerViewModel

@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val args: PlayerFragmentArgs by navArgs()
    private val viewModel: PlayerViewModel by activityViewModels()

    private val progressHandler = Handler(Looper.getMainLooper())
    private val saveProgressRunnable = object : Runnable {
        override fun run() {
            viewModel.saveProgress(currentPositionMs, estimatedDurationMs)
            progressHandler.postDelayed(this, 5_000)
        }
    }

    private var currentPositionMs = 0L
    private var estimatedDurationMs = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setCurrentFilm(args.filmId, args.filmTitle, args.posterUrl, args.playerUrl)
        setupWebView()
        observeM3u8()
        progressHandler.postDelayed(saveProgressRunnable, 5_000)
        viewModel.addToHistory()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url.toString()
                    if (url.contains(".m3u8") || url.contains("m3u8")) {
                        requireActivity().runOnUiThread { viewModel.onM3u8Found(url) }
                    }
                    val host = request.url.host?.lowercase() ?: return null
                    val allowed = setOf("nepu.to", "vr-m.net")
                    if (!allowed.any { host == it || host.endsWith(".$it") }) {
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                    return null
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val host = request.url.host?.lowercase() ?: return true
                    val allowed = setOf("nepu.to", "vr-m.net")
                    return !allowed.any { host == it || host.endsWith(".$it") }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                    return false // block popups
                }
            }

            loadUrl(args.playerUrl)
        }
    }

    private fun observeM3u8() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.m3u8Url.collect { url ->
                    binding.btnDownload.isVisible = url != null
                    binding.btnDownload.setOnClickListener {
                        url ?: return@setOnClickListener
                        val dialog = QualityPickerDialog.newInstance(
                            filmId = args.filmId,
                            filmTitle = args.filmTitle,
                            posterUrl = args.posterUrl,
                            m3u8Url = url
                        )
                        dialog.show(childFragmentManager, "quality")
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullscreen()
        } else {
            exitFullscreen()
        }
    }

    private fun enterFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.hide(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun exitFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.show(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            requireActivity().enterPictureInPictureMode(params)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroyView() {
        progressHandler.removeCallbacks(saveProgressRunnable)
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
