package net.nepuview.ui

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.nepuview.databinding.FragmentPlayerBinding
import net.nepuview.viewmodel.DownloadViewModel
import net.nepuview.viewmodel.PlayerViewModel

private const val POSITION_JS = """
(function() {
    var v = document.querySelector('video');
    if (v) {
        Android.onPosition(
            Math.round(v.currentTime * 1000),
            Math.round(v.duration * 1000)
        );
    } else {
        Android.onPosition(0, 0);
    }
})();
"""

@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val args: PlayerFragmentArgs by navArgs()
    private val viewModel: PlayerViewModel by activityViewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()

    private val progressHandler = Handler(Looper.getMainLooper())
    private val saveProgressRunnable = object : Runnable {
        override fun run() {
            // Inject JS to read video position; result arrives via PositionBridge.onPosition
            _binding?.webView?.evaluateJavascript(POSITION_JS, null)
            progressHandler.postDelayed(this, 5_000)
        }
    }

    private var currentPositionMs = 0L
    private var estimatedDurationMs = 0L

    private inner class PositionBridge {
        @JavascriptInterface
        fun onPosition(posMs: Long, durMs: Long) {
            if (posMs > 0 || durMs > 0) {
                currentPositionMs = posMs
                estimatedDurationMs = durMs
                viewModel.saveProgress(posMs, durMs)
            }
        }
    }

    // Whitelisted hosts — only these domains are allowed to load
    private val allowedHosts = setOf("nepu.to", "vr-m.net")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setCurrentFilm(args.filmId, args.filmTitle, args.posterUrl, args.playerUrl)
        setupWebView()
        observeM3u8()
        observeDownloadMessages()
        progressHandler.postDelayed(saveProgressRunnable, 5_000)
        viewModel.addToHistory()
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun setupWebView() {
        binding.webView.apply {
            addJavascriptInterface(PositionBridge(), "Android")
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // Hardened: no file/content access needed for streaming
                allowFileAccess = false
                allowContentAccess = false
                // Required for video autoplay inside the controlled WebView
                mediaPlaybackRequiresUserGesture = false
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    val uri = request.url ?: return null
                    val urlStr = uri.toString()
                    val host = uri.host?.lowercase() ?: return blockResponse()

                    // Block any non-whitelisted host
                    if (!isAllowedHost(host)) {
                        Log.d("PlayerFragment", "Blocked request to $host")
                        return blockResponse()
                    }

                    // Intercept M3U8 — validate host before passing to ViewModel
                    if (isM3u8Url(uri) && isAllowedHost(host)) {
                        Log.d("PlayerFragment", "M3U8 found: $urlStr")
                        requireActivity().runOnUiThread { viewModel.onM3u8Found(urlStr) }
                    }
                    return null
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url?.host?.lowercase() ?: return true
                    return !isAllowedHost(host)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        Log.e("PlayerFragment", "WebView error: ${error.description}")
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                // Block all popup windows
                override fun onCreateWindow(
                    view: WebView, isDialog: Boolean,
                    isUserGesture: Boolean, resultMsg: android.os.Message?
                ) = false
            }

            loadUrl(args.playerUrl)
        }
    }

    /** Returns true if the URI looks like an HLS manifest. */
    private fun isM3u8Url(uri: Uri): Boolean {
        val path = uri.path?.lowercase() ?: return false
        return path.endsWith(".m3u8") || path.contains(".m3u8?") ||
               uri.toString().contains("playlist.m3u8") ||
               uri.toString().contains("master.m3u8")
    }

    /** Returns true if host is within the allowed domain list (exact or subdomain). */
    private fun isAllowedHost(host: String): Boolean =
        allowedHosts.any { allowed -> host == allowed || host.endsWith(".$allowed") }

    private fun blockResponse() =
        WebResourceResponse("text/plain", "utf-8", null)

    private fun observeDownloadMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloadViewModel.userMessage.collect { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeM3u8() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.m3u8Url.collect { url ->
                    binding.btnDownload.isVisible = url != null
                    binding.btnDownload.setOnClickListener {
                        url ?: return@setOnClickListener
                        QualityPickerDialog.newInstance(
                            filmId = args.filmId,
                            filmTitle = args.filmTitle,
                            posterUrl = args.posterUrl,
                            m3u8Url = url
                        ).show(childFragmentManager, "quality")
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
                android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()
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
                android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (_binding == null) return
        if (isInPictureInPictureMode) {
            // Hide controls in PiP mode
            binding.btnDownload.visibility = View.GONE
            enterFullscreen()
        } else {
            binding.btnDownload.isVisible = viewModel.m3u8Url.value != null
            exitFullscreen()
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
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
