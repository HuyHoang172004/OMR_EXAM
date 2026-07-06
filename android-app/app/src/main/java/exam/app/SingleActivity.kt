package exam.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleActivity : BaseOmrActivity() {
    private lateinit var editAnswerKey: EditText
    private lateinit var editStructure: EditText
    private lateinit var imgTemplate: ImageView
    private lateinit var txtTemplateName: TextView
    private lateinit var txtSummary: TextView
    private lateinit var txtDetails: TextView
    private lateinit var btnToggleDetails: Button

    private val examUris: MutableList<Uri> = mutableListOf()
    private var templateUri: Uri? = null
    private var layoutCache: List<Pair<String, android.graphics.RectF>>? = null

    private lateinit var imageAdapter: ImageListAdapter

    private val pickTemplate = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            templateUri = uri
            val name = getDisplayName(uri) ?: uri.lastPathSegment ?: "(template)"
            txtTemplateName.text = "Template: $name"
            imgTemplate.setImageBitmap(loadBitmap(uri))
            layoutCache = null
        }
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            examUris.clear()
            examUris.addAll(uris)
            imageAdapter.setItems(examUris)
        }
    }

    private val captureImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uris = result.data?.getStringArrayListExtra("capturedUris")
                ?.map { Uri.parse(it) }
                ?.filterNotNull()
                ?: emptyList()
            if (uris.isNotEmpty()) {
                examUris.clear()
                examUris.addAll(uris)
                imageAdapter.setItems(examUris)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single)

        initModels()

        editAnswerKey = findViewById(R.id.editAnswerKey)
        editStructure = findViewById(R.id.editStructure)
        imgTemplate = findViewById(R.id.imgTemplate)
        txtTemplateName = findViewById(R.id.txtTemplateName)
        txtSummary = findViewById(R.id.txtSummary)
        txtDetails = findViewById(R.id.txtDetails)
        btnToggleDetails = findViewById(R.id.btnToggleDetails)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnPickTemplate = findViewById<Button>(R.id.btnPickTemplate)
        val btnRemoveTemplate = findViewById<Button>(R.id.btnRemoveTemplate)
        val btnOpenCamera = findViewById<Button>(R.id.btnOpenCamera)
        val btnPickImages = findViewById<Button>(R.id.btnPickImages)
        val btnRun = findViewById<Button>(R.id.btnRun)

        val listImages = findViewById<RecyclerView>(R.id.listImages)
        imageAdapter = ImageListAdapter(this) { index ->
            if (index in examUris.indices) {
                examUris.removeAt(index)
                imageAdapter.setItems(examUris)
            }
        }
        listImages.layoutManager = LinearLayoutManager(this)
        listImages.adapter = imageAdapter

        btnBack.setOnClickListener { finish() }
        btnPickTemplate.setOnClickListener { pickTemplate.launch("image/*") }
        btnRemoveTemplate.setOnClickListener { clearTemplate() }
        btnOpenCamera.setOnClickListener { openCaptureScreen() }
        btnPickImages.setOnClickListener { pickImages.launch("image/*") }
        btnRun.setOnClickListener { runSinglePipeline() }

        btnToggleDetails.setOnClickListener { toggleDetails() }
        refreshSummary("Chua co ket qua")
    }

    private fun openCaptureScreen() {
        captureImages.launch(Intent(this, CaptureActivity::class.java))
    }

    private fun clearTemplate() {
        templateUri = null
        txtTemplateName.text = "Template: (none)"
        imgTemplate.setImageDrawable(null)
        layoutCache = null
    }

    private fun toggleDetails() {
        if (txtDetails.visibility == View.VISIBLE) {
            txtDetails.visibility = View.GONE
            btnToggleDetails.text = "Show details"
        } else {
            txtDetails.visibility = View.VISIBLE
            btnToggleDetails.text = "Hide details"
        }
    }

    private fun refreshSummary(text: String) {
        txtSummary.text = text
    }

    private fun runSinglePipeline() {
        val template = templateUri
        if (template == null || examUris.isEmpty()) {
            refreshSummary("Thieu template hoac anh bai cham")
            return
        }

        val config = try {
            parseConfig(editAnswerKey.text.toString(), editStructure.text.toString())
        } catch (e: Exception) {
            refreshSummary("Config error: ${e.message}")
            return
        }

        lifecycleScope.launch {
            refreshSummary("Dang xu ly...")
            val details = StringBuilder()

            withContext(Dispatchers.Default) {
                val startTime = System.currentTimeMillis()
                val templateBitmap = loadBitmap(template)
                val layout = try {
                    layoutCache ?: processor.buildLayout(templateBitmap, config).also {
                        layoutCache = it
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        refreshSummary("Template error: ${e.message}")
                    }
                    return@withContext
                }

                val detections = yolo.detect(templateBitmap)

                details.append("DIAGNOSTIC: Template => ${detections.size} raw -> ${layout.size}\n")
                details.append("--------------------------------\n")

                val studentResults = mutableMapOf<String, StudentAggregate>()

                examUris.forEachIndexed { index, uri ->
                    val studentImg = loadBitmap(uri)
                    val fileName = getDisplayName(uri) ?: uri.lastPathSegment ?: "Img_$index"
                    val parsed = parseStudentInfo(fileName)

                    val aligned = ImageUtils.alignImages(studentImg, templateBitmap)
                    val isAligned = aligned != null
                    val finalImg = aligned ?: studentImg

                    val result = processor.gradeSheet(templateBitmap, finalImg, config, layout)

                    val sbd = parsed?.sbd ?: fileName.substringBefore('.').ifBlank { "SBD_$index" }
                    val agg = studentResults.getOrPut(sbd) {
                        StudentAggregate(sbd, 0f, 0f, mutableSetOf())
                    }
                    agg.totalScore += result.totalScore
                    agg.totalMax += result.maxScore

                    val scoreText = "${result.totalScore}/${result.maxScore}"
                    val alignStatus = if (isAligned) "OK" else "FAIL (Using raw)"
                    val confirmedCount = result.answers.values.count { it == "confirmed" }

                    details.append("${index + 1}. $fileName\n")
                    details.append("   - Score: $scoreText, Confirmed: $confirmedCount\n")
                    details.append("   - Align: $alignStatus, Layout: ${layout.size} bubbles\n\n")
                }

                val reportRows = buildReportRows(studentResults, emptySet())
                val table = if (reportRows.isNotEmpty()) {
                    formatReportTable(reportRows)
                } else {
                    "Khong co du lieu"
                }

                val elapsedMs = System.currentTimeMillis() - startTime
                val perPaperMs = if (examUris.isNotEmpty()) elapsedMs / examUris.size else 0L

                val exportPath = if (reportRows.isNotEmpty()) {
                    exportReportXlsx(reportRows)
                } else {
                    null
                }

                val summary = StringBuilder()
                summary.append(table)
                summary.append("\nTong thoi gian: ${elapsedMs} ms\n")
                summary.append("Thoi gian moi bai: ${perPaperMs} ms\n")
                if (exportPath != null) {
                    summary.append("\nXuat Excel: $exportPath\n")
                }

                withContext(Dispatchers.Main) {
                    refreshSummary(summary.toString())
                    txtDetails.text = details.toString()
                }
            }
        }
    }
}
