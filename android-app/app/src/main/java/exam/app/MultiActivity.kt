package exam.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MultiActivity : BaseOmrActivity() {
    private lateinit var pageContainer: LinearLayout
    private lateinit var txtSummary: TextView
    private lateinit var txtDetails: TextView
    private lateinit var btnToggleDetails: Button

    private val pageRows: MutableList<PageRow> = mutableListOf()
    private val examUris: MutableList<Uri> = mutableListOf()
    private val layoutCache: MutableMap<Int, List<Pair<String, android.graphics.RectF>>> = mutableMapOf()

    private lateinit var imageAdapter: ImageListAdapter
    private var pendingRow: PageRow? = null

    private data class PageRow(
        val container: View,
        val editPage: EditText,
        val editAnswerKey: EditText,
        val editStructure: EditText,
        val btnPickTemplate: Button,
        val btnRemoveTemplate: Button,
        val txtTemplateName: TextView,
        val imgTemplate: ImageView,
        var templateUri: Uri?
    )

    private val pickTemplate = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val row = pendingRow ?: return@registerForActivityResult
            val name = getDisplayName(uri) ?: uri.lastPathSegment ?: "(template)"
            row.templateUri = uri
            row.txtTemplateName.text = "Template: $name"
            row.imgTemplate.setImageBitmap(loadBitmap(uri))
            layoutCache.clear()
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
        setContentView(R.layout.activity_multi)

        initModels()

        pageContainer = findViewById(R.id.pageContainer)
        txtSummary = findViewById(R.id.txtSummary)
        txtDetails = findViewById(R.id.txtDetails)
        btnToggleDetails = findViewById(R.id.btnToggleDetails)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnAddPage = findViewById<Button>(R.id.btnAddPage)
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
        btnAddPage.setOnClickListener { addPageRow() }
        btnOpenCamera.setOnClickListener { openCaptureScreen() }
        btnPickImages.setOnClickListener { pickImages.launch("image/*") }
        btnRun.setOnClickListener { runMultiPipeline() }
        btnToggleDetails.setOnClickListener { toggleDetails() }

        if (pageRows.isEmpty()) {
            addPageRow()
        }
        refreshSummary("Chua co ket qua")
    }

    private fun addPageRow() {
        val rowView = layoutInflater.inflate(R.layout.item_page_config, pageContainer, false)
        val editPage = rowView.findViewById<EditText>(R.id.editPageNumber)
        val editAnswer = rowView.findViewById<EditText>(R.id.editAnswerKey)
        val editStruct = rowView.findViewById<EditText>(R.id.editStructure)
        val btnPick = rowView.findViewById<Button>(R.id.btnPickTemplate)
        val btnRemove = rowView.findViewById<Button>(R.id.btnRemoveTemplate)
        val txtTemplate = rowView.findViewById<TextView>(R.id.txtTemplateName)
        val imgTemplate = rowView.findViewById<ImageView>(R.id.imgTemplate)

        val row = PageRow(
            rowView,
            editPage,
            editAnswer,
            editStruct,
            btnPick,
            btnRemove,
            txtTemplate,
            imgTemplate,
            null
        )

        btnPick.setOnClickListener {
            pendingRow = row
            pickTemplate.launch("image/*")
        }

        btnRemove.setOnClickListener {
            row.templateUri = null
            row.txtTemplateName.text = "Template: (none)"
            row.imgTemplate.setImageDrawable(null)
            layoutCache.clear()
        }

        pageContainer.addView(rowView)
        pageRows.add(row)
    }

    private fun openCaptureScreen() {
        captureImages.launch(Intent(this, CaptureActivity::class.java))
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

    private data class MultiConfig(
        val configMap: Map<Int, TemplateConfig>,
        val templateBitmaps: Map<Int, Bitmap>
    )

    private fun buildMultiConfig(): MultiConfig {
        if (pageRows.isEmpty()) throw Exception("No page configs")

        val configMap = mutableMapOf<Int, TemplateConfig>()
        val templateMap = mutableMapOf<Int, Bitmap>()

        pageRows.forEachIndexed { index, row ->
            val page = row.editPage.text.toString().trim().toIntOrNull()
                ?: throw Exception("Page number invalid at row ${index + 1}")
            val answerKeyStr = row.editAnswerKey.text.toString()
            val structureStr = row.editStructure.text.toString()
            val templateUri = row.templateUri
                ?: throw Exception("Missing template at row ${index + 1}")

            if (configMap.containsKey(page)) {
                throw Exception("Duplicate page number: $page")
            }

            val config = parseConfig(answerKeyStr, structureStr)
            configMap[page] = config
            templateMap[page] = loadBitmap(templateUri)
        }

        return MultiConfig(configMap, templateMap)
    }

    private fun runMultiPipeline() {
        if (examUris.isEmpty()) {
            refreshSummary("Missing images")
            return
        }

        val multiConfig = try {
            buildMultiConfig()
        } catch (e: Exception) {
            refreshSummary("Config error: ${e.message}")
            return
        }

        val templateMap = multiConfig.templateBitmaps
        val configMap = multiConfig.configMap

        if (templateMap.isEmpty()) {
            refreshSummary("Missing templates")
            return
        }

        lifecycleScope.launch {
            refreshSummary("Dang xu ly...")
            val details = StringBuilder()
            layoutCache.clear()

            withContext(Dispatchers.Default) {
                val startTime = System.currentTimeMillis()
                val pageTemplateStats = mutableListOf<String>()
                templateMap.forEach { (page, bitmap) ->
                    val config = configMap.getValue(page)
                    val layout = try {
                        layoutCache[page] ?: processor.buildLayout(bitmap, config).also {
                            layoutCache[page] = it
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            refreshSummary("Template error (page $page): ${e.message}")
                        }
                        return@withContext
                    }
                    val detections = yolo.detect(bitmap)
                    pageTemplateStats.add("p$page: ${detections.size} raw -> ${layout.size}")
                }

                details.append("DIAGNOSTIC: Templates => ${pageTemplateStats.joinToString(" | ")}\n")
                details.append("--------------------------------\n")

                val studentResults = mutableMapOf<String, StudentAggregate>()
                val requiredPages = templateMap.keys.toSet()

                examUris.forEachIndexed { index, uri ->
                    val studentImg = loadBitmap(uri)
                    val fileName = getDisplayName(uri) ?: uri.lastPathSegment ?: "Img_$index"
                    val parsed = parseStudentInfo(fileName)
                    val page = parsed?.page
                    if (page == null || !templateMap.containsKey(page)) {
                        details.append("${index + 1}. $fileName\n")
                        details.append("   - Skipped: Missing template for page\n\n")
                        return@forEachIndexed
                    }

                    val template = templateMap.getValue(page)
                    val config = configMap.getValue(page)
                    val aligned = ImageUtils.alignImages(studentImg, template)
                    val isAligned = aligned != null
                    val finalImg = aligned ?: studentImg

                    val layout = layoutCache[page] ?: processor.buildLayout(template, config).also {
                        layoutCache[page] = it
                    }

                    val result = processor.gradeSheet(template, finalImg, config, layout)

                    val sbd = parsed?.sbd ?: fileName.substringBefore('.').ifBlank { "SBD_$index" }
                    val agg = studentResults.getOrPut(sbd) {
                        StudentAggregate(sbd, 0f, 0f, mutableSetOf())
                    }
                    agg.totalScore += result.totalScore
                    agg.totalMax += result.maxScore
                    agg.pages.add(page)

                    val scoreText = "${result.totalScore}/${result.maxScore}"
                    val alignStatus = if (isAligned) "OK" else "FAIL (Using raw)"
                    val confirmedCount = result.answers.values.count { it == "confirmed" }

                    details.append("${index + 1}. $fileName\n")
                    details.append("   - Score: $scoreText, Confirmed: $confirmedCount\n")
                    details.append("   - Align: $alignStatus, Layout: ${layout.size} bubbles, Page: $page\n\n")
                }

                val reportRows = buildReportRows(studentResults, requiredPages)
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
