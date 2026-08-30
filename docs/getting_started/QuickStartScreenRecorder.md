# 🚀 Quick Start: Screen Recorder

## Dependencies

Add the StreamPack packages in dependency in your module `build.gradle` file:

```groovy
dependencies {
    // Replace $latest_version with the version on the Maven Central badge
    implementation 'io.github.thibaultbee.streampack:streampack-core:$latest_version'
    // For services (incl. screen capture/media projection service)
    implementation 'io.github.thibaultbee.streampack:streampack-services:$latest_version'
    // RTMP or/and SRT package according to your requirements
}
```

## Permissions

Request the required permissions in your Activity/Fragment and `AndroidManifest.xml`

**Base Permissions:**
You need to add the following permissions in your `AndroidManifest.xml`:

```xml

<manifest>
    <!-- Only for a live -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Only for a local network live (Android 37+) -->
    <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
    <!-- Only for a local record -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
</manifest>
```

!!! warning "Dangerous Permissions"
To record locally, you also need to request the following dangerous permission:
`android.permission.WRITE_EXTERNAL_STORAGE`.

!!! warning "Android 37+ Permissions"
To stream on a local network, for Android 37 and later, you also need to request the following
dangerous permission: `android.permission.ACCESS_LOCAL_NETWORK`.

**Screen Recorder Permissions:**
To use the screen recorder, you need to request the following permission:

```xml

<manifest>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <!-- Only if you have to record audio -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
</manifest>
```

You will also have to declare the `Service`,

```xml

<application>
    <!-- YourScreenRecorderService extends DefaultScreenRecorderService -->
    <service android:name=".services.MyService" android:exported="false"
        android:foregroundServiceType="mediaProjection" />
</application>
```

## Service

Create a `MyService` that extends `MediaProjectionService` (so you can customize notifications among
other things).

## Intent

Create a screen record `Intent` and requests the activity result

```kotlin
MediaProjectionUtils.createScreenCaptureIntent(context = requireContext())
```

## Start the service

```kotlin
MediaProjectionService.bindService(
    requireContext(),
    MyService::class.java,
    result.resultCode,
    result.data,
    { streamer ->
        try {
            configure(streamer)
        } catch (t: Throwable) {
            // Handle exception
        }
        startStream(streamer)
    }
)
```

For a complete example, check out
the [demos/screenrecorder](https://github.com/ThibaultBee/StreamPack/tree/main/demos/screenrecorder)
directory .