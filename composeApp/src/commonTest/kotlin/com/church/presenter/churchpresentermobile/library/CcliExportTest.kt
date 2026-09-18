package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.CcliReport
import com.church.presenter.churchpresentermobile.model.ReportActivity
import com.church.presenter.churchpresentermobile.model.ReportFixtures
import com.church.presenter.churchpresentermobile.model.ReportFixtures.firstQuarter
import com.church.presenter.churchpresentermobile.model.ReportFixtures.log
import com.church.presenter.churchpresentermobile.model.ReportFixtures.zone
import com.church.presenter.churchpresentermobile.model.SongCredit
import com.church.presenter.churchpresentermobile.model.SongPlay
import com.church.presenter.churchpresentermobile.model.PlayLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The report as files: the CSV CCLI's page takes, and the workbook. */
class CcliExportTest {

    private val songs = CcliReport.songs(log, firstQuarter, zone)
    private val verses = CcliReport.verses(log, firstQuarter, zone)
    private val activity = ReportActivity.points(log, firstQuarter, zone)

    // ── CSV ──────────────────────────────────────────────────────────────

    @Test
    fun `the CSV has the desktop's header and one row per song`() {
        val lines = CcliExport.csv(songs, zone).trimEnd().lines()

        assertEquals("Title,Author,Songbook,Song Number,CCLI Number,Times Used,First Used,Last Used", lines.first())
        assertEquals(songs.size + 1, lines.size)
        assertEquals(""""Amazing Grace","John Newton","Hymnal","42","",4,2026-01-04,2026-03-22""", lines[1])
    }

    @Test
    fun `a title with a comma or a quote is quoted the RFC 4180 way`() {
        val credit = SongCredit(id = "x", title = """Holy, Holy, "Holy"""")
        val awkward = PlayLog(songs = listOf(SongPlay(credit, ReportFixtures.at(ReportFixtures.day(2026, 1, 4)))))

        val row = CcliExport.csv(CcliReport.songs(awkward, firstQuarter, zone), zone).lines()[1]

        assertTrue(row.startsWith(""""Holy, Holy, ""Holy""""""), row)
    }

    @Test
    fun `an empty report is just the header`() {
        assertEquals(1, CcliExport.csv(emptyList(), zone).trimEnd().lines().size)
    }

    @Test
    fun `the file is named for its range`() {
        assertEquals("CCLI-report-2026-01-01-to-2026-03-31", CcliExport.fileStem(firstQuarter))
    }

    // ── Workbook ─────────────────────────────────────────────────────────

    @Test
    fun `the workbook is a zip with the three sheets in it`() {
        val bytes = CcliExport.xlsx(songs, verses, activity, zone)
        val text = bytes.decodeToString()

        assertEquals(listOf('P'.code.toByte(), 'K'.code.toByte()), bytes.take(2), "a zip starts with PK")
        assertTrue("xl/worksheets/sheet1.xml" in text)
        assertTrue("xl/worksheets/sheet3.xml" in text)
        assertTrue("""<sheet name="Songs" """ in text)
        assertTrue("""<sheet name="Bible Verses" """ in text)
        assertTrue("""<sheet name="Activity" """ in text)
    }

    @Test
    fun `the songs sheet carries the desktop's columns and the rows' values`() {
        val bytes = CcliExport.xlsx(songs, verses, activity, zone).decodeToString()

        listOf("Rank", "Title", "Author", "Songbook", "Song #", "CCLI #", "Times Used", "First Used", "Last Used")
            .forEach { assertTrue("<t>$it</t>" in bytes, "missing header $it") }
        assertTrue("<t>Amazing Grace</t>" in bytes)
        assertTrue("<t>John</t>" in bytes, "the verses sheet names the book")
        assertTrue("<v>16</v>" in bytes, "and the verse, as a number")
        assertTrue("<t>2026-01-04</t>" in bytes)
    }

    // ── The writer underneath ────────────────────────────────────────────

    @Test
    fun `numbers are numbers and text is inline text`() {
        val sheet = XlsxSheet(
            "S", listOf("H"),
            listOf(listOf(XlsxCell.Number(4.0), XlsxCell.Text("a & b"), XlsxCell.Number(2.5))),
        )

        val xml = XlsxWriter.worksheet(sheet)

        assertTrue("""<c r="A1" t="inlineStr"><is><t>H</t></is></c>""" in xml)
        assertTrue("""<c r="A2"><v>4</v></c>""" in xml, "a whole number has no decimal, on any platform")
        assertTrue("""<c r="B2" t="inlineStr"><is><t>a &amp; b</t></is></c>""" in xml, xml)
        assertTrue("""<c r="C2"><v>2.5</v></c>""" in xml, "a fraction keeps its decimals")
    }

    @Test
    fun `columns are lettered the way a spreadsheet letters them`() {
        assertEquals("A", XlsxWriter.columnName(0))
        assertEquals("Z", XlsxWriter.columnName(25))
        assertEquals("AA", XlsxWriter.columnName(26))
        assertEquals("AZ", XlsxWriter.columnName(51))
        assertEquals("BA", XlsxWriter.columnName(52))
    }

    @Test
    fun `the CRC matches the reference vector every zip reader checks against`() {
        assertEquals(0xCBF43926.toInt(), StoredZip.crc32("123456789".encodeToByteArray()))
        assertEquals(0, StoredZip.crc32(ByteArray(0)))
    }

    @Test
    fun `a stored zip ends with a central directory naming every entry`() {
        val bytes = StoredZip.write(listOf("a.txt" to "hello".encodeToByteArray(), "dir/b.txt" to ByteArray(0)))
        val text = bytes.decodeToString()

        assertTrue(text.startsWith("PK"))
        // Each name appears twice: in its local header and again in the central directory.
        assertEquals(2, Regex("a\\.txt").findAll(text).count())
        assertEquals(2, Regex("dir/b\\.txt").findAll(text).count())
        // The end-of-central-directory record's entry counts, little-endian, sit right after its signature.
        val eocd = bytes.size - 22
        assertEquals(0x06054b50, readInt(bytes, eocd))
        assertEquals(2, bytes[eocd + 8].toInt())
        assertEquals(2, bytes[eocd + 10].toInt())
    }

    private fun readInt(bytes: ByteArray, at: Int): Int =
        (0 until 4).sumOf { (bytes[at + it].toInt() and 0xFF) shl (8 * it) }
}
