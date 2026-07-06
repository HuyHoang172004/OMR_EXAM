package exam.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.opencv.android.OpenCVLoader
import org.tensorflow.lite.Interpreter
import java.io.OutputStream
import java.text.DecimalFormat
import java.util.Locale

abstract class BaseOmrActivity : AppCompatActivity() {
    protected lateinit var yolo: YoloDetector
    protected lateinit var classifier: EfficientNetClassifier
    protected lateinit var processor: OmrProcessor

    protected data class StudentAggregate(
        val sbd: String,
        var totalScore: Float,
        var totalMax: Float,
        val pages: MutableSet<Int>
    )

    protected data class ReportRow(
        val sbd: String,
        val totalScore: Float,
        val totalMax: Float,
        val status: String
    )

    protected data class ParsedName(val sbd: String, val examId: String?, val page: Int?)

    protected fun initModels() {
        OpenCVLoader.initDebug()
        val yoloInterpreter = Interpreter(
            ModelLoader.loadMappedFile(this, "weights/best_float32.tflite")
        )
        val effInterpreter = Interpreter(
            ModelLoader.loadMappedFile(this, "weights/efficientnetb0.tflite")
        )

        yolo = YoloDetector(yoloInterpreter)
        classifier = EfficientNetClassifier(effInterpreter)
        processor = OmrProcessor(yolo, classifier)
    }

    protected fun parseStudentInfo(fileName: String): ParsedName? {
        val base = fileName.substringBeforeLast('.')
        val parts = base.split('_')
        if (parts.size < 3) return null
        val sbd = parts[0].trim().ifEmpty { return null }
        val examId = parts[1].trim().ifEmpty { null } ?: parts[1].trim()
        val page = parts[2].toIntOrNull()
        return ParsedName(sbd, examId, page)
    }

    protected fun parseConfig(keyStrInput: String, structStrInput: String): TemplateConfig {
        val keyStr = keyStrInput.trim()
        val structStr = structStrInput.trim()

        if (keyStr.isEmpty() || structStr.isEmpty()) throw Exception("Keys or Structure empty")

        val structure = structStr.split(",").map { it.trim().toInt() }
        val answerKey = mutableMapOf<String, String>()

        val questionPrefixes = (1..structure.size).map { "Q$it" }
        if (keyStr.length != structure.size) {
            throw Exception("Answer key length (${keyStr.length}) != questions count (${structure.size})")
        }

        keyStr.forEachIndexed { i, char ->
            answerKey[questionPrefixes[i]] = char.toString()
        }

        return TemplateConfig(structure, answerKey)
    }

    protected fun buildReportRows(
        studentResults: Map<String, StudentAggregate>,
        requiredPages: Set<Int>
    ): List<ReportRow> {
        return studentResults.values
            .sortedBy { it.sbd }
            .map { agg ->
                val isValid = requiredPages.isEmpty() || agg.pages.containsAll(requiredPages)
                val status = if (isValid) "Hop le" else "Thieu trang"
                ReportRow(agg.sbd, agg.totalScore, agg.totalMax, status)
            }
    }

    protected fun formatReportTable(rows: List<ReportRow>): String {
        val df = DecimalFormat("0.##")
        val sbdWidth = maxOf(3, rows.maxOf { it.sbd.length })
        val scoreWidth = maxOf(4, rows.maxOf { df.format(it.totalScore).length })
        val maxWidth = maxOf(3, rows.maxOf { df.format(it.totalMax).length })
        val statusWidth = maxOf(10, rows.maxOf { it.status.length })

        val header = String.format(
            Locale.US,
            "%-${sbdWidth}s | %-${scoreWidth}s | %-${maxWidth}s | %-${statusWidth}s",
            "SBD", "Diem", "Toi da", "Trang thai"
        )
        val sep = "-".repeat(header.length)

        val body = rows.joinToString("\n") { row ->
            String.format(
                Locale.US,
                "%-${sbdWidth}s | %-${scoreWidth}s | %-${maxWidth}s | %-${statusWidth}s",
                row.sbd,
                df.format(row.totalScore),
                df.format(row.totalMax),
                row.status
            )
        }

        return "$header\n$sep\n$body\n"
    }

    protected fun exportReportXlsx(rows: List<ReportRow>): String? {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Report")
        val df = DecimalFormat("0.##")

        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("SBD")
        header.createCell(1).setCellValue("Diem Tong")
        header.createCell(2).setCellValue("Diem Toi Da")
        header.createCell(3).setCellValue("Trang Thai")

        rows.forEachIndexed { index, row ->
            val r = sheet.createRow(index + 1)
            r.createCell(0).setCellValue(row.sbd)
            r.createCell(1).setCellValue(df.format(row.totalScore))
            r.createCell(2).setCellValue(df.format(row.totalMax))
            r.createCell(3).setCellValue(row.status)
        }

        val fileName = "omr_report_${System.currentTimeMillis()}.xlsx"
        val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return try {
            contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                workbook.use { it.write(out) }
            }
            "Downloads/$fileName"
        } catch (e: Exception) {
            null
        }
    }

    protected fun loadBitmap(uri: Uri): Bitmap {
        contentResolver.openInputStream(uri).use { input ->
            return BitmapFactory.decodeStream(input)
        }
    }

    protected fun getDisplayName(uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
