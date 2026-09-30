package com.church.presenter.churchpresentermobile.bibleimport

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The fixtures are real zlib output (Python's `zipfile` / `zlib.compressobj`), so this checks the
 * port against the reference implementation rather than against itself — one fixture per DEFLATE
 * block type: stored, fixed Huffman and dynamic Huffman.
 */
@OptIn(ExperimentalEncodingApi::class)
class ZipReaderTest {

    /** Three entries: a dynamic-Huffman text in a folder, a fixed-Huffman "Hi!", and a stored file. */
    private val archive = Base64.decode(
        "UEsDBBQAAAAIAAAAIQD/jYgufAAAAEYaAAAbAAAAZW5na2p2X3VzZngvZW5na2p2X3VzZngueG1s7dYxDoNADETRPqfwA" +
            "SLuQBVxDId12C3ilTYGro8EDWWKNCi/HNmvmG4Gl8gmT5uKe/FJHjXJ2EzD0n7Jpou5qB/RtEXuZIDBYDAYDAb7NevP" +
            "UVb9yFoi1znkVdv7vr8vtaTudok6MBgMBoPB/oKxYGAwGAwGg12Pfb9gNlBLAwQUAAAACAAAACEA2sWeeQUAAAADAAAA" +
            "DQAAAEJvb2tOYW1lcy54bWzzyFQEAFBLAwQUAAAAAAAAACEAy69ccQsAAAALAAAACgAAAHN0b3JlZC50eHRwbGFpbiBi" +
            "eXRlc1BLAQIUAxQAAAAIAAAAIQD/jYgufAAAAEYaAAAbAAAAAAAAAAAAAACAAQAAAABlbmdranZfdXNmeC9lbmdranZf" +
            "dXNmeC54bWxQSwECFAMUAAAACAAAACEA2sWeeQUAAAADAAAADQAAAAAAAAAAAAAAgAG1AAAAQm9va05hbWVzLnhtbFBL" +
            "AQIUAxQAAAAAAAAAIQDLr1xxCwAAAAsAAAAKAAAAAAAAAAAAAACAAeUAAABzdG9yZWQudHh0UEsFBgAAAAADAAMAvAAA" +
            "ABgBAAAAAA==",
    )

    private val longText = (
        "In the beginning God created the heaven and the earth. ".repeat(40) +
            "And the earth was without form, and void.\n"
        ).repeat(3)

    @Test
    fun `lists every entry with its path and base name`() {
        val entries = ZipReader(archive).entries
        assertEquals(listOf("engkjv_usfx/engkjv_usfx.xml", "BookNames.xml", "stored.txt"), entries.map { it.name })
        assertEquals("engkjv_usfx.xml", entries.first().baseName)
    }

    @Test
    fun `a dynamic-Huffman entry inflates to the original text`() {
        val zip = ZipReader(archive)
        assertEquals(longText, zip.read(zip.entries[0]).decodeToString())
    }

    @Test
    fun `a fixed-Huffman entry inflates`() {
        val zip = ZipReader(archive)
        assertEquals("Hi!", zip.read(zip.entries[1]).decodeToString())
    }

    @Test
    fun `a stored entry is copied out`() {
        val zip = ZipReader(archive)
        assertEquals("plain bytes", zip.read(zip.entries[2]).decodeToString())
    }

    @Test
    fun `a stored DEFLATE block inflates`() {
        val raw = Base64.decode("AREA7v9zdG9yZWQgYmxvY2sgdGV4dA==")
        assertEquals("stored block text", Inflater.inflate(raw, 0, raw.size, 0, MAX).decodeToString())
    }

    @Test
    fun `long back-references across a module-shaped text inflate exactly`() {
        val raw = Base64.decode(MODULE_SHAPED)
        val expected = buildString {
            for (b in 1..3) for (c in 1..5) for (v in 1..30) {
                append("B${pad(b)}C${pad(c)}V${pad(v)}\t$b\t$c\t$v\tVerse text number $v of chapter $c.\n")
            }
        }
        assertEquals(expected, Inflater.inflate(raw, 0, raw.size, 0, MAX).decodeToString())
    }

    @Test
    fun `a corrupted entry fails its checksum rather than returning garbage`() {
        val corrupted = archive.copyOf()
        // Flip a byte inside the stored entry's data, found by its bytes rather than as text.
        val needle = "plain bytes".encodeToByteArray()
        val dataAt = (0..corrupted.size - needle.size).first { at ->
            needle.indices.all { corrupted[at + it] == needle[it] }
        }
        corrupted[dataAt] = 'X'.code.toByte()
        val zip = ZipReader(corrupted)
        assertFailsWith<CorruptArchiveException> { zip.read(zip.entries[2]) }
    }

    @Test
    fun `something that is not a zip is refused`() {
        assertFailsWith<CorruptArchiveException> { ZipReader("<html>Captive portal</html>".encodeToByteArray()) }
    }

    @Test
    fun `truncated compressed data is refused`() {
        val raw = Base64.decode(MODULE_SHAPED)
        assertFailsWith<CorruptArchiveException> { Inflater.inflate(raw, 0, raw.size / 2, 0, MAX) }
    }

    @Test
    fun `output past the limit is refused so a zip bomb cannot exhaust memory`() {
        val raw = Base64.decode(MODULE_SHAPED)
        assertFailsWith<CorruptArchiveException> { Inflater.inflate(raw, 0, raw.size, 0, 10_000) }
    }

    @Test
    fun `inflating the same bytes twice gives the same result`() {
        val raw = Base64.decode(MODULE_SHAPED)
        assertContentEquals(
            Inflater.inflate(raw, 0, raw.size, 0, MAX),
            Inflater.inflate(raw, 0, raw.size, 24_480, MAX),
        )
    }

    private fun pad(value: Int) = value.toString().padStart(3, '0')

    private companion object {
        const val MAX = 1_000_000

        /** `zlib.compressobj(9, DEFLATED, -15)` over 450 `.spb`-shaped verse lines. */
        const val MODULE_SHAPED =
            "ldk7rmRVEERRG0bxRoDe3VH1Pm4zh/YBNcLho6aRGD7NyVK0UrpXJ0OlMsrYVoVOGuvD8/Px49fvx6/f7+rz8dPnvz89f" +
                "fn075enP/75/edPn5+Opz9/ffrlt5/++vL/jx++//AtYiWcRFxHWpFOIl1HtxXdTqLbdXRf0f0kul9HLyt6OYlerqPX" +
                "Fb2eRK/X0duK3k6it+vofUXvJ9H7ZXQ815/7fPbvPl9nj02cjuJ6FUet4jibxXG9i6N2cZwN47hexlHLOM6mcVxv46ht" +
                "HGfjOK7XcdQ6jrN5HNf7OGofx9lAjuuFHLWQ42wix/VGjtrIcTaS43ol1Eo4WwnXK6FWwtlKuF4Jj7fj9PG4Xgm1Es5W" +
                "wvVKqJVwthKuV0KthLOVcL0SaiWcrYTrlVAr4WwlXK+EWglnK+F6JdRKOFsJ1ytRrURnK9HFSnjcF/b3hRaxou196ZFW" +
                "tL0vPbqtaHtfenRf0fa+9OhlRdv70qPXFW3vS4/eVrS9Lz16X9H2vrRo3Rcm96Vnj03s70vPahWD+9Kz2sXgvvSsljG4" +
                "Lz2rbQzuS89qHYP70rPax+C+9KwWMrgvPauNDO5Ly6iVDO5Lz2olg/vSs8fbsb8vPauVDO5Lz2olg/vSs1rJ4L70rFYy" +
                "uC89q5UM7kvPaiWD+9KzWsngvrRMtZLBffmW6XFftL8vahEr2t6XHmlF2/vSo9uKtvelR/cVbe9Lj15WtL0vPXpd0fa+" +
                "9OhtRdv70qP3FW3vS4vWfdHkvvTssYn9felZrWJwX3pWuxjcl57VMgb3pWe1jcF96VmtY3Bfelb7GNyXntVCBvelZ7WR" +
                "wX1pGbWSwX3pWa1kcF969ng79velZ7WSwX3pWa1kcF96VisZ3Jee1UoG96VntZLBfelZrWRwX3pWKxncl5apVjK4L9+y" +
                "2+O+3Pb35dYiVrS9Lz3Sirb3pUe3FW3vS4/uK9relx69rGh7X3r0uqLtfenR24q296VH7yva3pcWrftym9yXnj02sb8v" +
                "PatVDO5Lz2oXg/vSs1rG4L70rLYxuC89q3UM7kvPah+D+9KzWsjgvvSsNjK4Ly2jVjK4Lz2rlQzuS88eb8f+vvSsVjK4" +
                "Lz2rlQzuS89qJYP70rNayeC+9KxWMrgvPauVDO5Lz2olg/vSMtVKBvflW3Z/3Jf7/r7cW8SKtvelR1rR9r706Lai7X3p" +
                "0X1F2/vSo5cVbe9Lj15XtL0vPXpb0fa+9Oh9Rdv70qJ1X+6T+9Kzxyb296VntYrBfelZ7WJwX3pWyxjcl57VNgb3pWe1" +
                "jsF96VntY3BfelYLGdyXntVGBvelZdRKBvelZ7WSwX3p2ePt2N+XntVKBvelZ7WSwX3pWa1kcF96VisZ3Jee1UoG96Vn" +
                "tZLBfelZrWRwX1qmWsngvlSGfZ+572PfZ+772PeZ+z72fea+j32fue9j32fu+9j3mfs+9n3mvo99n7nvY98n8X3s+yS+" +
                "j32fxPex75P4PvZ9Et/Hvk/i+9j3SXwf+z6J72PfJ/F97Pskvo99n8T3se+T+D72fRLfx75P4vvY90l8H/s+ie9j3yfx" +
                "fez7JL6PfZ/E97Hvk/g+9n0S38e+z9z3se8z933s+8x9H/s+c9/Hvs/c97HvM/d97PvMfR/7PnPfx77P3Pex75P4PvZ9" +
                "Et/Hvk/i+9j3SXwf+z6J72PfJ/F97Pskvo99n8T3se+T+D72fRLfx75P4vvY90l8H/s+ie9j3yfxfez7JL6PfZ/E97Hv" +
                "k/g+9n0S38e+T+L72PdJfB/7PonvY99n7vvY95n7PvZ95r6PfZ+572PfZ+772PeZ+z72fea+j32fue9j32fu+9j3SXwf" +
                "+z6J72PfJ/F97Pskvo99n8T3se+T+D72fRLfx75P4vvY90l8H/s+ie9j3yfxfez7JL6PfZ/E97Hvk/g+9n0S38e+T+L7" +
                "2PdJfB/7PonvY98n8X3s+yS+j32fxPex7zP3fez7zH0f+z5z38e+z9z3se8z933s+8x9H/s+c9/Hvs/c97HvM/d97Psk" +
                "vo99n8T3se+T+D72fRLfx75P4vvY90l8H/s+ie9j3yfxfez7JL6PfZ/E97Hvk/g+9n0S38e+T+L72PdJfB/7PonvY98n" +
                "8X3s+yS+j32fxPex75P4PvZ9Et/Hvk/i+9j3mfs+9n3mvo99n7nvY99n7vvY95n7PvZ95r6PfZ+572PfZ+772PeZ+z72" +
                "fRLfx75P4vvY90l8H/s+ie9j3yfxfez7JL6PfZ/E97Hvk/g+9n0S38e+T+L72PdJfB/7PonvY98n8X3s+yS+j32fxPex" +
                "75P4PvZ9Et/Hvk/i+9j3SXwf+z6J72PfJ/F92fc1933Z9zX3fdn3Nfd92fc1933Z9zX3fdn3Nfd92fc1933Z9zX3fdn3" +
                "Nfd92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76v" +
                "xPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vue/Lvq+578u+r7nvy76vue/Lvq+578u+r7nvy76v" +
                "ue/Lvq+578u+r7nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F92feV" +
                "+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9zX1f9n3NfV/2fc19X/Z9zX1f9n3N" +
                "fV/2fc19X/Z9zX1f9n3NfV/2fc19X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S" +
                "35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7Lva+77su9r7vuy72vu" +
                "+7Lva+77su9r7vuy72vu+7Lva+77su9r7vuy72vu+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi" +
                "+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X3Pf" +
                "l31fc9+XfV9z35d9X3Pfl31fc9+XfV9z35d9X3Pfl31fc9+XfV9z35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8" +
                "X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F92feV+L7s+0p8X/Z9Jb4v+74S35d9X4nvy76vxPdl31fi+7LvK/F9" +
                "2fc19v3/AA=="
    }
}
