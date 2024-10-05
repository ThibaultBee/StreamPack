# StreamPack: RTMP and [SRT](https://github.com/Haivision/srt) live streaming SDK for Android

StreamPack is a modular live streaming library for Android made for both demanding video
broadcasters and new video enthusiasts.

It is designed to be used in live streaming and gaming apps.

## Setup

Get StreamPack core latest artifacts on mavenCentral:

```groovy
dependencies {
    implementation 'io.github.thibaultbee.streampack:streampack-core:2.6.1'
    // For UI (incl. PreviewView)
    implementation 'io.github.thibaultbee.streampack:streampack-ui:2.6.1'
    // For ScreenRecorder service
    implementation 'io.github.thibaultbee.streampack:streampack-services:2.6.1'
    // For RTMP
    implementation 'io.github.thibaultbee.streampack:streampack-extension-rtmp:2.6.1'
    // For SRT
    implementation 'io.github.thibaultbee.streampack:streampack-extension-srt:2.6.1'
}
```

If you use both RTMP and SRT, you might have a conflict with `libssl.so` and `libcrypto.so` because
they
are both includes in native dependencies. To solve this, you can add in your `build.gradle`:

```groovy
android {
    packagingOptions {
        pickFirst '**/*.so'
    }
}
```

## Features

* Video:
    * Source: Cameras or Screen recorder
    * Orientation: portrait or landscape
    * Codec: HEVC/H.265, AVC/H.264, VP9 or AV1 (experimental,
      see https://github.com/ThibaultBee/StreamPack/discussions/90)
    * HDR (experimental, see https://github.com/ThibaultBee/StreamPack/discussions/91)
    * Configurable bitrate, resolution, framerate (tested up to 60), encoder level, encoder profile
    * Video only mode
    * Device video capabilities
* Audio:
    * Codec: AAC:LC, HE, HEv2,... or Opus
    * Configurable bitrate, sample rate, stereo/mono, data format
    * Processing: Noise suppressor or echo cancellation
    * Audio only mode
    * Device audio capabilities
* File: TS, FLV, MP4, WebM and Fragmented MP4
    * Write to a single file or multiple chunk files
* Streaming: RTMP/RTMPS or SRT
    * Support for enhanced RTMP
    * Ultra low-latency based on [SRT](https://github.com/Haivision/srt)
    * Network adaptive bitrate mechanism for [SRT](https://github.com/Haivision/srt)

## Samples

### Camera and audio sample

For source code example on how to use camera and audio streamers, check
the [sample app directory](https://github.com/ThibaultBee/StreamPack/tree/master/demos/camera). On
first launch, you will have to set RTMP url or SRT server IP in the settings menu.

### Screen recorder

For source code example on how to use screen recorder streamer, check
the [sample screen recorder directory](https://github.com/ThibaultBee/StreamPack/tree/master/demos/screenrecorder)
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

Check how to build FFmpeg with libsrt
in [SRT CookBook](https://srtlab.github.io/srt-cookbook/apps/ffmpeg/). Tells FFplay to listen on
IP `0.0.0.0` and port `9998`:

```
ffplay -fflags nobuffer 'srt://0.0.0.0:9998?mode=listener'
```

On StreamPack sample app settings, set the server `IP` to your server IP and server `Port` to `9998`
. At this point, StreamPack sample app should successfully sends audio and video frames. On FFplay
side, you should be able to watch this live stream.

## Quick start

If you want to create a new application, you should use the
template [StreamPack boilerplate](https://github.com/ThibaultBee/StreamPack-boilerplate). In 5
minutes, you will be able to stream live video to your server.

1. Request the required permissions in your Activity/Fragment.

2. Creates a `SurfaceView` to display camera preview in your layout

As a camera preview, you can use a `SurfaceView`, a `TextureView` or any
`View` where that can provide a `Surface`.

To simplify integration, StreamPack provides an `PreviewView`.

```xml

<layout>
    <io.github.thibaultbee.streampack.views.PreviewView android:id="@+id/preview"
        android:layout_width="match_parent" android:layout_height="match_parent"
        app:enableZoomOnPinch="true" />
</layout>
```

`app:enableZoomOnPinch` is a boolean to enable zoom on pinch gesture.

3. Instantiates the streamer (main live streaming class)

There are 2 types of streamers:

- Kotlin Coroutine based: streamer APIs use `suspend` functions and `Flow`
- callback based: streamer APIs use callbacks

```kotlin
// For coroutine based
val streamer = DefaultCameraStreamer(context = requireContext())
// For callback based
// val streamer = DefaultCameraCallbackStreamer(context = requireContext())
```

4. Configures audio and video settings

```kotlin
// Already instantiated streamer
val streamer = DefaultCameraStreamer(context = requireContext())

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

streamer.configure(audioConfig, videoConfig)
```

5. Inflates the camera preview with the streamer

```kotlin
// Already instantiated streamer
val streamer = DefaultCameraStreamer(context = requireContext())

/**
 * If the preview is a [PreviewView]
 */
preview.streamer = streamer
/**
 * If the preview is in a SurfaceView, a TextureView, a Surface,... you can use:
 */
streamer.startPreview(preview)
```

6. Starts the live streaming

```kotlin
// Already instantiated streamer
val streamer = DefaultCameraStreamer(context = requireContext())


val descriptor =
    UriMediaDescriptor("rtmps://serverip:1935/s/streamKey") // For RTMP/RTMPS. Uri also supports SRT url, file, content path,...
/**
 * Alternatively, you can use object syntax:
 * - RtmpConnectionDescriptor("rtmps", "serverip", 1935, "s", "streamKey") // For RTMP/RTMPS
 * - SrtConnectionDescriptor("serverip", 1234) // For SRT
 */

streamer.startStream() 
```

7. Stops and releases the streamer

```kotlin
// Already instantiated streamer
val streamer = DefaultCameraStreamer(context = requireContext())

streamer.stopStream()
streamer.close() // Disconnect from server or close the file
streamer.stopPreview() // The StreamerSurfaceView will be automatically stop the preview
streamer.release()
```

For more detailed explanation, check out
the [API documentation](https://thibaultbee.github.io/StreamPack).

## Permissions

You need to add the following permissions in your `AndroidManifest.xml`:

```xml

<manifest>
    <!-- Only for a live -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Only for a record -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
</manifest>
```

For a record, you also need to request the following dangerous
permission: `android.permission.WRITE_EXTERNAL_STORAGE`.

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
    <service android:name=".services.YourScreenRecorderService" android:exported="false"
        android:foregroundServiceType="mediaProjection" />
</application>
```

## Rotations

To set the `Streamer` orientation, you can use the `targetRotation` setter:

```kotlin
// Already instantiated streamer
val streamer = DefaultCameraStreamer(context = requireContext())

streamer.targetRotation =
    Surface.ROTATION_90 // Or Surface.ROTATION_0, Surface.ROTATION_180, Surface.ROTATION_270
```

StreamPack comes with a `RotationProvider` that fetches and listens the device rotation: the
`DeviceRotationProvider`. The `DeviceRotationProvider` is backed by the `OrientationEventListener`.

```kotlin
// Already instantiated streamer
val streamer = DefaultCameraStreamer(context = requireContext())

val listener = object : IRotationProvider.Listener {
    override fun onOrientationChanged(rotation: Int) {
        streamer.targetRotation = rotation
    }
}
rotationProvider.addListener(listener)

// Don't forget to remove the listener when you don't need it anymore
rotationProvider.removeListener(listener)
```

See the `demos/camera` for a complete example.

To only get the device supported orientations, you can use the `DisplayManager.DisplayListener` or
create your own `targetRotation` provider.

## Tips

### RTMP or SRT

RTMP and SRT are both live streaming protocols . SRT is a UDP - based modern protocol, it is
reliable
and ultra low latency . RTMP is a TCP - based protocol, it is also reliable but it is only low
latency .
There are already a lot of comparison over the Internet, so here is a summary:
SRT:

-Ultra low latency(< 1 s)
-HEVC support through MPEG -TS RTMP :
-Low latency (2 - 3 s)
-HEVC not officially support (specification has been aban by its creator)

So, the main question is : "which protocol to use?"
It is easy: if your server has SRT support, use SRT otherwise use RTMP.

### Streamers

Let's start with some definitions! `Streamers` are classes that represent a streaming pipeline:
capture, encode, mux and send.They comes in multiple flavours: with different audio and video
source . 3 types of base streamers
are available :

-`DefaultCameraStreamer`: for streaming from camera
-`DefaultScreenRecorderStreamer`: for streaming from screen
-`DefaultAudioOnlyStreamer`: for streaming audio only

Since 3.0.0, the endpoint of a `Streamer` is inferred from the `MediaDescriptor` object passed to
the `open` or `startStream` methods.It is possible to limit the possibility of the endpoint by
implementing your own `DynamicEndpoint.Factory` or passing a endpoint as the `Streamer` `endpoint`
parameter.To create a `Streamer` for a new source, you have to create a new `Streamer` class that
inherits
from `DefaultStreamer` .

### Get device capabilities

Have you ever wonder : "What are the supported resolution of my cameras?" or "What is the supported
sample rate of my audio codecs ?"? `Info` classes are made for this. All `Streamer` comes with a
specific `Info` object :

    ```kotlin

val info = streamer.getInfo(MediaDescriptor("rtmps://serverip:1935/s/streamKey"))

```

For static endpoint or an opened dynamic endpoint, you can directly get the info:

```kotlin
val info = streamer.info
```

### Get extended settings

If you are looking for more settings on streamer, like the exposure compensation of your camera, you
must have a look on `Settings` class. Each `Streamer` elements (such
as `VideoSource`, `AudioSource`,...)
comes with a public interface that allows to have access to specific information or configuration.

```kotlin
 (streamer.videoSource as IPublicCameraSource).settings
```

For example, if you want to change the exposure compensation of your camera, on a `CameraStreamers`
you can do it like this:

```kotlin
 (streamer.videoSource as IPublicCameraSource).settings.exposure.compensation = value
```

Moreover you can check exposure range and step with:

```kotlin
 (streamer.videoSource as IPublicCameraSource).settings.exposure.availableCompensationRange
(streamer.videoSource as IPublicCameraSource).settings.exposure.availableCompensationStep
```

### Screen recorder Service

To record the screen, you have to use the `DefaultScreenRecorderStreamer` inside
an [Android Service](https://developer.android.com/guide/components/services). To simplify this
integration, StreamPack provides the `DefaultScreenRecorderService` classes. Extends one of these
class
and overrides `onNotification` to customise the notification.

### Android SDK version

Even if StreamPack sdk supports a `minSdkVersion` 21. I strongly recommend to set the
`minSdkVersion` of your application to a higher version (the highest is the best!) for higher
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
