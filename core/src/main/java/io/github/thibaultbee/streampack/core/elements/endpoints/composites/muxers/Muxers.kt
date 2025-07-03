package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers

object Muxers {
    /**
     * Creates a FLV muxer.
     */
    internal fun createFlvMuxer(): IMuxerInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxer")
            clazz.getConstructor().newInstance() as IMuxerInternal
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the RTMP extension.
            throw ClassNotFoundException(
                "Attempting to use FLV muxer without depending on the FLV extension",
                e
            )
        } catch (t: Throwable) {
            // The RTMP extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FLV extension", t)
        }
    }
}