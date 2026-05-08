package com.pabl3st.rutapp.core.importer

import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parser ligero de CSV/TSV.
 * No usa dependencias externas — suficiente para importar listas de PDVs.
 *
 * Soporta:
 *  - Separadores: coma, punto y coma, tabulador (auto-detectado)
 *  - Comillas dobles para campos con separador interno
 *  - Primera fila como cabecera
 *  - Encoding UTF-8 y Latin-1
 */
object CsvParser {

    data class ParseResult(
        val headers: List<String>,
        val rows:    List<Map<String, String>>,
        val errors:  List<String> = emptyList(),
    )

    fun parse(stream: InputStream, forceSep: Char? = null): ParseResult {
        val lines = InputStreamReader(stream, Charsets.UTF_8)
            .buffered()
            .readLines()
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return ParseResult(emptyList(), emptyList(), listOf("Fichero vacío"))

        val sep = forceSep ?: detectSeparator(lines.first())
        val headers = splitLine(lines.first(), sep).map { it.trim() }

        if (headers.isEmpty()) return ParseResult(
            emptyList(), emptyList(), listOf("No se encontraron cabeceras")
        )

        val errors = mutableListOf<String>()
        val rows = lines.drop(1).mapIndexedNotNull { idx, line ->
            val cols = splitLine(line, sep)
            if (cols.size < headers.size / 2) {
                errors.add("Línea ${idx + 2}: columnas insuficientes (${cols.size}/${headers.size})")
                null
            } else {
                headers.mapIndexed { i, header ->
                    header to (cols.getOrNull(i)?.trim() ?: "")
                }.toMap()
            }
        }

        return ParseResult(headers, rows, errors)
    }

    private fun detectSeparator(line: String): Char {
        val counts = mapOf(
            ';'  to line.count { it == ';' },
            ','  to line.count { it == ',' },
            '\t' to line.count { it == '\t' },
        )
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    private fun splitLine(line: String, sep: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == sep && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
