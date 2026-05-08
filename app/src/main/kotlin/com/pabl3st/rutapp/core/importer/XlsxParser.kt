package com.pabl3st.rutapp.core.importer

import android.util.Xml
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Parser XLSX ligero — sin dependencias externas.
 * Lee la primera hoja de un fichero .xlsx usando la spec OOXML (ZIP + XML).
 *
 * Limitaciones intencionadas (suficientes para importar PDVs):
 *  - Solo primera hoja (Sheet1 / xl/worksheets/sheet1.xml)
 *  - Tipos soportados: string, número, fecha ISO, inline string
 *  - Sin fórmulas (se ignoran, se lee cached value si existe)
 */
object XlsxParser {

    /** Reutiliza el mismo ParseResult que CsvParser */
    fun parse(stream: InputStream): CsvParser.ParseResult {
        val entries = mutableMapOf<String, ByteArray>()

        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }

        // 1. Shared strings (sst)
        val sharedStrings = parseSharedStrings(
            entries["xl/sharedStrings.xml"]?.inputStream()
        )

        // 2. Primera hoja
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: return CsvParser.ParseResult(emptyList(), emptyList(), listOf("No se encontró la primera hoja"))

        val rows = parseSheet(sheetBytes.inputStream(), sharedStrings)

        if (rows.isEmpty()) return CsvParser.ParseResult(emptyList(), emptyList(), listOf("Hoja vacía"))

        val headers = rows.first().map { it.trim() }
        if (headers.isEmpty()) return CsvParser.ParseResult(emptyList(), emptyList(), listOf("Sin cabeceras"))

        val dataRows = rows.drop(1).mapIndexedNotNull { idx, cols ->
            if (cols.all { it.isBlank() }) return@mapIndexedNotNull null
            headers.mapIndexed { i, h -> h to (cols.getOrNull(i)?.trim() ?: "") }.toMap()
        }

        return CsvParser.ParseResult(headers, dataRows)
    }

    // ── Shared Strings ───────────────────────────────────────

    private fun parseSharedStrings(stream: InputStream?): List<String> {
        stream ?: return emptyList()
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser().apply { setInput(stream, "UTF-8") }
        val sb = StringBuilder()
        var inT = false
        var inSi = false

        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { inSi = true; sb.clear() }
                    "t"  -> inT = true
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if (inT && inSi) sb.append(parser.text)
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
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
        val rows  = mutableMapOf<Int, MutableMap<Int, String>>() // rowIdx -> colIdx -> value
        val parser = Xml.newPullParser().apply { setInput(stream, "UTF-8") }

        var currentRow = -1
        var currentCol = -1
        var currentType = ""   // "s"=sharedString, "inlineStr", ""=number/date
        var inV   = false
        var inIs  = false
        var inT   = false
        val cellVal = StringBuilder()

        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        currentRow = (parser.getAttributeValue(null, "r")?.toIntOrNull() ?: (currentRow + 1)) - 1
                    }
                    "c"   -> {
                        val ref = parser.getAttributeValue(null, "r") ?: ""
                        currentCol = colIndexFromRef(ref)
                        currentType = parser.getAttributeValue(null, "t") ?: ""
                        cellVal.clear()
                    }
                    "v"   -> { inV = true }
                    "is"  -> { inIs = true }
                    "t"   -> if (inIs) inT = true
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (inV || (inT && inIs)) cellVal.append(parser.text)
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
                    "v"   -> inV = false
                    "t"   -> inT = false
                    "is"  -> inIs = false
                    "c"   -> {
                        if (currentRow >= 0 && currentCol >= 0) {
                            val raw = cellVal.toString().trim()
                            val value = when (currentType) {
                                "s"         -> sharedStrings.getOrElse(raw.toIntOrNull() ?: -1) { raw }
                                "inlineStr" -> raw
                                "b"         -> if (raw == "1") "TRUE" else "FALSE"
                                else        -> raw   // number, date — keep as-is
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

    // ── "B3" -> 1  (0-based column index) ───────────────────

    private fun colIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch.isLetter()) col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            else break
        }
        return col - 1
    }
}
