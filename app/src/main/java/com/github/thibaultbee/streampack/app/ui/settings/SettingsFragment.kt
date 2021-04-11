package com.github.thibaultbee.streampack.app.ui.settings

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Size
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.github.thibaultbee.streampack.app.R
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.app.configuration.ConfigurationHelper

class SettingsFragment : PreferenceFragmentCompat() {
    private val configuration: Configuration by lazy {
        Configuration(requireActivity())
    }

    private val configHelper: ConfigurationHelper by lazy {
        ConfigurationHelper(requireContext())
    }

    private val resolutionListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.resolution_key))!!
    }

    private val serverIpPreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.server_ip_key))!!
    }

    private val serverPortPreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.server_port_key))!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
    }

    private fun loadPreferences() {
        configHelper.resolutionEntries.map { it.toString() }.toTypedArray().run {
            resolutionListPreference.entries = this
            resolutionListPreference.entryValues = this
        }
        resolutionListPreference.value = configuration.video.resolution.toString()

        serverIpPreference.text = configuration.connection.ip
        serverIpPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        serverPortPreference.text = configuration.connection.port.toString()
        serverPortPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.filters = arrayOf(InputFilter.LengthFilter(5))
        }
    }

    override fun onPause() {
        super.onPause()
        savePreferences()
    }

    private fun savePreferences() {
        val resolutionList = resolutionListPreference.value.split("x").map { it.toInt() }
        configuration.video.resolution = Size(resolutionList[0], resolutionList[1])

        configuration.connection.ip = serverIpPreference.text
        configuration.connection.port = serverPortPreference.text.toInt()
    }

}