package com.pabl3st.rutapp.core.importer

import android.util.Xml
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Parser XLSX ligero — sin dependencias externas.
 * Lee hojas de un fichero .xlsx usando la spec OOXML (ZIP + XML).
 *
 * Tipos soportados: sharedString, inlineStr, número, booleano, fecha ISO.
 * Sin fórmulas (se ignora la fórmula, se usa el cached value si existe).
 *
 * FIX namespace (bug original):
 *   Android's Xml.newPullParser() con xmlns="..." en el elemento raíz puede
 *   devolver parser.name con el namespace completo p.e. "{ns}sheet" en lugar
 *   de "sheet". Todos los comparadores usan localName() para ser robustos.
 */
object XlsxParser {

    /** Resultado de parsear múltiples hojas */
    data class MultiSheetResult(val sheets: Map<String, CsvParser.ParseResult>) {
        operator fun get(name: String): CsvParser.ParseResult? = sheets[name]
        val values get() = sheets.values
    }

    // ── Helpers namespace-safe ────────────────────────────────
    // parser.name puede ser "sheet" o "{ns}sheet" dependiendo del parser.
    // localTag() normaliza siempre al nombre local.
    private fun org.xmlpull.v1.XmlPullParser.localTag(): String {
        val n = name ?: return ""
        return if (n.contains('}')) n.substringAfterLast('}') else n
    }

    // ── API pública ───────────────────────────────────────────

    /** Lee todas las hojas del XLSX. Devuelve mapa nombre → ParseResult. */
    fun parseMultiSheet(stream: InputStream): MultiSheetResult {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }

        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"]?.inputStream())

        // Resolver nombres de hojas y su orden desde workbook.xml
        // También usamos workbook.xml.rels para mapear rId → archivo real
        val sheetNames  = mutableListOf<String>()
        val sheetRIds   = mutableListOf<String>()

        entries["xl/workbook.xml"]?.inputStream()?.let { wbStream ->
            try {
                val parser = Xml.newPullParser()
                parser.setInput(wbStream, "UTF-8")
                var t = parser.eventType
                while (t != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (t == org.xmlpull.v1.XmlPullParser.START_TAG && parser.localTag() == "sheet") {
                        val name = parser.getAttributeValue(null, "name")
                            ?: "Sheet${sheetNames.size + 1}"
                        // r:id puede estar como "r:id" o con namespace completo
                        val rId = (0 until parser.attributeCount)
                            .firstNotNullOfOrNull { i ->
                                val attrName = parser.getAttributeName(i)
                                if (attrName.endsWith("id") || attrName == "r:id")
                                    parser.getAttributeValue(i)
                                else null
                            } ?: "rId${sheetNames.size + 1}"
                        sheetNames += name
                        sheetRIds  += rId
                    }
                    t = parser.next()
                }
            } catch (_: Exception) {}
        }

        // Mapear rId → archivo usando workbook.xml.rels
        val rIdToFile = mutableMapOf<String, String>()
        entries["xl/_rels/workbook.xml.rels"]?.inputStream()?.let { relStream ->
            try {
                val parser = Xml.newPullParser()
                parser.setInput(relStream, "UTF-8")
                var t = parser.eventType
                while (t != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (t == org.xmlpull.v1.XmlPullParser.START_TAG && parser.localTag() == "Relationship") {
                        val id     = parser.getAttributeValue(null, "Id") ?: ""
                        val target = parser.getAttributeValue(null, "Target") ?: ""
                        if (id.isNotEmpty() && target.isNotEmpty()) {
                            // Target puede ser "worksheets/sheet1.xml" o "/xl/worksheets/sheet1.xml"
                            val normalized = when {
                                target.startsWith("/xl/") -> target.removePrefix("/")
                                target.startsWith("xl/")  -> target
                                else                      -> "xl/$target"
                            }
                            rIdToFile[id] = normalized
                        }
                    }
                    t = parser.next()
                }
            } catch (_: Exception) {}
        }

        // Fallback si workbook no se pudo parsear
        if (sheetNames.isEmpty()) {
            // Detectar hojas disponibles por nombre de archivo
            val available = entries.keys
                .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
                .sortedBy { it }
            available.forEachIndexed { i, _ -> sheetNames += "Sheet${i + 1}" }
        }

        val result = mutableMapOf<String, CsvParser.ParseResult>()
        sheetNames.forEachIndexed { idx, name ->
            // Resolver archivo: primero por rId, luego por índice
            val rId      = sheetRIds.getOrNull(idx) ?: ""
            val sheetKey = rIdToFile[rId]
                ?: "xl/worksheets/sheet${idx + 1}.xml"

            val bytes = entries[sheetKey] ?: return@forEachIndexed
            try {
                val rows = parseSheet(bytes.inputStream(), sharedStrings)
                if (rows.isNotEmpty()) {
                    val headers  = rows.first().map { it.trim() }
                    val dataRows = rows.drop(1)
                        .filter { row -> row.any { it.isNotBlank() } }
                        .map { row ->
                            headers.mapIndexed { i, h ->
                                h to (row.getOrNull(i)?.trim() ?: "")
                            }.toMap()
                        }
                    result[name] = CsvParser.ParseResult(headers, dataRows, emptyList())
                }
            } catch (_: Exception) {}
        }

        return MultiSheetResult(result)
    }

    /** Parsea solo la primera hoja — compatible con CsvParser.ParseResult */
    fun parse(stream: InputStream): CsvParser.ParseResult {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }

        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"]?.inputStream())

        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: return CsvParser.ParseResult(emptyList(), emptyList(), listOf("No se encontró la primera hoja"))

        val rows = parseSheet(sheetBytes.inputStream(), sharedStrings)
        if (rows.isEmpty()) return CsvParser.ParseResult(emptyList(), emptyList(), listOf("Hoja vacía"))

        val headers = rows.first().map { it.trim() }
        if (headers.isEmpty()) return CsvParser.ParseResult(emptyList(), emptyList(), listOf("Sin cabeceras"))

        val dataRows = rows.drop(1).mapIndexedNotNull { _, cols ->
            if (cols.all { it.isBlank() }) null
            else headers.mapIndexed { i, h -> h to (cols.getOrNull(i)?.trim() ?: "") }.toMap()
        }

        return CsvParser.ParseResult(headers, dataRows)
    }

    // ── Shared Strings ───────────────────────────────────────

    private fun parseSharedStrings(stream: InputStream?): List<String> {
        stream ?: return emptyList()
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser().apply { setInput(stream, "UTF-8") }
        val sb     = StringBuilder()
        var inT    = false
        var inSi   = false

        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.localTag()) {
                    "si" -> { inSi = true; sb.clear() }
                    "t"  -> inT = true
                }
                org.xmlpull.v1.XmlPullParser.TEXT ->
                    if (inT && inSi) sb.append(parser.text)
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.localTag()) {
                    "t"  -> inT = false
                    "si" -> { result.add(sb.toString()); inSi = false }
                }
            }
            event = parser.next()
        }
        return result
    }

    // ── Sheet ────────────────────────────────────────────────

    private fun parseSheet(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows   = mutableMapOf<Int, MutableMap<Int, String>>()
        val parser = Xml.newPullParser().apply { setInput(stream, "UTF-8") }

        var currentRow  = -1
        var currentCol  = -1
        var currentType = ""
        var inV         = false
        var inIs        = false
        var inT         = false
        val cellVal     = StringBuilder()

        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.localTag()) {
                    "row" -> {
                        val r = parser.getAttributeValue(null, "r")?.toIntOrNull()
                        currentRow = if (r != null) r - 1 else currentRow + 1
                    }
                    "c" -> {
                        val ref = parser.getAttributeValue(null, "r") ?: ""
                        currentCol  = colIndexFromRef(ref)
                        currentType = parser.getAttributeValue(null, "t") ?: ""
                        cellVal.clear()
                        inV  = false
                        inIs = false
                        inT  = false
                    }
                    "v"  -> inV  = true
                    "is" -> inIs = true
                    "t"  -> if (inIs) inT = true
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (inV || (inT && inIs)) cellVal.append(parser.text)
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.localTag()) {
                    "v"  -> inV  = false
                    "t"  -> inT  = false
                    "is" -> inIs = false
                    "c"  -> {
                        if (currentRow >= 0 && currentCol >= 0) {
                            val raw   = cellVal.toString().trim()
                            val value = when (currentType) {
                                "s"         -> sharedStrings.getOrElse(raw.toIntOrNull() ?: -1) { raw }
                                "inlineStr" -> raw
                                "b"         -> if (raw == "1") "TRUE" else "FALSE"
                                else        -> raw
                            }
                            rows.getOrPut(currentRow) { mutableMapOf() }[currentCol] = value
                        }
                    }
                }
            }
            event = parser.next()
        }

        if (rows.isEmpty()) return emptyList()
        val maxRow = rows.keys.max()
        val maxCol = rows.values.flatMap { it.keys }.maxOrNull() ?: 0

        return (0..maxRow).map { r ->
            val rowMap = rows[r] ?: emptyMap()
            (0..maxCol).map { c -> rowMap[c] ?: "" }
        }
    }

    // ── "B3" → 1  (0-based column index) ────────────────────

    private fun colIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch.isLetter()) col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            else break
        }
        return col - 1
    }
}
