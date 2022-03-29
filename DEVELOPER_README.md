# StreamPack developer guide

## Introduction

### Definitions

`Source`:
A class that represents an audio or video source. For example, a camera (`CameraCapture`), or a
microphone (`AudioCapture`).

`Encoder`:
A class that represents an audio or video encoders. Only Android MediaCodec API is used (
`MediaCodecEncoder`).

`Muxer`:
A class that packs audio and video frames to a container (FLV, MPEG-TS, MP4,...).

`Endpoint`:
The last element of a live streaming pipeline. It is responsible for sending the audio and video
streams somewhere For example, a file (`FileWriter`), or a remote RTMP server (`RtmpProducer`).

`Streamer`:
A class that represent a audio and/or video live streaming pipeline. It manages sources, encoders,
muxers, endpoints,... and have lot of tools. They are the most important class for users.

![streamer.png](https://github.com/ThibaultBee/StreamPack/tree/master/app/docs/assets/streamer.png)

`Streamer element`:
Could be a `Source`, `Encoder`, `Muxer`, or `Endpoint`. They implement the `Streamable<T>`.

`Helper`:
A class that provides a set of methods to help to `streamer` configuration such as supported
resolutions,... It comes with an instantiated `Streamer` object:

```kotlin
val helper = streamer.helper
```

They might be different for each `Streamer` object. For example, a `FlvStreamer` object will not
have the same `Helper` object as a `TsStreamer` object because FLV does not support a wide range of
codecs, audio sample rate,...

`Settings`:
A class that adds settings to the `Streamer` object. For example, you can access camera settings,
encoders bitrate,... It comes with an instantiated `Streamer` object:

```kotlin
val settings = streamer.settings
```

They might be different for each `Streamer` object. For example, only `CameraStreamer` have a camera
settings.

`Extensions`:
A library that adds new features from native libraries. It often comes with a `Streamer elements`
and specific pipelines.

## Streamers

The base streamer class is `BaseStreamer`. All other streamers inherit from it. Then 2 specifics
base streamers inherit from it:

- `BaseCameraStreamer`: A streamer that streams from a camera and microphone. It adds `startPreview`
  , `stopPreview` methods to the `Streamer` object as well as a camera settings.
- `BaseScreenRecorderStreamer`: A streamer that streams from the phone screen and microphone. It
  adds specific methods for screen recorder as a API to set activity result.

Then these base streamers are specialized for a File or for a Live:

- `BaseFileStreamer`: A streamer that streams to a file. That means you will find
  `BaseFileStreamer` for MPEG-TS and one for FLV.
- `BaseLiveStreamer`: A streamer that streams to a remote server (RTMP or SRT both in `extensions`).

## Sources

There are 2 types of sources:

- source from buffer: such as a microphone. It implements `IFrameCapture` or `IAudioCapture` for
  audio source.
- source from encoder surface (video only): when the video source can write to a `Surface`. Its
  purpose is to improve encoder performance. For example, it suits camera and screen recorder.

## Encoders

They implement the `IMuxer` interface.

## Muxers

Both `AudioMediaCodecEncoder` and `VideoMediaCodecEncoder` inherit from `MediaCodecEncoder`. They
are using Android `MediaCodec` API in asynchronous mode

## Endpoints

They implement the `IEndpoint` interface. Use specific `ILiveEndpoint` for live streaming.