package com.github.thibaultbee.streampack.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.thibaultbee.streampack.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }
}