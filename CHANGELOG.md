Change Log
==========

Version 0.8.0
-------------

## Bug fixes:
- Fix [issue 11](https://github.com/ThibaultBee/StreamPack/issues/11): Crash on display orientation on Android 11.
- Fix timestamps when camera timestamp source is [SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME)
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

