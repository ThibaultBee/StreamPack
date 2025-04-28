# Streamer elements

## Introduction

The `Streamer` is a class that streams audio and video from a source to an endpoint. It is
responsible for controlling the audio and video sources, encoders, and endpoint.

Multiple streamers are available depending on the number of independent outputs you want to
have:

- `SingleStreamer`: for a single output (example: live or record)
- `DualStreamer`: for 2 independent outputs (example: live stream and record)
- for multiple outputs, you can use the `StreamerPipeline` class that allows to create more complex
  pipeline with multiple independent outputs (example: audio in one file, video in another file)

## Single streamers

The `SingleStreamer` is a `Streamer` that streams to a single output.
The implementation is the `SingleStreamer`. Underneath, it is `StreamerPipeline` with a single
`EncodingOutput`.

The single streamers data flow is as follows:

<!--
@startuml
rectangle SingleStreamer {
rectangle StreamerPipeline {
rectangle EncodingOutput {
  port audio
  port video
  [Video encoder] as VideoEncoder
  [Audio encoder] as AudioEncoder
  [Endpoint] as Endpoint

  audio --> AudioEncoder
video --> VideoEncoder
AudioEncoder -r-> Endpoint
VideoEncoder -r-> Endpoint
}
[Video source] as VideoSource
[Audio source] as AudioSource

VideoSource --> video
AudioSource --> audio
}
}
@enduml
-->

- `AudioOnlySingleStreamer`: A streamer that streams from an audio source (microphone by default).
- `VideoOnlySingleStreamer`: A streamer that streams from a video source (microphone by default).
- `cameraSingleStreamer`: A factory to create a streamer with a camera source.
- `videoMediaProjectionSingleStreamer`: A factory to create a streamer with a media projection video
  source. You need to set activity result
- `audioVideoMediaProjectionSingleStreamer`: A factory to create a streamer with a media projection
  video source and a media projection audio source. You need to set activity result

By default the `Streamer` endpoint is the `DynamicEndpoint` which made the `Streamer` agnostic of
the protocol.
The `DynamicEndpoint` infers from the `MediaDescriptor` object passed to the `Streamer` by `open` or
`startStream` methods.

## Dual streamers

The `DualStreamer` is a `Streamer` that streams to 2 independent outputs.
The implementation is the `DualStreamer`. Underneath, it is `StreamerPipeline` with 2
`EncodingOutput`.

## Streamer pipeline

The `StreamerPipeline` offers a way to create a custom pipeline with multiple independent outputs.
Add an `EncodingOutput` to the pipeline with `createOutput` method.

There are currently no limitations on the number of outputs you can create but be careful with the
number of encoders you create. Each encoder will use CPU and memory resources.

```kotlin
// In this example, we create a streamer pipeline with 2 outputs: one for audio and one for video
val streamerPipeline = StreamerPipeline(context, withAudio = true, withVideo = true)

// Add sources
streamerPipeline.setAudioSource(MicrophoneSourceFactory())
streamerPipeline.setVideoSource(CameraSourceFactory())

// Add outputs
val audioOnlyOutput =
    streamerPipeline.createEncodingOutput(withVideo = false) as IConfigurableAudioEncodingPipelineOutput
val videoOnlyOutput =
    streamerPipeline.createEncodingOutput(withAudio = false) as IConfigurableVideoEncodingPipelineOutput

// Configure outputs
val audioConfig = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
val videoConfig = VideoCodecConfig(
    mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
    resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT)
)

audioOnlyOutput.setAudioCodecConfig(audioConfig)
videoOnlyOutput.setVideoCodecConfig(videoConfig)

// Run stream
val audioOnlyDescriptor = UriMediaDescriptor(FileUtils.createCacheFile("audio.ogg").toUri())
val videoOnlyDescriptor = UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri())
audioOnlyOutput.startStream(audioOnlyDescriptor)
videoOnlyOutput.startStream(videoOnlyDescriptor)
```

## Limiting supported protocols

By default, the `Streamer` supports all StreamPack supported output protocols thanks to the
`DynamicEndpoint`.
If you want to limit the supported protocols, you can directly pass a endpoint to the `Streamer`
constructor.

```kotlin
// For single streamer
val streamer = SingleStreamer(
    context,
    endpointFactory = YourEndpointFactory() // Example: RtmpEndpointFactory()
)
// For dual streamer
val streamer = DualStreamer(
    context,
    firstEndpointFactory = YourEndpointFactory(), // Example: RtmpEndpointFactory()
    secondEndpointFactory = YourEndpointFactory() // Example: RtmpEndpointFactory()
)
// For streamer pipeline
val streamer = StreamerPipeline(
    context
)
streamer.createOutput(
    YourEndpointFactory() // Example: RtmpEndpointFactory()
)
```

