package exam.app

import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class YoloDetector(
    private val interpreter: Interpreter,
    private val inputSize: Int = 640,
    private val confThreshold: Float = 0.25f
) {
    data class Detection(val rect: RectF, val score: Float)

    fun detect(bitmap: Bitmap, conf: Float? = null): List<Detection> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = bitmapToFloatBuffer(resized)
        val output = Array(1) { Array(300) { FloatArray(6) } }

        interpreter.run(input, output)

        val detections = mutableListOf<Detection>()
        val scaleX = bitmap.width.toFloat() / inputSize
        val scaleY = bitmap.height.toFloat() / inputSize
        val threshold = conf ?: confThreshold

        for (i in 0 until 300) {
            val row = output[0][i]
            val score = row[4]
            if (score < threshold) continue

            val maxCoord = max(max(row[0], row[1]), max(row[2], row[3]))
            val coordScale = if (maxCoord <= 1.5f) inputSize.toFloat() else 1.0f

            var x1 = row[0] * coordScale
            var y1 = row[1] * coordScale
            var x2 = row[2] * coordScale
            var y2 = row[3] * coordScale

            if (x2 < x1 || y2 < y1) {
                val cx = x1
                val cy = y1
                val w = x2
                val h = y2
                x1 = cx - w / 2f
                y1 = cy - h / 2f
                x2 = cx + w / 2f
                y2 = cy + h / 2f
            }

            x1 *= scaleX
            x2 *= scaleX
            y1 *= scaleY
            y2 *= scaleY

            val left = min(x1, x2)
            val top = min(y1, y2)
            val right = max(x1, x2)
            val bottom = max(y1, y2)

            detections.add(Detection(RectF(left, top, right, bottom), score))
        }

        return detections
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val inputChannels = 3
        val inputBytes = inputSize * inputSize * inputChannels * 4
        val buffer = ByteBuffer.allocateDirect(inputBytes)
        buffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixel = 0
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val v = intValues[pixel++]
                val r = (v shr 16 and 0xFF) / 255.0f
                val g = (v shr 8 and 0xFF) / 255.0f
                val b = (v and 0xFF) / 255.0f
                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }
        buffer.rewind()
        return buffer
    }
}
