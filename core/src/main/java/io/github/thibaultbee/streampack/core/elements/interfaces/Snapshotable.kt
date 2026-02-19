package io.github.thibaultbee.streampack.core.elements.interfaces

import android.graphics.Bitmap
import androidx.annotation.IntRange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * An interface to take a snapshot of the current video frame.
 */
interface ISnapshotable {
    /**
     * Takes a snapshot of the current video frame.
     *
     * The snapshot is returned as a [Bitmap].
     *
     * @param rotationDegrees The rotation to apply to the snapshot, in degrees. 0 means no rotation.
     * @return The snapshot as a [Bitmap].
     */
    suspend fun takeSnapshot(@IntRange(from = 0, to = 359) rotationDegrees: Int = 0): Bitmap
}

/**
 * Takes a JPEG snapshot of the current video frame.
 *
 * The snapshot is saved to the specified file.
 *
 * @param filePathString The path of the file to save the snapshot to.
 * @param quality The quality of the JPEG, from 0 to 100.
 * @param rotationDegrees The rotation to apply to the snapshot, in degrees.
 */
suspend fun ISnapshotable.takeJpegSnapshot(
    filePathString: String,
    @IntRange(from = 0, to = 100) quality: Int = 100,
    @IntRange(from = 0, to = 359) rotationDegrees: Int = 0,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) = takeJpegSnapshot(withContext(dispatcher) {
    FileOutputStream(filePathString)
}, quality, rotationDegrees)


/**
 * Takes a JPEG snapshot of the current video frame.
 *
 * The snapshot is saved to the specified file.
 *
 * @param file The file to save the snapshot to.
 * @param quality The quality of the JPEG, from 0 to 100.
 * @param rotationDegrees The rotation to apply to the snapshot, in degrees.
 */
suspend fun ISnapshotable.takeJpegSnapshot(
    file: File,
    @IntRange(from = 0, to = 100) quality: Int = 100,
    @IntRange(from = 0, to = 359) rotationDegrees: Int = 0,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) = takeJpegSnapshot(withContext(dispatcher) {
    FileOutputStream(file)
}, quality, rotationDegrees)

/**
 * Takes a snapshot of the current video frame.
 *
 * The snapshot is saved as a JPEG to the specified output stream.
 * @param outputStream The output stream to save the snapshot to.
 * @param quality The quality of the JPEG, from 0 to 100.
 * @param rotationDegrees The rotation to apply to the snapshot, in degrees.
 */
suspend fun ISnapshotable.takeJpegSnapshot(
    outputStream: OutputStream,
    @IntRange(from = 0, to = 100) quality: Int = 100,
    @IntRange(from = 0, to = 359) rotationDegrees: Int = 0
) {
    val bitmap = takeSnapshot(rotationDegrees)
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
}