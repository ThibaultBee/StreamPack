# 🎥 Source Configuration

## Setting Audio & Video Sources

You can dynamically set or change the audio and video sources in your `Streamer` object by passing a source factory to `setAudioSource` and `setVideoSource`.

```kotlin
val streamer = SingleStreamer(context)

// Set microphone and camera sources
streamer.setAudioSource(MicrophoneSourceFactory())
streamer.setVideoSource(CameraSourceFactory())
```

### Available Source Factories

You can pass any of the following built-in factories to `setAudioSource()` or `setVideoSource()` depending on your needs:

**Audio:**

- [`MicrophoneSourceFactory`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord/-microphone-source-factory/index.html) (Standard device microphone)
- [`MediaProjectionAudioSourceFactory`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord/-media-projection-audio-source-factory/index.html) (Screen audio recording)

**Video:**

- [`CameraSourceFactory`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.camera/-camera-source-factory/index.html) (Standard device camera)
- [`MediaProjectionVideoSourceFactory`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection/-media-projection-video-source-factory/index.html) (Screen video recording)
- [`BitmapSourceFactory`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.bitmap/-bitmap-source-factory/index.html) (Stream a static image)

!!! note "If you need to stream from a source that isn't provided out of the box, check out the [Creating Custom Sources](../use_cases/CustomSources.md) guide."

### Helpful Source Extensions

StreamPack provides convenient extension functions to quickly configure specific sources without needing to cast them manually:

```kotlin
// Switch to a specific camera ID (often "0" for back, "1" for front but you should always query the camera list)
streamer.setCameraId("0") 

// Set a static image/bitmap as the video source (useful for "stream starting soon" screens)
val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.holding_image)
streamer.setBitmap(bitmap)
```

## On-the-fly control

On a streamer object, you can retrieve the source object and cast it to the specific source:
`audioSource` (an [`IAudioSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio/-i-audio-source/index.html)) or `videoSource` (an [`IVideoSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video/-i-video-source/index.html)). This allows you to have access to specific hardware configuration on the fly while streaming.

```kotlin 
val streamer = cameraSingleStreamer(context)

// Audio source
streamer.audioInput.sourceFlow.filterNotNull().collect {
    if (this is IAudioRecordSource) {
        // Specific audio source configuration
        // Example: IAudioRecordSource specific configuration has `addEffect` method
        addEffect(AudioEffect.EFFECT_TYPE_AEC)
    }
}

// Video source
streamer.videoInput.sourceFlow.filterNotNull().collect {
    if (this is ICameraSource) {
        // Specific video source configuration
        // Example: ICameraSource specific configuration has `settings` member.
        settings.flash.setIsEnable(true)
        settings.stabilization.setIsEnableOptical(false)
    }
}
```

Here is how to cast the sources:

* [`MicrophoneSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord/-microphone-source/index.html) -> [`IAudioRecordSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord/-i-audio-record-source/index.html)
* [`MediaProjectionAudioSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord/-media-projection-audio-source/index.html) -> [`IMediaProjectionSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources/-i-media-projection-source/index.html) and [`IAudioRecordSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord/-i-audio-record-source/index.html)
* [`CameraSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.camera/-camera-source/index.html) -> [`ICameraSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.camera/-i-camera-source/index.html)
* [`MediaProjectionVideoSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection/-media-projection-video-source/index.html) -> [`IMediaProjectionSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources/-i-media-projection-source/index.html)
