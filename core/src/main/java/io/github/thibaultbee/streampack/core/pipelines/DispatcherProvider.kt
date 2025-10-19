package io.github.thibaultbee.streampack.core.pipelines

import android.os.Handler
import android.os.Process
import io.github.thibaultbee.streampack.core.elements.utils.ProcessThreadPriorityValue
import io.github.thibaultbee.streampack.core.pipelines.utils.HandlerThreadExecutor
import io.github.thibaultbee.streampack.core.pipelines.utils.ThreadUtils
import io.github.thibaultbee.streampack.core.pipelines.utils.ThreadUtils.defaultVideoPriorityValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

/**
 * Provides [CoroutineDispatcher]s for audio elements.
 */
interface IAudioDispatcherProvider {
    @ProcessThreadPriorityValue
    val audioThreadPriority: Int

    val default: CoroutineDispatcher
    val io: CoroutineDispatcher

    /**
     * Creates a [CoroutineDispatcher] for audio processing.
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [CoroutineDispatcher]
     */
    fun createAudioDispatcher(numOfThread: Int, componentName: String): CoroutineDispatcher
}

/**
 * Provides [CoroutineDispatcher]s for video elements.
 */
interface IVideoDispatcherProvider {
    @ProcessThreadPriorityValue
    val videoThreadPriority: Int

    val default: CoroutineDispatcher
    val io: CoroutineDispatcher

    /**
     * Creates a [CoroutineDispatcher] for video processing.
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [CoroutineDispatcher]
     */
    fun createVideoDispatcher(numOfThread: Int, componentName: String): CoroutineDispatcher

    /**
     * Creates an [Executor] for video processing.
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [Executor]
     */
    fun createVideoExecutor(numOfThread: Int, componentName: String): ExecutorService

    /**
     * Creates an [Handler] for video processing.
     *
     * @param componentName name of the component using the dispatcher
     * @return the created [Handler]
     */
    fun createVideoHandlerExecutor(componentName: String): HandlerThreadExecutor
}

/**
 * Provides [CoroutineDispatcher]s for pipelines and its elements.
 */
interface IDispatcherProvider : IAudioDispatcherProvider, IVideoDispatcherProvider

/**
 * A default implementation of [IDispatcherProvider].
 */
data class DispatcherProvider(
    @ProcessThreadPriorityValue override val audioThreadPriority: Int = Process.THREAD_PRIORITY_AUDIO,
    @ProcessThreadPriorityValue override val videoThreadPriority: Int = defaultVideoPriorityValue
) : IDispatcherProvider {
    /**
     * The default dispatcher for CPU intensive tasks.
     */
    override val default = Dispatchers.Default

    /**
     * The IO dispatcher for IO intensive tasks (reading/writing files, network...).
     */
    override val io = Dispatchers.IO

    /**
     * Creates a [CoroutineDispatcher] for audio processing with [audioThreadPriority].
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [CoroutineDispatcher]
     */
    override fun createAudioDispatcher(numOfThread: Int, componentName: String) =
        ThreadUtils.newFixedThreadPool(
            numOfThread,
            ThreadUtils.THREAD_NAME_AUDIO_PREFIX + componentName,
            audioThreadPriority
        ).asCoroutineDispatcher()

    /**
     * Creates a [CoroutineDispatcher] for video processing with [videoThreadPriority].
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [CoroutineDispatcher]
     */
    override fun createVideoDispatcher(numOfThread: Int, componentName: String) =
        createVideoExecutor(numOfThread, componentName).asCoroutineDispatcher()


    /**
     * Creates an [Executor] for video processing with [videoThreadPriority].
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [Executor]
     */
    override fun createVideoExecutor(numOfThread: Int, componentName: String) =
        ThreadUtils.newFixedThreadPool(
            numOfThread,
            ThreadUtils.THREAD_NAME_VIDEO_PREFIX + componentName,
            videoThreadPriority
        )

    /**
     * Creates an [Handler] for video processing with [videoThreadPriority].
     *
     * @param componentName name of the component using the dispatcher
     * @return the created [Handler]
     */
    override fun createVideoHandlerExecutor(componentName: String) = HandlerThreadExecutor(
        ThreadUtils.THREAD_NAME_VIDEO_PREFIX + componentName, videoThreadPriority
    )

    companion object {
        internal const val THREAD_NAME_ENCODER_PREFIX = "encoder-"
        internal const val THREAD_NAME_ENCODING_OUTPUT_PREFIX = "encoding-output-"
        internal const val THREAD_NAME_CAMERA = "camera"
        internal const val THREAD_NAME_GL = "gl"
        internal const val THREAD_NAME_VIRTUAL_DISPLAY = "virtual-display"
    }
}