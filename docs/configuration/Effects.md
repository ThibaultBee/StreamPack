# ✨ Effects & Processing

StreamPack provides a powerful processing pipeline that allows you to apply audio and video effects to your stream.

## Audio Effects

You can apply custom audio effects to your stream by implementing either `IConsumerAudioEffect` (for read-only effects like VU meters) or `IProcessorAudioEffect` (to modify the audio buffer). 

Add your effect to the `AudioInput`'s processor:

```kotlin
val streamer = cameraSingleStreamer(context)

// 1. Create a custom audio effect
class MyVuMeterEffect : IConsumerAudioEffect {
    override fun consume(isMuted: Boolean, data: RawFrame) {
        if (isMuted) return
        // Analyze data.rawBuffer for VU meter calculation
    }

    override fun close() {
        // Cleanup resources
    }
}

// 2. Add it to the streamer's audio processor
streamer.audioInput.processor.add(MyVuMeterEffect())
```

## Video Effects (Surface Processor)

Video processing is handled by the `ISurfaceProcessorInternal`. By default, StreamPack uses the `DefaultSurfaceProcessorFactory`. 

To apply custom video effects (like OpenGL shaders, overlays, or filters), you can create your own `ISurfaceProcessorInternal` factory and pass it directly to the streamer constructor:

```kotlin
class MyCustomSurfaceProcessorFactory : ISurfaceProcessorInternal.Factory {
    override fun create(
        context: Context,
        dynamicRangeProfile: DynamicRangeProfile,
        dispatcherProvider: IVideoDispatcherProvider
    ): ISurfaceProcessorInternal {
        // Return your custom ISurfaceProcessorInternal implementation here
        // E.g., wrapping OpenGlRenderer with a custom ShaderProvider
        return MyCustomSurfaceProcessor()
    }
}

// Pass your custom surface processor factory to the streamer
val streamer = cameraSingleStreamer(
    context = context,
    surfaceProcessorFactory = MyCustomSurfaceProcessorFactory()
)
```
