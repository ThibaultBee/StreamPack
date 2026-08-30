# 📖 Glossary

* `Source`:
  A class that represents an audio or video source. For example, a camera (
  [`CameraSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.video.camera/-camera-source/index.html)), or a microphone (`AudioSource`).

* `Encoder`:
  A class that represents an audio or video encoders. Only Android MediaCodec API is
  used ([`MediaCodecEncoder`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.encoders.mediacodec/-media-codec-encoder/index.html)).

* `Endpoint`:
  The last element of a live streaming pipeline. It is responsible for handling the frames after the
  encoder.
  The endpoint could be a remote server (RTMP, SRT,...) or a file (FLV, MPEG-TS,...).
  The main endpoint is [`CompositeEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites/-composite-endpoint/index.html) that is composed of a `Muxer` and a `Sink`.

* `Muxer`:
  A process that packs audio and video frames to a container (FLV, MPEG-TS, MP4,...).
  The [`CompositeEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites/-composite-endpoint/index.html) is composed of a `IMuxer`.

* `Sink`:
  A process that sends the container to a remote server (RTMP, SRT,...) or to a file.
  The [`CompositeEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites/-composite-endpoint/index.html) is composed of a [`ISink`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks/-i-sink/index.html).

* `Streamer`:
  A class that represent a audio and/or video live streaming pipeline.
  Unless explicitly stated, the `Endpoint` is inferred from the [`MediaDescriptor`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.configuration.mediadescriptor/-media-descriptor/index.html) object thanks to
  the [`DynamicEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-dynamic-endpoint/index.html).

* `Streamer element`:
  Could be a `Source`, `Encoder`, `Muxer`, or `Endpoint`. They implement the `Streamable<T>` and
  they
  might have a public interface to access specific info.

* `Pipeline output`:
  An output of the `Streamer pipeline`. The [`EncodingPipelineOutput`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.pipelines.outputs.encoding/-encoding-pipeline-output/index.html) manages audio and video
  encoders, the `Muxer` and the `Endpoint`.

* `Streamer pipeline`:
  A class that manages one audio and one video `Source` and multiple `Pipeline outputs`.

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

* `Internal` interface and `public` interface:
  They are public to let you create your own `Source`, `Encoder`, `Muxer` or `Sink`.
  Example: `IAudioSourceInternal` is the internal interface of [`IAudioSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio/-i-audio-source/index.html).
  In other cases, use the public interface instead.
  Example: [`IAudioSource`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.sources.audio/-i-audio-source/index.html) is the public interface of `IAudioSourceInternal`.
