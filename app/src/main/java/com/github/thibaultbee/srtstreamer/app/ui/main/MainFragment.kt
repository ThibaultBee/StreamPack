package com.github.thibaultbee.srtstreamer.app.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ToggleButton
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
import com.github.thibaultbee.srtstreamer.utils.getOutputSizes
import com.jakewharton.rxbinding4.view.clicks
import com.tbruyelle.rxpermissions3.RxPermissions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit


class MainFragment : Fragment() {
    private val fragmentDisposables = CompositeDisposable()

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private val rxPermissions: RxPermissions by lazy { RxPermissions(this) }

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

    @SuppressLint("MissingPermission")
    private fun bindProperties() {
        liveButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .compose(
                rxPermissions.ensure(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
            .subscribe { granted ->
                if (!granted) {
                    context?.let { AlertUtils.show(it, "Error", "Permission not granted") }
                } else {
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
            }
            .let(fragmentDisposables::add)

        switchButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(3000, TimeUnit.MILLISECONDS)
            .compose(rxPermissions.ensure(Manifest.permission.CAMERA))
            .map { viewModel.streamer.videoSource }
            .subscribe { camera ->
                camera?.let {
                    if (it.cameraId == "0") {
                        viewModel.streamer.changeVideoSource("1")
                    } else {
                        viewModel.streamer.changeVideoSource("0")
                    }
                }
            }
            .let(fragmentDisposables::add)
    }

    @SuppressLint("MissingPermission")
    override fun onAttach(context: Context) {
        super.onAttach(context)

        rxPermissions
            .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .subscribe { granted: Boolean ->
                if (!granted) {
                    context.let { AlertUtils.show(it, "Error", "Permission not granted") }
                }
            }.let(fragmentDisposables::add)

        viewModel.buildStreamer(context)
        viewModel.streamer.configure(viewModel.audioConfig)
        viewModel.streamer.configure(viewModel.videoConfig)

        viewModel.streamer.onErrorListener = object : OnErrorListener {
            override fun onError(name: String, type: Error) {
                AlertUtils.show(context, "Error", "$type on $name")
            }
        }

        viewModel.streamer.onConnectionListener = object : OnConnectionListener {
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
    private val surfaceViewCallback = object : SurfaceHolder.Callback {
        var nbOnSurfaceChange = 0

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            require(context != null)

            holder?.let {
                nbOnSurfaceChange++
                if (nbOnSurfaceChange == 2) {
                    viewModel.streamer.startCapture(holder.surface)
                } else {
                    val choices = viewModel.streamer.videoSource?.let {
                        context!!.getOutputSizes(
                            SurfaceHolder::class.java,
                            viewModel.streamer.videoSource!!.cameraId
                        )
                    } ?: context!!.getOutputSizes(SurfaceHolder::class.java, "0")

                    chooseBigEnoughSize(choices, width, height)?.let { size ->
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
