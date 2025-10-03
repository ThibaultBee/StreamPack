package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context

object Endpoints {
    /**
     * Creates an endpoint for RTMP (with a FLV muxer)
     */
    internal fun createRtmpEndpoint(): IEndpointInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints.RtmpEndpoint")
            clazz.getConstructor().newInstance() as IEndpointInternal
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
    internal fun createFlvFileEndpoint(): IEndpointInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.flv.elements.endpoints.FlvFileEndpoint")
            clazz.getConstructor().newInstance() as IEndpointInternal
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
    internal fun createFlvContentEndpoint(context: Context): IEndpointInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.flv.elements.endpoints.FlvContentEndpoint")
            clazz.getConstructor(Context::class.java).newInstance(context) as IEndpointInternal
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
}
