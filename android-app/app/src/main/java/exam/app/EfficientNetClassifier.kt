package exam.app

import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EfficientNetClassifier(
    private val interpreter: Interpreter
) {
    private val inputSize = 128
    private val labels = listOf("confirmed", "crossedout", "empty")
    private val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun predictBatch(rois: List<Bitmap>): List<String> {
        if (rois.isEmpty()) return emptyList()
        return rois.map { predict(it) }
    }

    fun predict(bitmap: Bitmap): String {
        val input = bitmapToNchwBuffer(bitmap)
        val output = Array(1) { FloatArray(3) }
        interpreter.run(input, output)

        val scores = output[0]
        var bestIdx = 0
        var bestScore = scores[0]
        for (i in 1 until scores.size) {
            if (scores[i] > bestScore) {
                bestScore = scores[i]
                bestIdx = i
            }
        }
        return labels[bestIdx]
    }

    private fun bitmapToNchwBuffer(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBytes = 1 * inputSize * inputSize * 3 * 4
        val buffer = ByteBuffer.allocateDirect(inputBytes)
        buffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resized.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        val planeSize = inputSize * inputSize
        val rPlane = FloatArray(planeSize)
        val gPlane = FloatArray(planeSize)
        val bPlane = FloatArray(planeSize)

        for (i in intValues.indices) {
            val pixelValue = intValues[i]
            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            rPlane[i] = (r - mean[0]) / std[0]
            gPlane[i] = (g - mean[1]) / std[1]
            bPlane[i] = (b - mean[2]) / std[2]
        }

        for (i in 0 until planeSize) buffer.putFloat(rPlane[i])
        for (i in 0 until planeSize) buffer.putFloat(gPlane[i])
        for (i in 0 until planeSize) buffer.putFloat(bPlane[i])

        buffer.rewind()
        return buffer
    }
}
