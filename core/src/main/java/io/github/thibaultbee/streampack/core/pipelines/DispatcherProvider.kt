package io.github.thibaultbee.streampack.core.pipelines

import android.os.Process
import io.github.thibaultbee.streampack.core.elements.utils.ProcessThreadPriorityValue
import io.github.thibaultbee.streampack.core.pipelines.utils.ThreadUtils
import io.github.thibaultbee.streampack.core.pipelines.utils.ThreadUtils.defaultVideoPriorityValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher

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
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [CoroutineDispatcher]
     */
    fun createVideoDispatcher(numOfThread: Int, componentName: String): CoroutineDispatcher
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
     * Creates a [CoroutineDispatcher] for audio processing.
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
     * Creates a [CoroutineDispatcher] for video processing.
     *
     * @param numOfThread number of threads in the pool
     * @param componentName name of the component using the dispatcher
     * @return the created [CoroutineDispatcher]
     */
    override fun createVideoDispatcher(numOfThread: Int, componentName: String) =
        ThreadUtils.newFixedThreadPool(
            numOfThread,
            ThreadUtils.THREAD_NAME_VIDEO_PREFIX + componentName,
            videoThreadPriority
        ).asCoroutineDispatcher()

    companion object {
        internal const val THREAD_NAME_ENCODER = "encoder-"
        internal const val THREAD_NAME_ENCODING_OUTPUT = "encoding-output-"
    }
}