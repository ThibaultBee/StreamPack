# 📡 Endpoint Configuration

## Setting the Endpoint

By default, the `Streamer` supports all StreamPack output protocols thanks to the `DynamicEndpoint`. It infers the endpoint type dynamically from the `MediaDescriptor` you provide.

However, if you want to limit the supported protocols, or need to use a specific endpoint (for example, to explicitly write an FLV file or stream over SRT), you can explicitly set it by passing an `EndpointFactory` directly to the `Streamer` constructor.

```kotlin
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.FlvFileEndpointFactory
import io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints.RtmpEndpointFactory

// For single streamer (or helpers like cameraSingleStreamer)
val streamer = cameraSingleStreamer(
    context = context,
    endpointFactory = FlvFileEndpointFactory() 
)

// For dual streamer
val dualStreamer = DualStreamer(
    context,
    firstEndpointFactory = RtmpEndpointFactory(),
    secondEndpointFactory = FlvFileEndpointFactory()
)

// For streamer pipeline
val streamerPipeline = StreamerPipeline(context)
streamerPipeline.createEncodingOutput(
    endpointFactory = RtmpEndpointFactory()
)
```

## Available Endpoints

* Composite endpoints:
    - [`CompositeEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites/-composite-endpoint/index.html): An endpoint that is composed of a `IMuxer` and a [`ISink`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks/-i-sink/index.html).
    - [`SrtEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-extension-srt/io.github.thibaultbee.streampack.ext.srt.elements.endpoints/-srt-endpoint/index.html): An [`CompositeEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites/-composite-endpoint/index.html) that streams to a SRT server in MPEG-TS (available in
      SRT package).
* Other endpoints:
    - [`RtmpEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-extension-rtmp/io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints/-rtmp-endpoint/index.html): An endpoint that streams directly to a RTMP server (available in RTMP package).
    - [`FlvEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-extension-flv/io.github.thibaultbee.streampack.ext.flv.elements.endpoints/-flv-endpoint/index.html): An endpoint that writes an FLV stream directly to a file ([`FlvFileEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-extension-flv/io.github.thibaultbee.streampack.ext.flv.elements.endpoints/-flv-file-endpoint/index.html)) or content URI ([`FlvContentEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-extension-flv/io.github.thibaultbee.streampack.ext.flv.elements.endpoints/-flv-content-endpoint/index.html)) without using a composite muxer/sink (available in the FLV package).

You can create your own endpoint by extending the [`CompositeEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites/-composite-endpoint/index.html) class.
Example, you can create a MPEG-TS endpoint that writes to a file:

```kotlin
class TsFileEndpoint : CompositeEndpoint(
    TsMuxer(),
    FileSink()
)
```

See available [muxers](#available-muxers) and [sinks](#available-sinks) below.

* Android based endpoints:
    - [`MediaMuxerEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-media-muxer-endpoint/index.html): An endpoint based on Android `MediaMuxer` API. It writes to a file or a
      content. It supports MP4, OGG, 3GP and WebM containers.

* Combine endpoints:
    - [`DynamicEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-dynamic-endpoint/index.html): The default endpoint of the `Streamer`. It infers the endpoint from the
      [`MediaDescriptor`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.configuration.mediadescriptor/-media-descriptor/index.html) object.
    - [`CombineEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-combine-endpoint/index.html): An endpoint that combines multiple endpoints.
    - [`DualEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-dual-endpoint/index.html): A [`CombineEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-combine-endpoint/index.html) that streams to 2 endpoints. It is useful to stream to a
      file (main) and a remote server at the same time (second).

!!! note "If you need to stream to an endpoint that isn't provided out of the box, check out the [Creating Custom Endpoints](../use_cases/CustomEndpoints.md) guide."

## Available muxers

* [`TsMuxer`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts/-ts-muxer/index.html): A muxer that packs audio and video frames to a MPEG-TS container.
* [`Mp4Muxer`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4/-mp4-muxer/index.html): A muxer that packs audio and video frames to a fragmented MP4 container (WIP).

## Available sinks

* [`SrtSink`](https://thibaultbee.github.io/StreamPack/api/streampack-extension-srt/io.github.thibaultbee.streampack.ext.srt.elements.endpoints.composites.sinks/-srt-sink/index.html): A sink that sends the container to a SRT server (available in SRT package).
* [`FileSink`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks/-file-sink/index.html): A sink that writes the container to a file.
* [`ContentSink`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks/-content-sink/index.html): A sink that writes the container to a content provider.
* [`OutputStreamSink`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks/-output-stream-sink/index.html): A sink that writes the container to an `OutputStream`.
* `ChunkedOutputStreamSink`: A sink that writes the container to an `OutputStream` in chunks (WIP).

## On-the-fly control

The endpoint object is accessible from the streamer object: `endpoint` directly as [`IEndpoint`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.elements.endpoints/-i-endpoint/index.html). This allows you to have access to specific endpoint information and configuration on the fly while streaming.

```kotlin
val streamer = cameraSingleStreamer()

// Endpoint
streamer.endpoint.apply {
    // Specific endpoint configuration
    
    // Example: When the endpoint supports metrics, you can get them
    if (this is WithEndpointMetrics<*>) {
        Log.i(TAG, "Metrics: ${this.metrics}")
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
