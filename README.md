# StreamPack: RTMP and [SRT](https://github.com/Haivision/srt) live streaming SDK for Android

StreamPack is a modular live streaming library for Android made for both demanding video
broadcasters and new video enthusiasts.

It is designed to be used in live streaming and gaming apps.

## Setup

Get StreamPack core latest artifacts on mavenCentral:

```groovy
dependencies {
    implementation 'io.github.thibaultbee:streampack:2.6.0'
    // For UI (incl. PreviewView)
    implementation 'io.github.thibaultbee:streampack-ui:2.6.0'
    // For RTMP
    implementation 'io.github.thibaultbee:streampack-extension-rtmp:2.6.0'
    // For SRT
    implementation 'io.github.thibaultbee:streampack-extension-srt:2.6.0'
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
* File: TS or FLV or Fragmented MP4
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

1. Add [permissions](#permissions) to your `AndroidManifest.xml` and request them in your
   Activity/Fragment.

2. Create a `SurfaceView` to display camera preview in your layout

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

3. Instantiate the streamer (main live streaming class)

```kotlin
val streamer = CameraSrtLiveStreamer(context = requireContext())
```

4. Configure audio and video settings

```kotlin
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

5. Inflate the camera preview with the streamer

```kotlin
/**
 * If the preview is in a PreviewView
 */
preview.streamer = streamer
/**
 * If the preview is in a SurfaceView, a TextureView, or any View that can provide a Surface
 */
streamer.startPreview(preview)
```

6. Start the live streaming

```kotlin
streamer.startStream(ip, port)
```

7. Stop and release the streamer

```kotlin
streamer.stopStream()
streamer.disconnect()
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
    <!-- YourScreenRecorderService extends ScreenRecorderRtmpLiveService or ScreenRecorderSrtLiveService -->
    <service android:name=".services.YourScreenRecorderService" android:exported="false"
        android:foregroundServiceType="mediaProjection" />
</application>
```

## Tips

### RTMP or SRT

RTMP and SRT are both live streaming protocols. SRT is a UDP-based modern protocol, it is reliable
and ultra low latency. RTMP is a TCP-based protocol, it is also reliable but it is only low latency.
There are already a lot of comparison over the Internet, so here is a summary:
SRT:

- Ultra low latency (< 1s)
- HEVC support through MPEG-TS RTMP:
- Low latency (2-3s)
- HEVC not officially support (specification has been aban by its creator)

So, the main question is: "which protocol to use?"
It is easy: if your server has SRT support, use SRT otherwise use RTMP.

### Streamers

Let's start with some definitions! `Streamers` are classes that represent a live streaming pipeline:
capture, encode, mux and send. They comes in multiple flavours: with different audio and video
source, with different endpoints and functionalities... 3 types of base streamers are available:

- `CameraStreamers`: for streaming from camera
- `ScreenRecorderStreamers`: for streaming from screen
- `AudioOnlyStreamers`: for streaming audio only

You can find specific streamers for File or for Live. Currently, there are 2 main endpoints:

- `FileStreamer`: for streaming to file
- `LiveStreamer`: for streaming to a RTMP or a SRT live streaming server

For example, you can use `AudioOnlyFlvFileStreamer` to stream from microphone only to a FLV file.
Another example, you can use `CameraRtmpLiveStreamer` to stream from camera to a RTMP server.

If a streamer is missing, of course, you can also create your own. You should definitely submit it
in a [pull request](https://github.com/ThibaultBee/StreamPack/pulls).

### Get device capabilities

Have you ever wonder: "What are the supported resolution of my cameras?" or "What is the supported
sample rate of my audio codecs?"? `Helpers` classes are made for this. All `Streamer` comes with a
specific `Helper` object (I am starting to have the feeling I repeat myself):

```kotlin
val helper = streamer.helper
```

### Get extended settings

If you are looking for more settings on streamer, like the exposure compensation of your camera, you
must have a look on `Settings` class. All together: "All `Streamer` comes with a specific `Settings`
object":

```kotlin
streamer.settings
```

For example, if you want to change the exposure compensation of your camera, on a `CameraStreamers`
you can do it like this:

```kotlin
streamer.settings.camera.exposure.compensation = value
```

Moreover you can check exposure range and step with:

```kotlin
streamer.settings.camera.exposure.availableCompensationRange
streamer.settings.camera.exposure.availableCompensationStep
```

### Screen recorder Service

To record the screen, you have to use one of the `ScreenRecorderStreamers` inside
an [Android Service](https://developer.android.com/guide/components/services). To simplify this
integration, StreamPack provides several `ScreenRecorderService` classes. Extends one of these class
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
