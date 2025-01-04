# Live and record simultaneously

Starting from version 3.0.0, you can now live stream and record at the same time. There are 2 ways
to do it:

- With the dual endpoint/single-encoder: `DualEndpoint`
- With the dual streamer/multi-encoder: `DualStreamer`

## Dual endpoint

The `DualEndpoint` is a `CombineEndpoint` that streams to 2 endpoints in a single streamer. The idea
is to that the encoded frames are pushed to 2 endpoints: one for the live stream and one for the
recording.

### Advantages

As only 2 encoders (1 for video and 1 for audio) are used, it uses less hardware resources.

### Implementation

The `DualEndpoint` has 2 endpoints: the main endpoint and the second endpoint.
Use the main endpoint for the recording and the second endpoint for the live stream.

You have to call the `open` or `startStream` of the second endpoint by yourself. The main endpoint
is started by the streamer.

```kotlin
val dualEndpoint = DualEndpoint(
    mainEndpoint = MediaMuxerEndpoint(context),
    secondEndpoint = SrtEndpoint()
)

val streamer = CameraSingleStreamer(
    context,
    internalEndpoint = dualEndpoint
)
```

## Dual streamer

The `DualStreamer` is a `Streamer` that streams to 2 independent outputs.

### Advantages

As the 2 outputs are independent, you can use different settings for each output. Like different
codecs, bitrate,...

### Implementation

Use a `DualStreamer`:

```kotlin
val streamer = DualStreamer(context)
```