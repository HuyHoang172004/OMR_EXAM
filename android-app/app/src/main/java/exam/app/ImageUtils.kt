package exam.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

object ImageUtils {
    fun squarePad(bitmap: Bitmap): Bitmap {
        val size = maxOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val left = (size - bitmap.width) / 2f
        val top = (size - bitmap.height) / 2f
        canvas.drawBitmap(bitmap, left, top, null)
        return output
    }

    fun cropRect(bitmap: Bitmap, rect: RectF): Bitmap? {
        val left = rect.left.toInt().coerceAtLeast(0)
        val top = rect.top.toInt().coerceAtLeast(0)
        val right = rect.right.toInt().coerceAtMost(bitmap.width)
        val bottom = rect.bottom.toInt().coerceAtMost(bitmap.height)
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, w, h)
    }

    fun alignImages(student: Bitmap, template: Bitmap): Bitmap? {
        val im1 = Mat()
        val im2 = Mat()
        Utils.bitmapToMat(student, im1)
        Utils.bitmapToMat(template, im2)
        Imgproc.cvtColor(im1, im1, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(im2, im2, Imgproc.COLOR_RGBA2GRAY)

        val orb = ORB.create(5000)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val desc1 = Mat()
        val desc2 = Mat()
        orb.detectAndCompute(im1, Mat(), kp1, desc1)
        orb.detectAndCompute(im2, Mat(), kp2, desc2)

        if (desc1.empty() || desc2.empty()) return null

        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
        val matches = MatOfDMatch()
        matcher.match(desc1, desc2, matches)

        val matchList = matches.toList().sortedBy { it.distance }
        if (matchList.size < 4) return null

        val keep = (matchList.size * 0.2).toInt().coerceAtLeast(4)
        val good = matchList.take(keep)

        if (good.size < 4) return null

        val points1 = ArrayList<Point>()
        val points2 = ArrayList<Point>()
        val kp1List = kp1.toList()
        val kp2List = kp2.toList()
        for (m in good) {
            points1.add(kp1List[m.queryIdx].pt)
            points2.add(kp2List[m.trainIdx].pt)
        }

        val p1 = MatOfPoint2f()
        val p2 = MatOfPoint2f()
        p1.fromList(points1)
        p2.fromList(points2)

        val h = Calib3d.findHomography(p1, p2, Calib3d.RANSAC)
        if (h.empty()) return null

        val aligned = Mat(template.height, template.width, CvType.CV_8UC4)
        Imgproc.warpPerspective(Mat().also { Utils.bitmapToMat(student, it) }, aligned, h, aligned.size())

        val out = Bitmap.createBitmap(template.width, template.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(aligned, out)
        return out
    }

    fun sortRectsGrid(rects: List<RectF>): List<RectF> {
        if (rects.isEmpty()) return emptyList()
        val heights = rects.map { it.height() }
        val avgHeight = heights.sum() / heights.size
        val thresholdY = avgHeight * 0.6f

        val sortedByY = rects.sortedBy { it.top }
        val result = mutableListOf<RectF>()
        var currentRow = mutableListOf<RectF>()
        var lastY = sortedByY.first().top

        for (rect in sortedByY) {
            val y = rect.top
            if (abs(y - lastY) <= thresholdY) {
                currentRow.add(rect)
            } else {
                currentRow = currentRow.sortedBy { it.left }.toMutableList()
                result.addAll(currentRow)
                currentRow = mutableListOf(rect)
                lastY = y
            }
        }

        if (currentRow.isNotEmpty()) {
            currentRow = currentRow.sortedBy { it.left }.toMutableList()
            result.addAll(currentRow)
        }

        return result
    }
}
