package com.github.thibaultbee.streampack.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.thibaultbee.streampack.app.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment())
                    .commitNow()
        }
    }
}
