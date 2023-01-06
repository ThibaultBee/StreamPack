Change Log
==========

Version 2.5.2
-------------

## API Changes:

- Rename `gopSize` to `gopDuration` in `VideoConfig` class.

## Features:

- RTMP: force synchronisation of audio and video frames.

## Bug fixes:

- Fixed a crash when the microphone is muted.

Version 2.5.1
-------------

## API Changes:

- `StreamerSurfaceView` has been renamed `PreviewView` and it is not longer a `SurfaceView`

Version 2.5.0
-------------

## API Changes:

- The logger is now a static class `Logger`. You still can set the `ILogger` implementation
  with `Logger.setLogger()`.

## Features:

- Add a new view that simplify StreamPack integration. It supports zoom on pinch and focus on tap.
  See `StreamerSurfaceView`.
- Add a `gopSize` parameter in `VideoConfig` to set the keyframe interval.
- Add an API for external cameras.
- In camera settings, add a zoom on pinch and a focus on tap API.
- Add encoder information in `AudioConfig` and `VideoConfig`.
- srt: `connect(String)` API supports URI with query parameters: `streamId` and `passphrase`.
  Example: `srt://server:port?streamId=myStreamId&passphrase=myPassphrase`.

## Bug fixes:

- Fixed a crash when created a MediaCodec on few Samsung devices.
- Fixed services notification for Android 13.

Version 2.4.2
-------------

## Experimental:

- Add HEVC for RTMP

## Bug fixes:

- flv: First frame timestamp must be at 0

Version 2.4.1
-------------

## Features:

- Add sources in release packages

## Bug fixes:

- Fix a crash in rtmpdroid in stopStream (in nativeClose)

Version 2.4.0
-------------

## Features:

- The screen recorder services have been moved to the library (instead of the example)
- Add zoom ratio API support for Android < R
- Introducing a life cycle observer for streamers called `StreamerLifeCycleObserver`
- Overload camera streamers' `startPreview` API to take a `SurfaceView` or a `TextureView` as input
  parameter
- Add `isConnected` field in live streamers

## Bug fixes:

- Clamp camera range settings: zoom, exposure,...

Version 2.3.3
-------------

## Features:

- srt: Update to srt 1.5.0

## Bug fixes:

- Fix ANR when connecting to a slow network

Version 2.3.2
-------------

## Features:

- Add apks in the build CI workflow
- Allow to set camera when camera has not been started

## Bug fixes:

- Multiple fixes on FLV/RTMP
- demo-screenrecorder: do not set profile nor level

Version 2.3.1
-------------

## Bug fixes:

- Camera streamer: fix the stream after a `stopPreview`
- Live streamer: disconnect if an exception happens in `startStreaming(String)`
- Fix log/exception messages

Version 2.3.0
-------------

## Features:

- Add support for OPUS audio codec (TS File streamer and SRT streamer only)

## Bug fixes:

- Obfuscation of RTMP extension

Version 2.2.0
-------------

## Features:

- Add `configure` for audio only and for video only
- Disable ringtones and alerts when camera is in streaming mode

Version 2.1.0
-------------

## Features:

- `FileStreamers` also takes an `OutputStream` as a parameter (as well as a `File`)

## Bug fixes:

- Send a `onLost` event when the RTMP connection is lost
- Use initial connection listener
- Fix FLV header for `FileStreamers` (wasn't written correctly)

## Other changes:

- Add github actions for Android instrumented test

Version 2.0.0
-------------

## API changes:

- Package has been moved to maven central and rename from `com.github.thibaultbee`
  to `io.github.thibaultbee`
- Splits `StreamPack` in multiple libraries:
    - `core`: main functionalities
    - `extension/srt`: SRT based streamers
    - `extension/rtmp`: RTMP based streamers
- Error and connection listeners are now available in `Streamers` constructors
- `Builder` have been removed in favor of Kotlin default parameters
- `ConfigurationHelper` are accessible through the `helper` field of `Streamers`

## Features:

- Adds support for RTMP with the `Streamers`: `AudioOnlyRtmpLiveStreamer`, `CameraRtmpLiveStreamer`
  and `ScreenRecorderRtmpLiveStreamer`
- Video sources can be operated in `ByteBuffer` mode as well as in `Surface` mode

## Bug fixes:

- You can call `configure` multiple times

Version 1.4.0
-------------

## API changes:

- Zoom bas been moved to `streamer.settings.camera`
- `audioBitrate` and `videoBitrate` have been moved respectively
  to `streamer.settings.audio.bitrate` and `streamer.settings.video.bitrate`

## Features:

- Introducing new surface to simplify usage: `AutoFitSurfaceView`
- Introducing camera settings for: auto white balance, focus, zoom, exposure, stabilization...
- Introducing an API to mute/unmute audio: `streamer.settings.audio.isMuted`
- Camera does not restart on `stopStream` anymore
- Refactor `app` sample
- Remove `jcenter` as a dependencies repository

## Bug fixes:

- Fix screen recorder display on stream part
- Fix camera portrait aspect ratio on stream part

Version 1.3.0
-------------

## Features:

- Introducing new streamers:
    - [ScreenRecorderSrtLiveStreamer] for screen sharing. A new sample application has been
      developed: have a look at `screenrecorder\` folder.
    - [AudioOnlySrtLiveStreamer] and [AudioOnlyTsFileStreamer] to record audio only
- Video orientation could be in portrait or/and in landscape. Use `Activity.requestedOrientation` to
  lock orientation in portrait or landscape.
- Add `isFrameRateSupported` API so you can check that camera supports current configured frame
  rate. Use it if you need to change the current camera.

Version 1.2.0
-------------

## Features:

- Add HEVC support
- Add API to check cameras orientation
- You can set video framerate to 60 FPS on sample if it is supported by your device and cameras.

Version 1.1.0
-------------

## Experimental:

- Add a bitrate regulation mechanism. A default bitrate regulator is provided but you can implement
  a custom bitrate regulator.

## Bug fixes:

- Do not create a tmp file each time a `FileWriter` is instantiated

## Features:

- Add a SRT passphrase set/get API
- Add an API to enable/disable audio effects: a noise suppressor and an echo canceler (
  check `AudioConfig` and `AudioConfig.Builder()`)

## API changes:

- `CaptureSrtLiveStreamer` `streamId` behavior has been changed. Set `streamId` field each time you
  try a new connection

Version 1.0.0
-------------

## Bug fixes:

- App: catch exception on stopStream, stopPreview, release,... to avoid crash when a streamer cannot
  be created.

## Features:

- Add a SRT stream Id set/get API
- Add Audio and Video configuration builder API
- Add streamers builder
- Add a configuration helper `CameraStreamerConfigurationHelper` for `BaseCameraStreamer`. It
  replaces `CodecUtils` and most configuration classes.

## API changes:

- `BaseCaptureStreamer` has been renamed `BaseCameraStreamer` (`CaptureSrtLiveStreamer`
  -> `CameraSrtLiveStreamer`,...)

Version 0.8.0
-------------

## Bug fixes:

- Fix [issue 11](https://github.com/ThibaultBee/StreamPack/issues/11): Crash on display orientation
  on Android 11.
- Fix timestamps when camera timestamp source
  is [SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME)
- Fix microphone timestamp source on Android >= N
- Fix TS packet when stuffing length == 1

## Features:

- Dispatch SRT connect API with kotlin coroutines
- Add camera torch switch on/off

## API changes:

- `CaptureSrtLiveStreamer` `connect` and `startStream``must be called in a kotlin coroutine.

Version 0.7.0
-------------

Initial release

