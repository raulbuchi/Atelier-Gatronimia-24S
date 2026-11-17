package com.example.data.curriculum

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DidacticLibraryCatalog(
    val schemaVersion: String,
    val sourceEdition: String,
    val title: String,
    val description: String,
    val axes: List<DidacticAxisBrochure>
)

data class DidacticAxisBrochure(
    val id: String,
    val axisNumber: Int,
    val title: String,
    val subtitle: String,
    val estimatedReadMinutes: Int,
    val weeksCovered: List<Int>,
    val badges: List<String>,
    val overviewCards: List<DidacticOverviewCard>,
    val sections: List<DidacticSection>,
    val flashcards: List<DidacticAxisFlashcard>,
    val bibliography: List<String>
)

data class DidacticOverviewCard(
    val label: String,
    val text: String
)

data class DidacticSection(
    val id: String,
    val number: String,
    val title: String,
    val weeks: List<Int>,
    val searchKeywords: List<String>,
    val blocks: List<DidacticBlock>
)

sealed interface DidacticBlock {
    data class Paragraph(val text: String) : DidacticBlock
    data class Bullets(val items: List<String>) : DidacticBlock
    data class Table(
        val caption: String,
        val headers: List<String>,
        val rows: List<List<String>>
    ) : DidacticBlock
    data class Callout(
        val variant: String, // "critical" | "mep" | "troubleshooting" | "tip"
        val title: String,
        val body: String
    ) : DidacticBlock
    data class MetricStrip(
        val title: String,
        val items: List<DidacticMetricItem>
    ) : DidacticBlock
}

data class DidacticMetricItem(
    val label: String,
    val value: String,
    val detail: String
)

data class DidacticAxisFlashcard(
    val id: String,
    val topic: String,
    val question: String,
    val answer: String
)

data class DidacticSearchHit(
    val axisNumber: Int,
    val axisTitle: String,
    val sectionId: String?,
    val sectionNumber: String,
    val sectionTitle: String,
    val hitKind: String, // "SEÇÃO" | "TABELA" | "DESTAQUE" | "FLASHCARD"
    val snippet: String
)

object DidacticLibraryLoader {
    private const val RESOURCE_FILE_NAME = "brochuras-didaticas-app_0f609061.json"

    val catalog: DidacticLibraryCatalog by lazy {
        val rawJson = readRawJson()
        parseCatalog(JSONObject(rawJson))
    }

    fun findAxis(axisNumber: Int): DidacticAxisBrochure {
        return catalog.axes.find { it.axisNumber == axisNumber }
            ?: catalog.axes.first()
    }

    fun searchAcrossLibrary(rawQuery: String, maxResults: Int = 36): List<DidacticSearchHit> {
        val q = rawQuery.trim().lowercase()
        if (q.length < 2) return emptyList()

        val hits = mutableListOf<DidacticSearchHit>()
        for (axis in catalog.axes) {
            for (section in axis.sections) {
                val keywordMatch = section.searchKeywords.firstOrNull { it.lowercase().contains(q) }
                if (section.title.lowercase().contains(q) || keywordMatch != null) {
                    hits += DidacticSearchHit(
                        axisNumber = axis.axisNumber,
                        axisTitle = axis.title,
                        sectionId = section.id,
                        sectionNumber = section.number,
                        sectionTitle = section.title,
                        hitKind = "SEÇÃO",
                        snippet = if (keywordMatch != null) {
                            "Palavra-chave técnica: $keywordMatch • Semanas ${section.weeks.joinToString(", ") { "S%02d".format(it) }}"
                        } else {
                            "Eixo ${axis.axisNumber} • Seção ${section.number}: ${section.title}"
                        }
                    )
                }

                for (block in section.blocks) {
                    if (hits.size >= maxResults) return hits
                    when (block) {
                        is DidacticBlock.Paragraph -> {
                            if (block.text.lowercase().contains(q)) {
                                hits += DidacticSearchHit(
                                    axisNumber = axis.axisNumber,
                                    axisTitle = axis.title,
                                    sectionId = section.id,
                                    sectionNumber = section.number,
                                    sectionTitle = section.title,
                                    hitKind = "SEÇÃO",
                                    snippet = extractSnippet(block.text, q)
                                )
                            }
                        }
                        is DidacticBlock.Bullets -> {
                            val item = block.items.firstOrNull { it.lowercase().contains(q) }
                            if (item != null) {
                                hits += DidacticSearchHit(
                                    axisNumber = axis.axisNumber,
                                    axisTitle = axis.title,
                                    sectionId = section.id,
                                    sectionNumber = section.number,
                                    sectionTitle = section.title,
                                    hitKind = "SEÇÃO",
                                    snippet = extractSnippet(item, q)
                                )
                            }
                        }
                        is DidacticBlock.Table -> {
                            val matchedRow = block.rows.firstOrNull { row ->
                                row.any { cell -> cell.lowercase().contains(q) }
                            }
                            if (block.caption.lowercase().contains(q) || matchedRow != null) {
                                val rowPreview = matchedRow?.joinToString(" • ") ?: block.caption
                                hits += DidacticSearchHit(
                                    axisNumber = axis.axisNumber,
                                    axisTitle = axis.title,
                                    sectionId = section.id,
                                    sectionNumber = section.number,
                                    sectionTitle = section.title,
                                    hitKind = "TABELA",
                                    snippet = "${block.caption}: ${extractSnippet(rowPreview, q)}"
                                )
                            }
                        }
                        is DidacticBlock.Callout -> {
                            if (block.title.lowercase().contains(q) || block.body.lowercase().contains(q)) {
                                hits += DidacticSearchHit(
                                    axisNumber = axis.axisNumber,
                                    axisTitle = axis.title,
                                    sectionId = section.id,
                                    sectionNumber = section.number,
                                    sectionTitle = section.title,
                                    hitKind = "DESTAQUE",
                                    snippet = "${block.title} — ${extractSnippet(block.body, q)}"
                                )
                            }
                        }
                        is DidacticBlock.MetricStrip -> {
                            val matchedMetric = block.items.firstOrNull {
                                it.label.lowercase().contains(q) ||
                                    it.value.lowercase().contains(q) ||
                                    it.detail.lowercase().contains(q)
                            }
                            if (block.title.lowercase().contains(q) || matchedMetric != null) {
                                val detail = matchedMetric?.let { "${it.label}: ${it.value} (${it.detail})" } ?: block.title
                                hits += DidacticSearchHit(
                                    axisNumber = axis.axisNumber,
                                    axisTitle = axis.title,
                                    sectionId = section.id,
                                    sectionNumber = section.number,
                                    sectionTitle = section.title,
                                    hitKind = "DESTAQUE",
                                    snippet = detail
                                )
                            }
                        }
                    }
                }
            }

            for (fc in axis.flashcards) {
                if (hits.size >= maxResults) return hits
                if (fc.topic.lowercase().contains(q) ||
                    fc.question.lowercase().contains(q) ||
                    fc.answer.lowercase().contains(q)
                ) {
                    hits += DidacticSearchHit(
                        axisNumber = axis.axisNumber,
                        axisTitle = axis.title,
                        sectionId = null,
                        sectionNumber = "SM-2",
                        sectionTitle = "Flashcard ${fc.id} • ${fc.topic}",
                        hitKind = "FLASHCARD",
                        snippet = "${fc.question} → ${fc.answer}"
                    )
                }
            }
        }
        return hits
    }

    private fun extractSnippet(raw: String, queryLower: String): String {
        val clean = raw.replace("**", "").replace("*", "")
        val idx = clean.lowercase().indexOf(queryLower)
        if (idx < 0) return clean.take(150)
        val start = (idx - 48).coerceAtLeast(0)
        val end = (idx + queryLower.length + 92).coerceAtMost(clean.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < clean.length) "…" else ""
        return "$prefix${clean.substring(start, end)}$suffix"
    }

    private fun readRawJson(): String {
        val classLoader = DidacticLibraryLoader::class.java.classLoader
        val stream = classLoader?.getResourceAsStream(RESOURCE_FILE_NAME)
            ?: DidacticLibraryLoader::class.java.getResourceAsStream("/$RESOURCE_FILE_NAME")
        if (stream != null) {
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val candidates = listOf(
            File("public/assets/aistudio/$RESOURCE_FILE_NAME"),
            File("../public/assets/aistudio/$RESOURCE_FILE_NAME")
        )
        val found = candidates.firstOrNull { it.exists() }
            ?: error("Catálogo didático $RESOURCE_FILE_NAME não encontrado nos recursos internos do app.")
        return found.readText(Charsets.UTF_8)
    }

    private fun parseCatalog(root: JSONObject): DidacticLibraryCatalog {
        val axesArr = root.optJSONArray("axes_brochures") ?: JSONArray()
        val axes = buildList {
            for (i in 0 until axesArr.length()) {
                val ax = axesArr.getJSONObject(i)
                add(parseAxis(ax))
            }
        }
        return DidacticLibraryCatalog(
            schemaVersion = root.optString("schema_version", "1.0"),
            sourceEdition = root.optString("source_edition", "v3 (Fontes Ampliadas)"),
            title = root.optString("title", "Biblioteca Didática de Apoio — Os 6 Eixos da Gastronomia"),
            description = root.optString("description"),
            axes = axes
        )
    }

    private fun parseAxis(ax: JSONObject): DidacticAxisBrochure {
        val overviewArr = ax.optJSONArray("overview_cards") ?: JSONArray()
        val overviewCards = buildList {
            for (i in 0 until overviewArr.length()) {
                val c = overviewArr.getJSONObject(i)
                add(DidacticOverviewCard(label = c.optString("label"), text = c.optString("text")))
            }
        }

        val sectionsArr = ax.optJSONArray("sections") ?: JSONArray()
        val sections = buildList {
            for (i in 0 until sectionsArr.length()) {
                add(parseSection(sectionsArr.getJSONObject(i)))
            }
        }

        val flashcardsArr = ax.optJSONArray("flashcards") ?: JSONArray()
        val flashcards = buildList {
            for (i in 0 until flashcardsArr.length()) {
                val fc = flashcardsArr.getJSONObject(i)
                add(
                    DidacticAxisFlashcard(
                        id = fc.optString("id"),
                        topic = fc.optString("topic"),
                        question = fc.optString("q"),
                        answer = fc.optString("a")
                    )
                )
            }
        }

        return DidacticAxisBrochure(
            id = ax.optString("id"),
            axisNumber = ax.optInt("axis_number", 1),
            title = ax.optString("title"),
            subtitle = ax.optString("subtitle"),
            estimatedReadMinutes = ax.optInt("estimated_read_minutes", 20),
            weeksCovered = ax.optJSONArray("weeks_covered").toIntList(),
            badges = ax.optJSONArray("badges").toStringList(),
            overviewCards = overviewCards,
            sections = sections,
            flashcards = flashcards,
            bibliography = ax.optJSONArray("bibliography").toStringList()
        )
    }

    private fun parseSection(sec: JSONObject): DidacticSection {
        val blocksArr = sec.optJSONArray("blocks") ?: JSONArray()
        val blocks = buildList {
            for (i in 0 until blocksArr.length()) {
                parseBlock(blocksArr.getJSONObject(i))?.let { add(it) }
            }
        }
        return DidacticSection(
            id = sec.optString("id"),
            number = sec.optString("number"),
            title = sec.optString("title"),
            weeks = sec.optJSONArray("weeks").toIntList(),
            searchKeywords = sec.optJSONArray("search_keywords").toStringList(),
            blocks = blocks
        )
    }

    private fun parseBlock(b: JSONObject): DidacticBlock? {
        return when (b.optString("type")) {
            "paragraph" -> DidacticBlock.Paragraph(text = b.optString("text"))
            "bullets" -> DidacticBlock.Bullets(items = b.optJSONArray("items").toStringList())
            "table" -> {
                val rowsArr = b.optJSONArray("rows") ?: JSONArray()
                val rows = buildList {
                    for (r in 0 until rowsArr.length()) {
                        add(rowsArr.optJSONArray(r).toStringList())
                    }
                }
                DidacticBlock.Table(
                    caption = b.optString("caption"),
                    headers = b.optJSONArray("headers").toStringList(),
                    rows = rows
                )
            }
            "callout" -> DidacticBlock.Callout(
                variant = b.optString("variant", "tip"),
                title = b.optString("title"),
                body = b.optString("body")
            )
            "metric_strip" -> {
                val itemsArr = b.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (m in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(m)
                        add(
                            DidacticMetricItem(
                                label = itemObj.optString("label"),
                                value = itemObj.optString("value"),
                                detail = itemObj.optString("detail")
                            )
                        )
                    }
                }
                DidacticBlock.MetricStrip(
                    title = b.optString("title"),
                    items = items
                )
            }
            else -> null
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) add(optString(i))
        }
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) add(optInt(i))
        }
    }
}
