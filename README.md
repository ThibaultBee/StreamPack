# StreamPack: RTMP and [SRT](https://github.com/Haivision/srt) live streaming SDK for Android

StreamPack is a flexible live streaming library for Android made for both demanding video
broadcasters and new video enthusiasts.

It is designed to be used in live streaming and gaming apps.

## Setup

Get StreamPack core latest artifacts on Maven Central:

```groovy
dependencies {
    implementation 'io.github.thibaultbee.streampack:streampack-core:3.0.0-RC3'
    // For UI (incl. PreviewView)
    implementation 'io.github.thibaultbee.streampack:streampack-ui:3.0.0-RC3'
    // For services (incl. screen capture/media projection service)
    implementation 'io.github.thibaultbee.streampack:streampack-services:3.0.0-RC3'
    // For RTMP
    implementation 'io.github.thibaultbee.streampack:streampack-rtmp:3.0.0-RC3'
    // For SRT
    implementation 'io.github.thibaultbee.streampack:streampack-srt:3.0.0-RC3'
}
```

## Features

* Video:
    * Source: Cameras, Screen recorder
      or [custom video source](docs/AdvancedStreamer.md#creates-your-custom-sources)
    * Orientation: portrait or landscape
    * Codec: HEVC/H.265, AVC/H.264, VP9 or AV1
    * HDR (experimental, see https://github.com/ThibaultBee/StreamPack/discussions/91)
    * Configurable bitrate, resolution, frame rate (tested up to 60), encoder level, encoder profile
    * Video only mode
    * Device video capabilities
    * Switch between video sources
    * Camera settings: auto-focus, exposure, white balance, zoom, flash,...
* Audio:
    * Source: Microphone, device audio
      or [custom audio source](docs/AdvancedStreamer.md#creates-your-custom-sources)
    * Codec: AAC:LC, HE, HEv2,... or Opus
    * Configurable bitrate, sample rate, stereo/mono, data format
    * Processing: Noise suppressor or echo cancellation
    * Audio only mode
    * Device audio capabilities
    * Switch between audio sources
* File: TS, FLV, MP4, WebM and Fragmented MP4
    * Write to a single file or multiple chunk files
* Streaming: RTMP/RTMPS or SRT
    * [Record to a file and stream at the same time](docs/LiveAndRecordSimultaneously.md)
    * Support for enhanced RTMP
    * Ultra low-latency based on [SRT](https://github.com/Haivision/srt)
    * Network adaptive bitrate mechanism for [SRT](https://github.com/Haivision/srt)

## Quick start

If you want to create a new application, you should use the
template [StreamPack boilerplate](https://github.com/ThibaultBee/StreamPack-boilerplate). In 5
minutes, you will be able to stream live video to your server.

## Getting started

### Getting started for a camera stream

1. Request the required permissions in your Activity/Fragment. See the
   [Permissions](#permissions) section for more information.

2. Creates a `View` to display the preview in your layout

   As a camera preview, you can also use a `SurfaceView`, a `TextureView` or any
   `View` where that can provide a `Surface`.

   To simplify integration, StreamPack provides an `PreviewView` in the `streampack-ui` package.

    ```xml
    
    <layout>
        <io.github.thibaultbee.streampack.views.PreviewView android:id="@+id/preview"
            android:layout_width="match_parent" android:layout_height="match_parent"
            app:enableZoomOnPinch="true" />
    </layout>
    ```

   `app:enableZoomOnPinch` is a boolean to enable zoom on pinch gesture.

3. Instantiates the streamer (main live streaming class)

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
    // To have multiple independent outputs (like for live and record), use a `cameraDualStreamer` or even the `StreamerPipeline`.
    // You can also use the `SingleStreamer`or the `DualStreamer` and add later the audio and video source with `setAudioSource` 
    // and `setVideoSource`.
    ```

   For more information, check the [Streamers](docs/Streamers.md) documentation.

4. Configures audio and video settings

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

5. Inflates the preview with the streamer

    ```kotlin
    val streamer = cameraSingleStreamer(context = requireContext()) // Already instantiated streamer
    val preview = findViewById<PreviewView>(R.id.preview) // Already inflated preview
    /**
     * If the preview is a `PreviewView`
     */
    preview.streamer = streamer
    // Then start the preview
    streamer.startPreview()
    
    /**
     * Otherwise if the preview is in a [SurfaceView], a [TextureView], a [Surface],... you can use:
     */
    streamer.startPreview(preview)
    ```

6. Sets the device orientation

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

   You can transform the `RotationProvider` into a `Flow` provider through the `asFlowProvider`.

   ```kotlin
    val streamer = cameraSingleStreamer(context = requireContext()) // Already instantiated streamer
    val rotationProvider = SensorRotationProvider(context = requireContext())
   
    // For coroutine based
    val rotationFlowProvider = rotationProvider.asFlowProvider()
    // Then in a coroutine suspend function
    rotationFlowProvider.rotationFlow.collect { rotation ->
    streamer.setTargetRotation(rotation)
    }
    ```

   You can also create your own `targetRotation` provider.

7. Starts the live streaming

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

8. Stops and releases the streamer

    ```kotlin
    // Already instantiated streamer
    val streamer = cameraSingleStreamer(context = requireContext())
    
    streamer.stopStream()
    streamer.close() // Disconnect from server or close the file
    streamer.release()
    ```

For more detailed explanation, check out
the [documentation](#documentations).

For a complete example, check out the [demos/camera](demos/camera) directory.

### Getting started for a screen recorder stream

1. Add the `streampack-services` dependency in your `build.gradle` file:

    ```groovy
    dependencies {
        implementation 'io.github.thibaultbee.streampack:streampack-services:3.0.0-RC3'
    }
    ```

2. Requests the required permissions in your Activity/Fragment. See the
   [Permissions](#permissions) section for more information.
3. Creates a `MyService` that extends `MediaProjectionService` (so you can customize
   notifications among other things).
4. Creates a screen record `Intent` and requests the activity result

    ```kotlin
    MediaProjectionUtils.createScreenCaptureIntent(context = requireContext())
    ```

5. Starts the service

    ```kotlin
    MediaProjectionService.bindService(
        requireContext(),
        MyService::class.java,
        result.resultCode,
        result.data,
        { streamer ->
            try {
                configure(streamer)
            } catch (t: Throwable) {
                // Handle exception
            }
            startStream(streamer)
        }
    )
    ```

For a complete example, check out the [demos/screenrecorder](demos/screenrecorder) directory .

## Permissions

You need to add the following permissions in your `AndroidManifest.xml`:

```xml

<manifest>
    <!-- Only for a live -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Only for a local record -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
</manifest>
```

To record locally, you also need to request the following dangerous
permission: `android.permission.WRITE_EXTERNAL_STORAGE`.

### Permissions for a camera stream

To use the camera, you need to request the following permission:

```xml

<manifest>
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
</manifest>
```

Your application also has to request the following dangerous
permission: `android.permission.RECORD_AUDIO`, `android.permission.CAMERA`.

For the PlayStore, your application might declare this in its `AndroidManifest.xml`

```xml

<manifest>
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
</manifest>
```

### Permissions for a screen recorder stream

To use the screen recorder, you need to request the following permission:

```xml

<manifest>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <!-- Only if you have to record audio -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
</manifest>
```

You will also have to declare the `Service`,

```xml

<application>
    <!-- YourScreenRecorderService extends DefaultScreenRecorderService -->
    <service android:name=".services.MyService" android:exported="false"
        android:foregroundServiceType="mediaProjection" />
</application>
```

## Documentations

[StreamPack API guide](https://thibaultbee.github.io/StreamPack)

- Additional documentations are available in the `docs` directory:
    - [Live and record simultaneously](docs/LiveAndRecordSimultaneously.md)
    - [Streamers](docs/Streamers.md)
    - [Streamer elements](docs/AdvancedStreamer)

## Demos

### Camera and audio demo

For source code example on how to use camera and audio streamers,
check [demos/camera](demos/camera). On
first launch, you will have to set RTMP url or SRT server IP in the settings menu.

### Screen recorder demo

For source code example on how to use screen recorder streamer, check
the [demos/screenrecorder](demos/screenrecorder)
. On first launch, you will have to set RTMP url or SRT server IP in the settings menu.

### Tests with a FFmpeg server

FFmpeg has been used as an SRT server+demuxer+decoder for the tests.

#### RTMP

Tells FFplay to listen on IP `0.0.0.0` and port `1935`.

```
ffplay -listen 1 -i 'rtmp://0.0.0.0:1935/s/streamKey'
```

On StreamPack sample app settings, set `Endpoint` -> `Type` to `Stream to a remove RTMP device`,
then set the server `URL` to `rtmp://serverip:1935/s/streamKey`. At this point, StreamPack sample
app should successfully sends audio and video frames. On FFplay side, you should be able to watch
this live stream.

#### SRT

Tells FFplay to listen on IP `0.0.0.0` and port `9998`:

```
ffplay -fflags nobuffer 'srt://0.0.0.0:9998?mode=listener'
```

On StreamPack sample app settings, set the server `IP` to your server IP and server `Port` to`9998`.
At this point, StreamPack sample app should successfully sends audio and video frames. On FFplay
side, you should be able to watch this live stream.

## Tips

### RTMP or SRT

RTMP and SRT are both live streaming protocols. SRT is a UDP-based modern protocol, it is
reliable and ultra low latency. RTMP is a TCP-based protocol, it is also reliable but it is only low
latency.
There are already a lot of comparison over the Internet, so here is a summary:

* SRT:
    - Ultra low latency(< 1 s)
* RTMP:
    - Low latency (2 - 3 s)

So, the main question is : "which protocol to use?"
It is easy: if your server has SRT support, use SRT otherwise use RTMP.

### Get device and protocol capabilities

Have you ever wonder : "What are the supported resolution of my cameras?" or "What is the supported
sample rate of my audio codecs ?"? `Info` classes are made for this. All `Streamer` comes with a
specific `Info` object:

 ```kotlin
val info = streamer.getInfo(MediaDescriptor("rtmps://serverip:1935/s/streamKey"))
```

For static endpoint or an opened dynamic endpoint, you can directly get the info:

```kotlin
val info = streamer.info
```

### Element specific configuration

If you are looking for more settings on streamer, like the exposure compensation of your camera, you
must have a look on `Settings` class. Each `Streamer` elements (such as `IVideoSource`,
`IAudioSource`,...)
comes with a public interface that allows to have access to specific information or configuration.

Example: if the video source can be cast to `ICameraSource` interface. You get the access to
`settings`
that allows to get and set the current camera settings:

```kotlin
(streamer.videoSource as ICameraSource).settings
```

Example: you can change the exposure compensation of your camera, on a `CameraStreamers`
you can do it like this:

```kotlin
(streamer.videoSource as ICameraSource).settings.exposure.compensation = value
```

Moreover you can retrieve exposure range and step with:

```kotlin
(streamer.videoSource as ICameraSource).settings.exposure.availableCompensationRange
(streamer.videoSource as ICameraSource).settings.exposure.availableCompensationStep
```

See the [docs/AdvancedStreamer.md](docs/AdvancedStreamer#element-specific-configuration) for more
information.

### Android versions

Even if StreamPack sdk supports a `minSdkVersion` 21. I strongly recommend to set the
`minSdkVersion` of your application to a higher version (the highest is the best!) for better
performance.

## Licence

    Copyright 2021 Thibault B.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
