package com.github.thibaultbee.srtstreamer.app

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.thibaultbee.srtstreamer.app.ui.main.MainFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {
    private val activityDisposables = CompositeDisposable()
    private val rxPermissions = RxPermissions(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }

        rxPermissions
            .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .subscribe { granted: Boolean ->
                if (!granted) {
                    finishAndRemoveTask()
                }
            }.let(activityDisposables::add)
    }

    override fun onDestroy() {
        super.onDestroy()

        activityDisposables.clear()
    }
}
