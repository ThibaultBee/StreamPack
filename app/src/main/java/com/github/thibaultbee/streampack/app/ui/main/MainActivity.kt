package com.github.thibaultbee.streampack.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.github.thibaultbee.streampack.app.R
import com.github.thibaultbee.streampack.app.databinding.MainActivityBinding
import com.github.thibaultbee.streampack.app.ui.settings.SettingsActivity
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.itemClicks
import io.reactivex.rxjava3.disposables.CompositeDisposable


class MainActivity : AppCompatActivity() {
    private val activityDisposables = CompositeDisposable()
    private lateinit var binding: MainActivityBinding
    private val tag = this::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, PreviewFragment())
                .commitNow()
        }

        bindProperties()
    }

    private fun bindProperties() {
        binding.actions.clicks()
            .subscribe {
                showPopup()
            }
            .let(activityDisposables::add)
    }

    private fun showPopup() {
        val popup = PopupMenu(this, binding.actions)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.actions, popup.menu)
        popup.show()
        popup.itemClicks().subscribe {
            if (it.itemId == R.id.action_settings) {
                goToSettingsActivity()
            } else {
                Log.e(tag, "Unknown menu item ${it.itemId}")
            }
        }
    }

    private fun goToSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions, menu)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        activityDisposables.clear()
    }
}
