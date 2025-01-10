# Streamer elements

## Definitions

* `Source`:
  A class that represents an audio or video source. For example, a camera (
  `CameraSource`), or a microphone (`AudioSource`).

* `Encoder`:
  A class that represents an audio or video encoders. Only Android MediaCodec API is
  used (`MediaCodecEncoder`).

* `Endpoint`:
  The last element of a live streaming pipeline. It is responsible for handling the frames after the
  encoder.
  The endpoint could be a remote server (RTMP, SRT,...) or a file (FLV, MPEG-TS,...).
  The main endpoint is `CompositeEndpoint` that is composed of a `Muxer` and a `Sink`.

* `Muxer`:
  A process that packs audio and video frames to a container (FLV, MPEG-TS, MP4,...).
  The `CompositeEndpoint` is composed of a `IMuxer`.

* `Sink`:
  A process that sends the container to a remote server (RTMP, SRT,...) or to a file.
  The `CompositeEndpoint` is composed of a `ISink`.

* `Streamer`:
  A class that represent a audio and/or video live streaming pipeline. It manages sources, encoders,
  muxers, endpoints,... and have lot of tools. They are the most important class for users.
  Unless explicitly stated, the `Endpoint` is inferred from the `MediaDescriptor` object thanks to
  the `DynamicEndpoint`.

* `Streamer element`:
  Could be a `Source`, `Encoder`, `Muxer`, or `Endpoint`. They implement the `Streamable<T>` and
  they
  might have a public interface to access specific info.

* `Info`:
  A class that provides a set of methods to help to `streamer` configuration such as supported
  resolutions,...
  They might be different for each `Streamer` object. For example, a `FlvStreamer` object will not
  have the same `Info` object as a `TsStreamer` object because FLV does not support a wide range of
  codecs, audio sample rate,...
  It comes with an instantiated `Streamer` object:

```kotlin
val info = streamer.getInfo(MediaDescriptor(`media uri`))
```

* Element specific configuration:
  Each streamer elements have a public interface that allows yo have access to specific
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

## Sources

Audio sources implement the `IAudioSource` interface.
Video sources implement the `IVideoSource` interface and `IFrameSource` or `ISurfaceSource`.

### Available sources

* Video sources:
    - `CameraSource`: A source that captures frames from a camera.
    - `MicrophoneSource`: A source that captures frames from a microphone.
    - `MediaProjectionVideoSource`: A source that captures frames from the screen.

* Audio sources:
    - `MicrophoneSource`: A source that captures frames from a microphone.
    - `MediaProjectionAudioSource`: A source that captures frames from the screen.

Sources are a parameter of `Streamer` constructors.

### Creates your custom sources

There are 2 types of sources:

- frames are captured in a `ByteBuffer`: such as a microphone. `ByteBuffer` sources
  implement `IFrameSource` (such as `MicrophoneSource`).
- frames are passed to the encoder surface (video only): when the video source can write to
  a `Surface`. Its purpose is to improve encoder performance. For example, it suits camera and
  screen recorder. `Surface` sources implement `ISurfaceSource` (such as `CameraSource`).

<!--
@startuml
interface ISurfaceSource {
  + outputSurface: Surface?
}

interface IFrameSource {
  + getFrame(buffer: ByteBuffer): Frame
}

interface IVideoSource {
}

class CameraSource {
}

CameraSource <|.. ISurfaceSource
CameraSource <|.. IVideoSource

interface IAudioSource
IAudioSource <|.. IFrameSource

class MicrophoneSource {
}

MicrophoneSource <|.. IAudioSource
@enduml
-->

To create a new audio source, implements a `IAudioSource`. It inherits from `IFrameSource`.

To create a new video source, implements a `IVideoSource` and `IFrameSource`
or `ISurfaceSource`. Always prefer to use a video source as a `Surface` source if it is possible.
`IFrameSource` has missing features and is not be stable.

### Encoders

The only encoder is based on Android `MediaCodec` API. It implements the `IEncoder` interface.

<!--
@startuml
interface IEncoder {
}
@enduml
-->

## Endpoints

Endpoints implement the `IEndpoint` interface.

### Available endpoints

* Composite endpoints:
    - `CompositeEndpoint`: An endpoint that is composed of a `IMuxer` and a `ISink`.
    - `RtmpEndpoint`: An `CompositeEndpoint` that streams to a RTMP server (available in RTMP
      package).
    - `SrtEndpoint`: An `CompositeEndpoint` that streams to a SRT server in MPEG-TS (available in
      SRT package).

You can create your own endpoint by extending the `CompositeEndpoint` class.
Example, you can create a MPEG-TS endpoint that writes to a file:

```kotlin
class TsFileEndpoint : CompositeEndpoint(
    TsMuxer(),
    FileSink()
)
```

See available [muxers](#available-muxers) and [sinks](#available-sinks) below.

* Android based endpoints:
    - `MediaMuxerEndpoint`: An endpoint based on Android `MediaMuxer` API. It writes to a file or a
      content. It supports MP4, OGG, 3GP and WebM containers.

* Combine endpoints:
    - `DynamicEndpoint`: The default endpoint of the `Streamer`. It infers the endpoint from the
      `MediaDescriptor` object.
    - `CombineEndpoint`: An endpoint that combines multiple endpoints.
    - `DualEndpoint`: A `CombineEndpoint` that streams to 2 endpoints. It is useful to stream to a
      file (main) and a remote server at the same time (second).

### Creates your custom endpoint

To create a new endpoint, implements the `IEndpointInternal` class or directly use the
`CompositeEndpoint` class.

<!--
@startuml
interface IEndpointInternal {
  + open()
  + close()
  + write()
}

class CompositeEndpoint {
  + muxer: IMuxerInternal
  + sink: ISinkInternal
}
CompositeEndpoint <-- IEndpointInternal
@enduml
-->

## Muxers

Muxers implement the `IMuxer` interface.
Muxers are passed in the constructor of the `CompositeEndpoint`.

To create a new muxer, implements the `IMuxerInternal` class.

### Available muxers

* `FlvMuxer`: A muxer that packs audio and video frames to a FLV container.
* `TsMuxer`: A muxer that packs audio and video frames to a MPEG-TS container.
* `Mp4Muxer`: A muxer that packs audio and video frames to a fragmented MP4 container.

## Sinks

Sinks implement the `ISink` interface.
Sinks are passed in the constructor of the `CompositeEndpoint`.

To create a new sink, implements the `ISinkInternal` class.

### Available sinks

* `RtmpSink`: A sink that sends the container to a RTMP server (available in RTMP package).
* `SrtSink`: A sink that sends the container to a SRT server (available in SRT package).
* `FileSink`: A sink that writes the container to a file.
* `ContentSink`: A sink that writes the container to a content provider.
* `OutputStreamSink`: A sink that writes the container to an `OutputStream`.
* `ChunkedOutputStreamSink`: A sink that writes the container to an `OutputStream` in chunks (WIP).

## Element specific configuration

Each streamer element has a public interface that allows you to have access to specific information
and configuration.

### Sources specific configuration

On a streamer object, you can retrieve the source object and cast it to the specific source:
`audioSource` (an `IAudioSource`) or `videoSource` (an `IVideoSource`).

```kotlin 
val streamer = CameraSingleStreamer()
// Audio source
streamer.audioSource?.apply {
    if (this is IAudioRecordSource) {
        // Specific audio source configuration
        // Example: IAudioRecordSource specific configuration has `addEffect` method
        addEffect(AudioEffect.EFFECT_TYPE_AEC)
    }
}
// Video source
streamer.videoSource?.apply {
    if (this is ICameraSource) {
        // Specific video source configuration
        // Example: ICameraSource specific configuration has `settings` method
        settings.flash.enable = true
        settings.stabilization.enableOptical = false
        settings.stabilization.enableVideo = false
    }
}
```

Here is how to cast the sources:

* `MicrophoneSource` -> `IAudioRecordSource`
* `MediaProjectionAudioSource` -> `IMediaProjectionSource` and `IAudioRecordSource`
* `CameraSource` -> `ICameraSource`
* `MediaProjectionVideoSource` -> `IMediaProjectionSource`

### Encoders specific configuration

The encoder object is accessible from the streamer object: `audioEncoder` and `videoEncoder`
directly as`IEncoder`.

```kotlin
val streamer = CameraSingleStreamer()
// Audio encoder
streamer.audioEncoder?.apply {
    // Specific audio encoder configuration
}
// Video encoder
streamer.videoEncoder?.apply {
    // Specific video encoder configuration
    // Example: VideoEncoder specific configuration has set `bitrate` on the fly property
    bitrate = 2000000
}
```

### Endpoints specific configuration

The endpoint object is accessible from the streamer object: `endpoint` directly as `IEndpoint`.

```kotlin
val streamer = CameraSingleStreamer()
// Endpoint
streamer.endpoint.apply {
    // Specific endpoint configuration
    // Example: When the endpoint contains an SRT sink, you can get SRT statistics
    if (metrics is Stats) {
        Log.i(TAG, "Metrics: $metrics")
    }
    // Example: When the endpoint is an `ICompositeEndpoint`, you can get the muxer and sink
    if (this is ICompositeEndpoint) {
        muxer.apply {
            // Specific muxer configuration
        }
        sink.apply {
            // Specific sink configuration
        }
    }
}
```
  



