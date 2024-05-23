package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.models

import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.internal.utils.extensions.toInt
import java.nio.ByteBuffer
import kotlin.experimental.and

data class SampleFlags(
    val isLeading: IsLeading = IsLeading.UNKNOWN,
    val dependsOn: SampleDependsOn = SampleDependsOn.UNKNOWN,
    val isDependedOn: SampleIsDependedOn = SampleIsDependedOn.UNKNOWN,
    val hasRedundancy: HasRedundancy = HasRedundancy.UNKNOWN,
    val paddingValue: Byte = 0,
    val isNonSyncSample: Boolean,
    val degradationPriority: Short = 0
)

fun ByteBuffer.putInt(f: SampleFlags) {
    this.put(((f.isLeading.value and 0x3) shl 2) or ((f.dependsOn.value and 0x3).toInt()))
    this.put(
        ((f.isDependedOn.value and 0x3) shl 6)
                or ((f.hasRedundancy.value and 0x3) shl 4)
                or ((f.paddingValue and 0x7) shl 1)
                or f.isNonSyncSample.toInt()
    )
    this.putShort(f.degradationPriority)
}


enum class IsLeading(val value: Byte) {
    UNKNOWN(0),
    LEADING_SAMPLE_WITH_DEPENDENCY(1),
    NOT_A_LEADING_SAMPLE(2),
    LEADING_SAMPLE_WITHOUT_DEPENDENCY(3)
}

enum class SampleDependsOn(val value: Byte) {
    UNKNOWN(0),
    OTHERS(1),
    NO_OTHER(2)
}

enum class SampleIsDependedOn(val value: Byte) {
    UNKNOWN(0),
    THIS_ONE(1),
    NOT_THIS_ONE(2)
}

enum class HasRedundancy(val value: Byte) {
    UNKNOWN(0),
    REDUNDANT_CODING(1),
    NO_REDUNDANT_CODING(2)
}