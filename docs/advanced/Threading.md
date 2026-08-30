# 🧵 Threading

StreamPack is highly asynchronous and relies heavily on Kotlin Coroutines and background threads to maintain high performance and low latency. By default, StreamPack spins up dedicated threads for audio processing, video processing, OpenGL rendering, and network I/O.

## The `DispatcherProvider`

All threading and coroutine dispatchers in StreamPack are managed by the [`IDispatcherProvider`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.pipelines/-i-dispatcher-provider/index.html). 

When you instantiate a Streamer (like `cameraSingleStreamer` or `audioSingleStreamer`), a default [`DispatcherProvider`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.pipelines/-dispatcher-provider/index.html) is used under the hood. This provider automatically applies appropriate OS-level thread priorities to the different data pipelines to prevent stuttering:

* **Audio Threads**: Run with `Process.THREAD_PRIORITY_AUDIO` by default.
* **Video Threads**: Run with a default video priority (typically `Process.THREAD_PRIORITY_DISPLAY`).

### Customizing Thread Priorities

If your application has strict CPU constraints, or you need to adjust the priority of the streaming threads (for example, lowering the priority to avoid dropping UI frames in your app, or raising it to ensure the stream never stutters), you can instantiate a custom [`DispatcherProvider`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.pipelines/-dispatcher-provider/index.html) with your preferred priorities and pass it to your streamer:

```kotlin
import android.os.Process
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.streamers.single.cameraSingleStreamer

// 1. Create a custom DispatcherProvider with modified priorities
val customDispatcherProvider = DispatcherProvider(
    audioThreadPriority = Process.THREAD_PRIORITY_URGENT_AUDIO,
    videoThreadPriority = Process.THREAD_PRIORITY_MORE_FAVORABLE
)

// 2. Pass it to the streamer factory
val streamer = cameraSingleStreamer(
    context = context,
    dispatcherProvider = customDispatcherProvider
)
```

### Advanced Dispatcher Implementations

If you need complete architectural control over how threads and coroutines are executed (for example, if you want to manage your own thread pools, restrict the total number of threads globally, or inject test dispatchers for unit testing), you can completely implement the [`IDispatcherProvider`](https://thibaultbee.github.io/StreamPack/api/streampack-core/io.github.thibaultbee.streampack.core.pipelines/-i-dispatcher-provider/index.html) interface yourself:

```kotlin
class MyCustomDispatcherProvider : IDispatcherProvider {
    override val audioThreadPriority = Process.THREAD_PRIORITY_AUDIO
    override val videoThreadPriority = Process.THREAD_PRIORITY_DISPLAY

    // Core Dispatchers
    override val default = Dispatchers.Default
    override val io = Dispatchers.IO

    // Custom Thread Generators
    override fun createAudioDispatcher(numOfThread: Int, componentName: String): CoroutineDispatcher {
        // Return your custom audio coroutine dispatcher
    }

    override fun createVideoDispatcher(numOfThread: Int, componentName: String): CoroutineDispatcher {
        // Return your custom video coroutine dispatcher
    }

    // ... Implement other executor/handler methods as required by IDispatcherProvider
}
```
