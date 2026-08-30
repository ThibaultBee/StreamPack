# 🎛️ Audio/Video Configuration

When starting a stream, you typically configure the static parameters (like resolution, FPS, and sample rate) using `AudioConfig` and `VideoConfig` before calling `startStream()`.

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

viewModelScope.launch {
    streamer.setAudioConfig(audioConfig)
    streamer.setVideoConfig(videoConfig)
}
```

## Device and Protocol Capabilities

When configuring your stream, you may need to query the specific capabilities of the user's device or the streaming protocol—such as the supported camera resolutions, frame rates, or audio sample rates. 

StreamPack provides `Info` classes to easily retrieve these capabilities. Every `Streamer` exposes a configuration `Info` object.

If you are using a dynamic endpoint, you must provide a `MediaDescriptor` to query the capabilities for that specific destination:

```kotlin
val info = streamer.getInfo(MediaDescriptor("rtmps://serverip:1935/s/streamKey"))
```

If you are using a static endpoint (or an already opened dynamic endpoint), you can retrieve the capabilities directly:

```kotlin
val info = streamer.info
```
