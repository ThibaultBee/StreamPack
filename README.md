# StreamPack: live streaming SDK for Android based on Secure Reliable Transport ([SRT](https://github.com/Haivision/srt))

StreamPack brings the best audio/video live technologies together in order to achieve low-latency &
high quality live streaming for Android.

## Features

* Network:
  * Ultra low-latency based on [SRT](https://github.com/Haivision/srt)
  * Network adaptive bitrate mechanism
* Video:
  * Source: Cameras or Screen recorder
  * Orientation: portrait or landscape
  * Codec: HEVC/H.265 or AVC/H.264
  * Configurable bitrate, resolution, framerate (tested up to 60), encoder level, encoder profile
  * Video only mode
* Audio:
  * Codec: AAC-LC
  * Configurable bitrate, sample rate, stereo/mono, data format
  * Processing: Noise suppressor or echo cancellation
  * Audio only mode

## Setup

Get StreamPack latest artifacts on [jitpack.io](https://jitpack.io/#ThibaultBee/StreamPack)

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.ThibaultBee:StreamPack:1.3.0'
}
```

## Samples

### Camera and audio sample

For source code example on how to use camera and audio streamers, check
the [sample app directory](https://github.com/ThibaultBee/StreamPack/tree/master/app). On first
launch, you will have to set SRT server IP in the settings menu.

### Screen recorder

For source code example on how to use screen recorder streamer, check
the [sample screen recorder directory](https://github.com/ThibaultBee/StreamPack/tree/master/screenrecorder)
. On first launch, you will have to set SRT server IP in the settings menu.

### Quick start with a FFmpeg SRT server

FFmpeg has been used as an SRT server+demuxer+decoder for the tests. Check how to build FFmpeg with
libsrt in [SRT CookBook](https://srtlab.github.io/srt-cookbook/apps/ffmpeg/). Tells FFplay to listen
on IP `0.0.0.0` and port `9998`:

```
ffplay -fflags nobuffer srt://0.0.0.0:9998?mode=listener
```

On StreamPack sample app settings, set the server `IP` to your server IP and server `port` to `9998`
. At this point, StreamPack sample app should successfully sends audio and video frames. On FFplay
side, you should be able to watch this stream.

## Quick start

1. Adds [permissions](#permissions) to your `AndroidManifest.xml` and request them in your
   Activity/Fragment.

2. Creates a `AutoFitSurfaceView` in your layout

To simplify development, StreamPack provides an `AutoFitSurfaceView`.

```xml
<com.github.thibaultbee.streampack.views.AutoFitSurfaceView
        android:id="@+id/preview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

3. Prepares audio and video configurations

```kotlin
val audioConfig = AudioConfig.Builder()
    .setStartBitrate(128000)
    .setSampleRate(48000)
    .setNumberOfChannel(2)
    .build()

val videoConfig = VideoConfig.Builder()
    .setStartBitrate(1000000) // 1 Mb/s
    .setResolution(Size(1280, 720))
    .setFps(30)
    .build()
```

4. Instantiates the streamer (main live streaming class)

```kotlin
val streamer = CameraSrtLiveStreamer.Builder()
    .setContext(getApplication())
    .setServiceInfo(tsServiceInfo)
    .setConfiguration(audioConfig, videoConfig)
    .build()
```

5. Starts the camera preview

```kotlin
streamer.startPreview(surface) // surface from AutoFitSurfaceView, SurfaceView or TextureView: where to display preview
```

6. Starts the live streaming

```kotlin
streamer.startStream(ip, port)
```

7. Stops and releases

```kotlin
streamer.stopStream()
streamer.disconnect()
streamer.stopPreview()
streamer.release()
```

For more detailed explanation, check out
the [API documentation](https://thibaultbee.github.io/StreamPack).

## Permissions

You need to add the following permissions in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<!-- Application requires android.permission.WRITE_EXTERNAL_STORAGE only for IFileStreamer implementation` -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

Your application also has to request the following dangerous
permission: `android.permission.RECORD_AUDIO`, `android.permission.CAMERA` and
`android.permission.WRITE_EXTERNAL_STORAGE` (only for only for `IFileStreamer` implementation).

For the PlayStore, your application might declare this in its `AndroidManifest.xml`

```xml
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

## Android SDK version

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