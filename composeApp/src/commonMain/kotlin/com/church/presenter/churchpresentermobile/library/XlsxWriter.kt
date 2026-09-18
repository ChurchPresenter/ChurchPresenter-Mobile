package com.church.presenter.churchpresentermobile.library

/** One cell of a sheet: text, or a number the spreadsheet can sum. */
sealed interface XlsxCell {
    data class Text(val value: String) : XlsxCell
    data class Number(val value: Double) : XlsxCell
}

/** A sheet: a name, a header row, and rows of cells. */
data class XlsxSheet(
    val name: String,
    val header: List<String>,
    val rows: List<List<XlsxCell>>,
)

private const val LETTERS_IN_ALPHABET = 26
private const val MAX_SHEET_NAME_LENGTH = 31

/**
 * Writes a minimal but standard `.xlsx` with no library behind it.
 *
 * The desktop uses Apache POI, which is JVM-only; the phone has nothing of the
 * kind and does not want a megabyte of dependency for three sheets. An `.xlsx`
 * is a zip of a handful of XML parts, and a zip that only *stores* its entries
 * needs no compressor — just the headers and a CRC. Cells carry their text
 * inline (`t="inlineStr"`) so no shared-strings part is needed, and no styles
 * part is written: Excel, Numbers and LibreOffice all open a workbook without
 * one.
 */
object XlsxWriter {

    /** The workbook, as the bytes of the file. */
    fun write(sheets: List<XlsxSheet>): ByteArray {
        val entries = mutableListOf<Pair<String, String>>()
        entries += "[Content_Types].xml" to contentTypes(sheets.size)
        entries += "_rels/.rels" to ROOT_RELS
        entries += "xl/workbook.xml" to workbook(sheets)
        entries += "xl/_rels/workbook.xml.rels" to workbookRels(sheets.size)
        sheets.forEachIndexed { index, sheet ->
            entries += "xl/worksheets/sheet${index + 1}.xml" to worksheet(sheet)
        }
        return StoredZip.write(entries.map { (name, xml) -> name to xml.encodeToByteArray() })
    }

    // ── The XML parts ────────────────────────────────────────────────────

    private const val XML_HEADER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

    private const val ROOT_RELS = XML_HEADER +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" """ +
        """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" """ +
        """Target="xl/workbook.xml"/></Relationships>"""

    private fun contentTypes(sheetCount: Int): String = buildString {
        append(XML_HEADER)
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" """)
        append("""ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" """)
        append("""ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" """)
            append("""ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun workbook(sheets: List<XlsxSheet>): String = buildString {
        append(XML_HEADER)
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
        append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { index, sheet ->
            val id = index + 1
            append("""<sheet name="${escape(sheetName(sheet.name))}" sheetId="$id" r:id="rId$id"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(sheetCount: Int): String = buildString {
        append(XML_HEADER)
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" """)
            append("""Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" """)
            append("""Target="worksheets/sheet$i.xml"/>""")
        }
        append("</Relationships>")
    }

    /** One sheet's XML. Internal so a test can read the cells back without unzipping. */
    internal fun worksheet(sheet: XlsxSheet): String = buildString {
        append(XML_HEADER)
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        appendRow(1, sheet.header.map { XlsxCell.Text(it) })
        sheet.rows.forEachIndexed { index, row -> appendRow(index + 2, row) }
        append("</sheetData></worksheet>")
    }

    private fun StringBuilder.appendRow(rowNumber: Int, cells: List<XlsxCell>) {
        append("""<row r="$rowNumber">""")
        cells.forEachIndexed { column, cell ->
            val ref = "${columnName(column)}$rowNumber"
            when (cell) {
                is XlsxCell.Number -> append("""<c r="$ref"><v>${numberText(cell.value)}</v></c>""")
                is XlsxCell.Text ->
                    append("""<c r="$ref" t="inlineStr"><is><t>${escape(cell.value)}</t></is></c>""")
            }
        }
        append("</row>")
    }

    /**
     * A whole number without its ".0": the JVM prints 4.0 and JS prints 4,
     * and a workbook must not depend on which platform wrote it.
     */
    private fun numberText(value: Double): String {
        val whole = value.toLong()
        return if (whole.toDouble() == value) whole.toString() else value.toString()
    }

    /** 0 → "A", 25 → "Z", 26 → "AA". */
    internal fun columnName(index: Int): String {
        var n = index
        val out = StringBuilder()
        do {
            out.insert(0, 'A' + n % LETTERS_IN_ALPHABET)
            n = n / LETTERS_IN_ALPHABET - 1
        } while (n >= 0)
        return out.toString()
    }

    /** Excel refuses sheet names over 31 characters or containing `[]*?/\:`. */
    private fun sheetName(name: String): String =
        name.replace(Regex("[\\[\\]*?/\\\\:]"), " ").take(MAX_SHEET_NAME_LENGTH).ifBlank { "Sheet" }

    private fun escape(text: String): String = buildString(text.length) {
        for (ch in text) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                // XML 1.0 cannot carry control characters at all; drop them rather than corrupt the sheet.
                else -> if (ch >= ' ' || ch == '\n' || ch == '\t') append(ch)
            }
        }
    }
}
