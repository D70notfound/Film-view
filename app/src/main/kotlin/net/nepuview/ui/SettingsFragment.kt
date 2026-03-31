package net.nepuview.ui

import android.os.Bundle
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import net.nepuview.R
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var filmRepo: FilmRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("download_quality")?.apply {
            entries = arrayOf("Auto", "480p", "720p", "1080p")
            entryValues = arrayOf("auto", "480p", "720p", "1080p")
            value = value ?: "720p"
        }

        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            requireContext().cacheDir.deleteRecursively()
            Toast.makeText(requireContext(), "Cache geleert", Toast.LENGTH_SHORT).show()
            true
        }

        findPreference<Preference>("app_version")?.summary =
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
    }
}
