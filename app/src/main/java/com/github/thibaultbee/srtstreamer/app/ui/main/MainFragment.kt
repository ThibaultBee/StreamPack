package com.github.thibaultbee.srtstreamer.app.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import butterknife.BindView
import butterknife.ButterKnife
import com.github.thibaultbee.srtstreamer.app.R
import com.github.thibaultbee.srtstreamer.app.utils.AlertUtils
import com.github.thibaultbee.srtstreamer.app.utils.PreviewUtils.Companion.chooseBigEnoughSize
import com.github.thibaultbee.srtstreamer.listeners.OnConnectionListener
import com.github.thibaultbee.srtstreamer.listeners.OnErrorListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class MainFragment : Fragment() {
    private val fragmentDisposables = CompositeDisposable()

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    @BindView(R.id.surfaceView)
    lateinit var surfaceView: SurfaceView

    @BindView(R.id.liveButton)
    lateinit var liveButton: ToggleButton

    @BindView(R.id.switchButton)
    lateinit var switchButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)
        ButterKnife.bind(this, view)
        bindProperties()
        return view
    }

    private fun bindProperties() {
        liveButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (liveButton.isChecked) {
                    try {
                        viewModel.endpoint.connect("192.168.1.27", 9998)
                        viewModel.streamer.startStream()
                    } catch (e: Exception) {
                        viewModel.endpoint.disconnect()
                        liveButton.isChecked = false
                    }
                } else {
                    viewModel.streamer.stopStream()
                    viewModel.endpoint.disconnect()
                }
            }
            .let(fragmentDisposables::add)

        switchButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(3000, TimeUnit.MILLISECONDS)
            .subscribe {
                if (viewModel.streamer.videoSource.cameraId == "0") {
                    viewModel.streamer.changeVideoSource("1")
                } else {
                    viewModel.streamer.changeVideoSource("0")
                }
            }
            .let(fragmentDisposables::add)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.streamer.context = context
        viewModel.streamer.configure(viewModel.audioConfig)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.streamer.configure(viewModel.videoConfig)
        } else {
            AlertUtils.show(context, "Error", "Permission not granted")
        }

        viewModel.streamer.onErrorListener = object : OnErrorListener {
            override fun onError(name: String, type: Error) {
                AlertUtils.show(context, "Error", "$type on $name")
            }
        }

        viewModel.streamer.onConnectionListener = object : OnConnectionListener{
            override fun onLost() {
                AlertUtils.show(context, "Connection Lost")
                liveButton.isChecked = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        surfaceView.holder.addCallback(surfaceViewCallback)
    }

    override fun onPause() {
        super.onPause()
        surfaceView.holder.setFixedSize(0, 0) // Ensure to trigger surface holder callback on resume
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.streamer.release()
        fragmentDisposables.clear()
    }

    @SuppressLint("MissingPermission")
    private val surfaceViewCallback = object: SurfaceHolder.Callback {
        var nbOnSurfaceChange = 0

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            if (holder != null) {
                nbOnSurfaceChange++
                if (nbOnSurfaceChange == 2) {
                    viewModel.streamer.startCapture(holder.surface)
                } else {
                    val choices = if (!viewModel.streamer.videoSource.isRunning()) {
                        viewModel.streamer.videoSource.getOutputSizes(SurfaceHolder::class.java, "0")
                    } else {
                        viewModel.streamer.videoSource.getOutputSizes(SurfaceHolder::class.java)
                    }
                    val size = chooseBigEnoughSize(choices, width, height)
                    if (size != null) {
                        holder.setFixedSize(size.width, size.height)
                    }
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            viewModel.streamer.stopCapture()
            surfaceView.holder.removeCallback(this)
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            nbOnSurfaceChange = 0
        }
    }
}
