# 🚀 Quick Start: Camera

## Dependencies

Add the StreamPack packages in dependency in your module `build.gradle` file:

```groovy
dependencies {
    // Replace $latest_version with the version on the Maven Central badge
    implementation 'io.github.thibaultbee.streampack:streampack-core:$latest_version'
    // For xml UI (incl. ViewfinderView)
    implementation 'io.github.thibaultbee.streampack:streampack-ui:$latest_version'
    // Or compose UI (incl. ViewfinderStreamerSource and ViewfinderStreamerOutput)
    implementation 'io.github.thibaultbee.streampack:streampack-compose:$latest_version'
    // RTMP or/and SRT package according to your requirements
}
```

## Permissions

Request the required permissions in your Activity/Fragment and `AndroidManifest.xml`.

**Base Permissions:**
You need to add the following permissions in your `AndroidManifest.xml`:

```xml

<manifest>
    <!-- Only for a live -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Only for a local network live (Android 37+) -->
    <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
    <!-- Only for a local record -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
</manifest>
```

!!! warning "Dangerous Permissions"
    To record locally, you also need to request the following dangerous permission: `android.permission.WRITE_EXTERNAL_STORAGE`.

!!! warning "Android 37+ Permissions"
    To stream on a local network, for Android 37 and later, you also need to request the following dangerous permission: `android.permission.ACCESS_LOCAL_NETWORK`.

**Camera Permissions:**
To use the camera, you need to request the following permission:

```xml

<manifest>
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
</manifest>
```

!!! warning "Dangerous Permissions"
    Your application also must request the following dangerous permissions at runtime: `android.permission.RECORD_AUDIO` and `android.permission.CAMERA`.

For the PlayStore, your application might declare this in its `AndroidManifest.xml`

```xml

<manifest>
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
</manifest>
```

## Streamer instantiation

A `Streamer` is a class that represents a whole streaming pipeline from capture to endpoint (
incl. encoding, muxing, sending).
Multiple streamers are available depending on the number of independent outputs you want to
have:

- `SingleStreamer`: for a single output (such as live or record)
- `DualStreamer`: for 2 independent outputs (such as independent live and record)
- for multiple outputs, you can use the `StreamerPipeline` class that allows to create more
  complex pipeline with multiple independent outputs (such as audio in one file, video in
  another file)

The `SingleStreamer` and the `DualStreamer` comes with factory for `Camera` and
`MediaProjection` (for screen capture).
Otherwise, you can set the audio and the video source manually.

```kotlin
/**
 * Most StreamPack components are coroutine based.
 * Suspend and flow have to be called from a coroutine scope.
 * Android comes with coroutine scopes like `lifecycleScope` or `viewModelScope`.
 * Call suspend functions from a coroutine scope:
 *  viewModelScope.launch {
 *  }
 */

val streamer = cameraSingleStreamer(context = requireContext())

/**
 * To have multiple independent outputs (like for live and record), use a `cameraDualStreamer` or even the `StreamerPipeline`.
 *
 * You can also create the `SingleStreamer`or the `DualStreamer` and add later the audio and video source with `setAudioSource` 
 * and `setVideoSource`.
 * val streamer = SingleStreamer(context = requireContext())
 * streamer.setVideoSource(CameraSourceFactory()) // Same as streamer.setCameraId(context.defaultCameraId)
 * streamer.setAudioSource(MicrophoneSourceFactory())
 */
```

For more information, check the [Streamers](../advanced/Streamers.md) documentation.

## Configuration

```kotlin
val streamer = cameraSingleStreamer(context = requireContext()) // Already instantiated streamer

// Creates a new audio and video config
val audioConfig = AudioConfig(
    startBitrate = 128000,
    sampleRate = 44100,
    channelConfig = AudioFormat.CHANNEL_IN_STEREO
)

val videoConfig = VideoConfig(
    startBitrate = 2000000, // 2 Mb/s
    resolution = Size(1280, 720),
    fps = 30
)

// Sets the audio and video config
viewModelScope.launch {
    streamer.setAudioConfig(audioConfig)
    streamer.setVideoConfig(videoConfig)
}
```

## Preview

StreamPack supports two types of previews:

- **Source Preview**: Directly displays the raw frames from the camera source with minimal latency.
- **Output Preview**: Displays the processed frames from the pipeline output, including all applied filters, effects, scaling, and positioning.

Both preview types automatically manage the surface lifecycle and support pinch-to-zoom and tap-to-focus gestures (when supported by the camera source).

=== "XML UI"

    StreamPack provides `ViewfinderView` in the `streampack-ui` package, backed by AndroidX `CameraViewfinder`. It supports aspect ratio scaling (`crop` or `fit`) and positioning (`start`, `center`, or `end`).

    Create a `ViewfinderView` to display the preview in your layout:

    ```xml
    <layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto">

        <io.github.thibaultbee.streampack.ui.views.ViewfinderView
            android:id="@+id/preview"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:scaleMode="crop"
            app:position="center" />
    </layout>
    ```

    - `app:scaleMode`: `crop` (fills view, cropping excess) or `fit` (fits inside view with letterbox/pillarbox).
    - `app:position`: `start` (top/left), `center`, or `end` (bottom/right).

    StreamPack provides extension functions on `ViewfinderView` to bind either the source or output preview.

    #### Source Preview

    ```kotlin
    val streamer = cameraSingleStreamer(context = requireContext())
    val preview = findViewById<ViewfinderView>(R.id.preview)

    // Binds ViewfinderView to the camera video source
    preview.bindToStreamerSource(
        lifecycleOwner = viewLifecycleOwner,
        streamer = streamer,
        isPinchToZoomEnabled = true,
        isTapToFocusEnabled = true
    )
    ```

    #### Output Preview

    ```kotlin
    val streamer = cameraSingleStreamer(context = requireContext())
    val preview = findViewById<ViewfinderView>(R.id.preview)

    // Binds ViewfinderView to the processed pipeline output
    preview.bindToStreamerOutput(
        lifecycleOwner = viewLifecycleOwner,
        streamer = streamer,
        isPinchToZoomEnabled = true,
        isTapToFocusEnabled = true
    )
    ```

    #### Scale Mode and Position

    You can change the scale mode and positioning programmatically at runtime:

    ```kotlin
    preview.scaleMode = ScaleMode.FIT // or ScaleMode.CROP
    preview.position = Position.CENTER // or Position.START, Position.END
    ```

    #### Unbinding

    To manually unbind the preview before its lifecycle ends:

    ```kotlin
    preview.unbind()
    ```

    #### Custom Views

    As a camera preview, you can also use a `SurfaceView`, a `TextureView`, or any custom view that can provide a `Surface`:

    ```kotlin
    streamer.startPreview(surface)
    ```

=== "Compose UI"

    StreamPack provides `ViewfinderStreamerSource` and `ViewfinderStreamerOutput` composables in `streampack-compose`.

    #### Source Preview

    ```kotlin
    val streamer = cameraSingleStreamer(context = LocalContext.current)

    Box(modifier = Modifier.fillMaxSize()) {
        ViewfinderStreamerSource(
            streamer = streamer,
            modifier = Modifier.fillMaxSize(),
            scaleMode = ScaleMode.CROP, // or ScaleMode.FIT
            position = Position.CENTER, // or Position.START, Position.END
            isPinchToZoomEnabled = true,
            isTapToFocusEnabled = true
        )
    }
    ```

    #### Output Preview

    ```kotlin
    val streamer = cameraSingleStreamer(context = LocalContext.current)

    Box(modifier = Modifier.fillMaxSize()) {
        ViewfinderStreamerOutput(
            streamer = streamer,
            modifier = Modifier.fillMaxSize(),
            scaleMode = ScaleMode.CROP, // or ScaleMode.FIT
            position = Position.CENTER, // or Position.START, Position.END
            isPinchToZoomEnabled = true,
            isTapToFocusEnabled = true
        )
    }
    ```

## Orientation

```kotlin
 // Already instantiated streamer
val streamer = cameraSingleStreamer(context = requireContext())

// Sets the device orientation
streamer.setTargetRotation(Surface.ROTATION_90) // Or Surface.ROTATION_0, Surface.ROTATION_180, Surface.ROTATION_270
```

StreamPack comes with 2 `RotationProvider` that fetches and listens the device rotation:

- the `SensorRotationProvider`. The `SensorRotationProvider` is backed by the
  `OrientationEventListener` and it follows the device orientation.
- the `DisplayRotationProvider`. The `DisplayRotationProvider` is backed by the `DisplayManager`
  and if orientation is locked, it will return the last known orientation.

```kotlin
val streamer = cameraSingleStreamer(context = requireContext()) // Already instantiated streamer
val rotationProvider = SensorRotationProvider(context = requireContext())

// Sets the device orientation
rotationProvider.addListener(object : IRotationProvider.Listener {
    override fun onOrientationChanged(rotation: Int) {
        streamer.setTargetRotation(rotation)
    }
})

// Don't forget to remove the listener when you don't need it anymore
rotationProvider.removeListener(listener)
```

You can transform the `RotationProvider` into a `Flow` through `rotationFlow()`.

```kotlin
val streamer = cameraSingleStreamer(context = requireContext()) // Already instantiated streamer
val rotationProvider = SensorRotationProvider(context = requireContext())

// Then in a coroutine suspend function
rotationProvider.rotationFlow().collect { rotation ->
    streamer.setTargetRotation(rotation)
}
```

You can also create your own `targetRotation` provider.

## Start the live streaming

```kotlin
 // Already instantiated streamer
val streamer = cameraSingleStreamer(context = requireContext())

val descriptor =
    UriMediaDescriptor("rtmps://serverip:1935/s/streamKey") // For RTMP/RTMPS. Uri also supports SRT url, file path, content path,...
/**
 * Alternatively, you can use object syntax:
 * - RtmpMediaDescriptor("rtmps", "serverip", 1935, "s", "streamKey") // For RTMP/RTMPS
 * - SrtMediaDescriptor("serverip", 1234) // For SRT
 */

streamer.startStream(descriptor)
// You can also use:
// streamer.startStream("rtmp://serverip:1935/s/streamKey") // For RTMP/RTMPS
```

## Stop and release the streamer

```kotlin
// Already instantiated streamer
val streamer = cameraSingleStreamer(context = requireContext())

streamer.stopStream()
streamer.close() // Disconnect from server or close the file
streamer.release()
```

For a complete example, check out
the [demos/camera](https://github.com/ThibaultBee/StreamPack/tree/main/demos/camera) directory.