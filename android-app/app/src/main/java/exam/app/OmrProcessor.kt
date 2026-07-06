package exam.app

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.abs

class OmrProcessor(
    private val yolo: YoloDetector,
    private val classifier: EfficientNetClassifier
) {
    data class GradeResult(
        val totalScore: Float,
        val maxScore: Float,
        val errors: List<String>,
        val answers: Map<String, String>
    )

    fun buildLayout(template: Bitmap, config: TemplateConfig): List<Pair<String, RectF>> {
        val targetCount = config.questionsStructure.sum()
        val confs = listOf(0.5f, 0.4f, 0.3f, 0.25f, 0.15f)
        var foundRects: List<RectF> = emptyList()

        for (conf in confs) {
            val detections = yolo.detect(template, conf)
            val rawRects = detections.map { it.rect }
            if (rawRects.isEmpty()) continue

            val filtered = filterRects(rawRects, targetCount)
            foundRects = filtered
            if (foundRects.size == targetCount) break
        }

        if (foundRects.isEmpty()) {
            throw IllegalStateException("Khong tim thay o dap an tu template")
        }

        val sorted = ImageUtils.sortRectsGrid(foundRects)

        val layout = mutableListOf<Pair<String, RectF>>()
        val questionNames = (1..config.questionsStructure.size).map { "Q$it" }
        var idx = 0
        for (qIndex in config.questionsStructure.indices) {
            val qName = questionNames[qIndex]
            val numOpts = config.questionsStructure[qIndex]
            for (k in 0 until numOpts) {
                if (idx >= sorted.size) break
                val opt = ('A'.code + k).toChar()
                layout.add("${qName}_${opt}" to sorted[idx])
                idx++
            }
        }
        return layout
    }

    fun gradeSheet(
        template: Bitmap,
        student: Bitmap,
        config: TemplateConfig,
        layout: List<Pair<String, RectF>>
    ): GradeResult {
        val aligned = ImageUtils.alignImages(student, template) ?: student

        val roiImgs = mutableListOf<Bitmap>()
        val roiKeys = mutableListOf<String>()

        for ((label, rect) in layout) {
            val roi = ImageUtils.cropRect(aligned, rect)
            if (roi != null) {
                val padded = ImageUtils.squarePad(roi)
                roiImgs.add(padded)
                roiKeys.add(label)
            }
        }

        val preds = classifier.predictBatch(roiImgs)
        val answerMap = roiKeys.mapIndexed { i, k -> k to preds[i] }.toMap()

        var totalScore = 0f
        var maxScore = 0f
        val errors = mutableListOf<String>()

        for ((qName, correctAns) in config.answerKey) {
            maxScore += 1f
            val related = answerMap.filterKeys { it.startsWith("${qName}_") }
            val confirmed = related.filterValues { it == "confirmed" }.keys
                .map { it.split("_")[1] }

            if (confirmed.size == 1) {
                if (confirmed[0] == correctAns) {
                    totalScore += 1f
                }
            } else if (confirmed.size > 1) {
                errors.add("${qName}: Chon nhieu (${confirmed.joinToString(",")})")
            }
        }

        return GradeResult(totalScore, maxScore, errors, answerMap)
    }

    private fun filterRects(rects: List<RectF>, targetCount: Int): List<RectF> {
        if (rects.isEmpty() || targetCount == 0) return emptyList()
        val areas = rects.map { it.width() * it.height() }
        val median = areas.sorted()[areas.size / 2]

        val step1 = rects.filter { r ->
            val area = r.width() * r.height()
            area > 0.4f * median && area < 2.5f * median
        }

        if (step1.isEmpty()) return rects

        val remaining = mutableListOf<RectF>()
        for (i in step1.indices) {
            val a = step1[i]
            var isContainer = false
            for (j in step1.indices) {
                if (i == j) continue
                val b = step1[j]
                if (containsRect(a, b, 5f)) {
                    isContainer = true
                    break
                }
            }
            if (!isContainer) remaining.add(a)
        }

        if (remaining.size == targetCount) return remaining
        if (remaining.size > targetCount) {
            return remaining.sortedBy { abs(it.width() * it.height() - median) }
                .take(targetCount)
        }
        if (remaining.isEmpty()) return step1
        return remaining
    }

    private fun containsRect(a: RectF, b: RectF, padding: Float): Boolean {
        return a.left < b.left + padding &&
            a.top < b.top + padding &&
            a.right > b.right - padding &&
            a.bottom > b.bottom - padding
    }
}
