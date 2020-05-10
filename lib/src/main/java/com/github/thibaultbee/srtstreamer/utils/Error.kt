package com.github.thibaultbee.srtstreamer.utils

enum class Error {
    SUCCESS,
    EOS,
    UNKNOWN,

    BAD_STATE,
    INVALID_BUFFER,
    INVALID_PARAMETER,
    INVALID_OPERATION,
    CAPACITY_IS_FULL,
    DEAD_OBJECT,

    DEVICE_ALREADY_IN_USE,
    DEVICE_MAX_IN_USE,
    DEVICE_DISABLED,

    CONNECTION_ERROR,
    TRANSMISSION_ERROR;

    companion object {
        fun valueOf(value: Int): Error? = values().find { it.ordinal == value }
    }
}