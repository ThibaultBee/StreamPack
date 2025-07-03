package io.github.thibaultbee.streampack.core.elements.endpoints

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
}
