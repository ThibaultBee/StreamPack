package com.github.thibaultbee.streampack.streamers.interfaces.builders

interface ILiveStreamerBuilder {
    /**
     * Set stream id.
     *
     * @param streamId string describing stream id
     */
    fun setStreamId(streamId: String): IStreamerBuilder

    /**
     * Set connection pass phrase.
     *
     * @param passPhrase pass phrase
     */
    fun setPassPhrase(passPhrase: String): IStreamerBuilder
}