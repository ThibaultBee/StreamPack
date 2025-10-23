package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoints
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.TsMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import kotlinx.coroutines.CoroutineDispatcher

object Endpoints {
    /**
     * Creates an endpoint for RTMP (with a FLV muxer)
     */
    internal fun createRtmpEndpoint(
        defaultDispatcher: CoroutineDispatcher,
        ioDispatcher: CoroutineDispatcher
    ): IEndpointInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints.RtmpEndpoint")
            clazz.getConstructor(CoroutineDispatcher::class.java, CoroutineDispatcher::class.java)
                .newInstance(defaultDispatcher, ioDispatcher) as IEndpointInternal
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the RTMP extension.
            throw ClassNotFoundException(
                "Attempting to stream RTMP stream without depending on the RTMP extension",
                e
            )
        } catch (t: Throwable) {
            // The RTMP extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating RTMP extension", t)
        }
    }

    /**
     * Creates an endpoint for FLV File
     */
    internal fun createFlvFileEndpoint(
        defaultDispatcher: CoroutineDispatcher,
        ioDispatcher: CoroutineDispatcher
    ): IEndpointInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.flv.elements.endpoints.FlvFileEndpoint")
            clazz.getConstructor(CoroutineDispatcher::class.java, CoroutineDispatcher::class.java)
                .newInstance(defaultDispatcher, ioDispatcher) as IEndpointInternal
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the FLV extension.
            throw ClassNotFoundException(
                "Attempting to stream FLV stream without depending on the FLV extension",
                e
            )
        } catch (t: Throwable) {
            // The FLV extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FLV file extension", t)
        }
    }


    /**
     * Creates an endpoint for FLV File
     */
    internal fun createFlvContentEndpoint(
        context: Context,
        defaultDispatcher: CoroutineDispatcher,
        ioDispatcher: CoroutineDispatcher
    ): IEndpointInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.flv.elements.endpoints.FlvContentEndpoint")
            clazz.getConstructor(
                Context::class.java,
                CoroutineDispatcher::class.java,
                CoroutineDispatcher::class.java
            )
                .newInstance(context, defaultDispatcher, ioDispatcher) as IEndpointInternal
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the FLV extension.
            throw ClassNotFoundException(
                "Attempting to stream FLV stream without depending on the FLV extension",
                e
            )
        } catch (t: Throwable) {
            // The FLV extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FLV content extension", t)
        }
    }

    /**
     * Creates an endpoint for SRT (with a TS muxer)
     */
    internal fun createSrtEndpoint(
        serviceInfo: TSServiceInfo?,
        coroutineDispatcher: CoroutineDispatcher
    ): IEndpointInternal {
        val sink = CompositeEndpoints.createSrtSink(coroutineDispatcher)
        val muxer = TsMuxer()
        if (serviceInfo != null) {
            muxer.addService(serviceInfo)
        }
        return CompositeEndpoint(muxer, sink)
    }
}
