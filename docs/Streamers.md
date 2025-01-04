# Streamer elements

## Introduction

The `Streamer` is a class that streams audio and video from a source to an endpoint. It is
responsible for controlling the audio and video sources, encoders, and endpoint.

StreamPack comes with a few streamers that are ready to use and can be customized. Also, it worth
noting that you can create your own streamer by extending the `SingleStreamer` or `DualStreamer`
class.

## Coroutine or callback streamers

There are 2 types of streamers:

- Kotlin Coroutine based: streamer APIs use `suspend` functions and `Flow`
- callback based: streamer APIs use callbacks.

I recommend using the coroutine based streamer. But if you don't want to use coroutines, you can use
the callback based streamer. The callback based streamer is a wrapper of the coroutine based
streamer but it might miss some features. On your side, you can create your own callback based
streamer by wrapping the coroutine based streamer.

## Single streamers

The base streamer implementation is the `SingleStreamer`. It is `Streamer` with a single endpoint.
Other single streamers inherit from `SingleStreamer`.

The single streamers data flow is as follows:

<!--
@startuml
rectangle CameraSingleStreamer {
[CameraSource] as VideoSource
[MicrophoneSource] as AudioSource
[Encoder] as VideoEncoder
[Encoder] as AudioEncoder
[Endpoint] as Endpoint
VideoSource -r-> VideoEncoder
AudioSource -r-> AudioEncoder
VideoEncoder -r-> Endpoint
AudioEncoder -r-> Endpoint
AudioSource -d[hidden]-> VideoSource
AudioEncoder -d[hidden]-> VideoEncoder
}
}
@enduml
-->

- `AudioOnlySingleStreamer`: A streamer that streams from an audio source (microphone by default).
- `CameraSingleStreamer`/`CameraCallbackSingleStreamer`: A streamer that streams from a camera and
  microphone. It
  adds `startPreview`, `stopPreview` methods to the `Streamer` object as well as a camera settings.
- `ScreenRecorderSingleStreamer`: A streamer that streams from the phone screen and microphone. It
  adds specific methods for screen recorder as a API to set activity result.

By default the `Streamer` endpoint is the `DynamicEndpoint` which made the `Streamer` agnostic of
the protocol.
The `DynamicEndpoint` infers from the `MediaDescriptor` object passed to the `Streamer` by `open` or
`startStream` methods.

### Limiting supported protocols

By default, the `Streamer` supports all StreamPack supported output protocols thanks to the
`DynamicEndpoint`.
If you want to limit the supported protocols, you can directly pass a endpoint to the `Streamer`
constructor.

```kotlin
val streamer = CameraSingleStreamer(
    context,
    internalEndpoint = YourEndpoint() // Example: RtmpEndpoint()
)
```

### Implementing your own single streamer

`Streamers` implement the `ICoroutineSingleStreamer` interface.

## Dual streamers

The `DualStreamer` is a `Streamer` that streams to 2 independent outputs.

