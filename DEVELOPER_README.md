# StreamPack developer guide

## Introduction

### Definitions

`Source`:
A class that represents an audio or video source. For example, a camera (`CameraSource`), or a
microphone (`AudioSource`).

`Encoder`:
A class that represents an audio or video encoders. Only Android MediaCodec API is used (
`MediaCodecEncoder`).

`Endpoint`:
The last element of a live streaming pipeline. It is responsible for handling the frames after the
encoder.
The endpoint could be a remote server (RTMP, SRT,...) or a file (FLV, MPEG-TS,...).
The main endpoint is `CompositeEndpoint` that is composed of a `Muxer` and a `Sink`.

`Muxer`:
A process that packs audio and video frames to a container (FLV, MPEG-TS, MP4,...).
The `CompositeEndpoint` is composed of a `IMuxer`.

`Sink`:
A process that sends the container to a remote server (RTMP, SRT,...) or to a file.
The `CompositeEndpoint` is composed of a `ISink`.

`Streamer`:
A class that represent a audio and/or video live streaming pipeline. It manages sources, encoders,
muxers, endpoints,... and have lot of tools. They are the most important class for users.
Unless explicitly stated, the `Endpoint` is inferred from the `MediaDescriptor` object thanks to
the `DynamicEndpoint`.

`Streamer element`:
Could be a `Source`, `Encoder`, `Muxer`, or `Endpoint`. They implement the `Streamable<T>` and they
might have a public interface to access specific info.

`Info`:
A class that provides a set of methods to help to `streamer` configuration such as supported
resolutions,... It comes with an instantiated `Streamer` object:

```kotlin
val info = streamer.getInfo(MediaDescriptor(`media uri`))
```

They might be different for each `Streamer` object. For example, a `FlvStreamer` object will not
have the same `Info` object as a `TsStreamer` object because FLV does not support a wide range of
codecs, audio sample rate,...

`Settings`:
Each streamer elements have a public interface that allows go have access to specific
information or configuration.
For example, the `VideoEncoder` object has a `bitrate` property that allows to get and set the
current video bitrate.
Example:

```kotlin
// Get video bitrate
val bitrate = streamer.videoEncoder!!.bitrate
// Set video bitrate on the fly
streamer.videoEncoder!!.bitrate = 2000000
```

## Streamers

The streamer implementation is the `DefaultStreamer`. All other streamers inherit from it. Then 2
specifics
base streamers inherit from it:

- `DefaultCameraStreamer`: A streamer that streams from a camera and microphone. It
  adds `startPreview`
  , `stopPreview` methods to the `Streamer` object as well as a camera settings.
- `DefaultScreenRecorderStreamer`: A streamer that streams from the phone screen and microphone. It
  adds specific methods for screen recorder as a API to set activity result.

To endpoint of a `Streamer` is inferred from the `MediaDescriptor` object passed to the `Streamer`
by `open` or `startStream` methods.

## Sources

There are 2 types of sources:

- frames are captured in a `ByteBuffer`: such as a microphone. `ByteBuffer` sources
  implement `IFrameSource`.
- frames are passed to the encoder surface (video only): when the video source can write to
  a `Surface`. Its purpose is to improve encoder performance. For example, it suits camera and
  screen recorder. `Surface` sources implement `ISurfaceSource`.

@startuml
interface IVideoSource {

+ hasSurface: Boolean
+ encoderSurface: Surface?
+ getFrame(): ByteBuffer
  }

interface IAudioSource {

+ getFrame(): ByteBuffer
  }
  @enduml
+

To create a new audio source, implements a `IAudioSource`. It inherits from `IFrameSource`.

To create a new video source, implements a `IVideSource`. It inherits from both `IFrameCapture`
and `ISurfaceSource`. Always prefer to use a video source as a `Surface` source if it is possible.

If your video source is a `Surface` source, set:

- `hasSurface` = true
- `encoderSurface` must not return null
- `getFrame` won't be use, you can make it throw an exception.

If your video source is a `ByteBuffer` source, set:

- `hasSurface` = false
- `encoderSurface` = null
- `getFrame` to fill the `ByteBuffer` with the raw video frame.

## Encoders

The only encoder is based on Android `MediaCodec` API. It implements the `IEncoder` interface.

@startuml
interface IEncoder {
}
@enduml

## Endpoints

They implement the `IEndpoint` interface.

@startuml
interface IEndpoint {

+ open()
+ close()
+ write()
  }

class CompositeEndpoint {

+ muxer: IMuxer
+ sink: ISink
  }
  @enduml

### Muxers

They implement the `IMuxer` interface.

### Sinks

They implement the `ISink` interface.

### Streamers

The implement the `ICoroutineStreamer` interface.

@startuml
class DefaultStreamer {

+ videoSource: IVideoSource
+ audioSource: IAudioSource
+ endoint: IEndpoint

- videoEncoder: IEncoder
- audioEncoder: IEncoder
  }
  @enduml

@startuml
rectangle DefaultCameraStreamer {
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