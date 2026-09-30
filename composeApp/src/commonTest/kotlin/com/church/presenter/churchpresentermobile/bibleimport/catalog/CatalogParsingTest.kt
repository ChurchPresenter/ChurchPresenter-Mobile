package com.church.presenter.churchpresentermobile.bibleimport.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogParsingTest {

    private val csv = """
        |languageCode,translationId,languageName,languageNameInEnglish,dialect,homeDomain,title,description,Redistributable,Copyright,UpdateDate,publicationURL,OTbooks,OTchapters,OTverses,NTbooks,NTchapters,NTverses,DCbooks,DCchapters,DCverses,FCBHID,Certified,inScript,swordName,rodCode,textDirection,downloadable,font,shortTitle,PODISBN,script,sourceDate
        |eng,engwebp,English,English,,ebible.org,World English Bible,"Public domain, ""really"" yes",True,Public Domain,2026-01-01,,39,929,23145,27,260,7957,0,0,0,,True,,,,ltr,True,,World English Bible,,Latin,
        |eng,engbsb,English,English,,ebible.org,Berean Standard Bible,,True,Public Domain,,,0,0,0,27,260,7957,0,0,0,,True,,,,ltr,True,,Berean Standard Bible,,Latin,
        |ukr,ukrogi,Українська,Ukrainian,,ebible.org,Біблія Огієнка,,True,PD,,,39,0,0,27,0,0,0,0,0,,True,,,,ltr,True,,Біблія (Огієнко),,Cyrillic,
        |eng,engnet,English,English,,ebible.org,NET Bible,,False,©,,,39,0,0,27,0,0,0,0,0,,True,,,,ltr,True,,NET,,Latin,
        |eng,engwebp,English,English,,ebible.org,Duplicate id,,True,PD,,,39,0,0,27,0,0,0,0,0,,True,,,,ltr,True,,Dup,,Latin,
    """.trimMargin()

    @Test
    fun `eBible keeps only what may be redistributed and downloaded`() {
        val rows = EBibleCatalog.parse(csv)
        assertEquals(listOf("engwebp", "engbsb", "ukrogi", "engwebp"), rows.map { it.identifier })
    }

    @Test
    fun `eBible rows carry the language in both spellings and the published book counts`() {
        val ukrainian = EBibleCatalog.parse(csv).first { it.identifier == "ukrogi" }
        assertEquals("UKR", ukrainian.language)
        assertEquals("Ukrainian", ukrainian.languageName)
        assertEquals("Українська", ukrainian.languageNativeName)
        assertEquals("Біблія (Огієнко)", ukrainian.displayName)
        assertEquals(Testament.FULL, ukrainian.testament)
        // The id repeats the language, so the prefix is dropped as the desktop drops it.
        assertEquals("UKR_OGI", ukrainian.fileStem)
    }

    @Test
    fun `a New Testament is recognised from its book counts`() {
        assertEquals(Testament.NEW, EBibleCatalog.parse(csv).first { it.identifier == "engbsb" }.testament)
    }

    @Test
    fun `stems that collide are numbered in catalogue order`() {
        assertEquals(listOf("ENG_WEBP", "ENG_WEBP_2"), EBibleCatalog.parse(csv).filter { it.identifier == "engwebp" }.map { it.fileStem })
    }

    @Test
    fun `quoted CSV fields keep their commas and doubled quotes`() {
        assertEquals(listOf(listOf("a", "b, \"c\"", "")), Csv.parse("a,\"b, \"\"c\"\"\",\r\n"))
    }

    @Test
    fun `eBible's language names are offered to the other sources`() {
        val names = EBibleCatalog.languageNames(EBibleCatalog.parse(csv))
        assertEquals(LanguageNaming("Ukrainian", "Українська"), names["UKR"])
        assertEquals(LanguageNaming("English"), names["ENG"])
    }

    private val tree = """
        {"sha":"x","truncated":false,"tree":[
          {"path":"zefania-sharp-sourceforge-backup/Bibles/RUS/Russian Synodal Translation/SF_2009-01-20_RUS_RST_(RUSSIAN SYNODAL TRANSLATION).zip","type":"blob","sha":"abc","size":1512807},
          {"path":"zefania-sharp-sourceforge-backup/Bibles/CZE/SF_2009-01-20_CZE__SP.zip","type":"blob","sha":"def","size":10},
          {"path":"zefania-sharp-sourceforge-backup/Bibles/RUS/readme.txt","type":"blob","sha":"x","size":1},
          {"path":"zefania-sharp-sourceforge-backup/Bibles/RUS","type":"tree","sha":"x","size":0}
        ]}
    """.trimIndent()

    @Test
    fun `Zefania lists the archive's zips with language from the folder`() {
        val rows = ZefaniaCatalog.parse(tree, mapOf("RUS" to LanguageNaming("Russian", "Русский")))!!
        assertEquals(2, rows.size)
        val rst = rows.first { it.language == "RUS" }
        assertEquals("Russian Synodal Translation", rst.displayName)
        assertEquals("Russian", rst.languageName)
        assertEquals("abc", rst.checksum)
        assertEquals(1_512_807, rst.sizeBytes)
        assertEquals("RUS_RST", rst.fileStem)
    }

    @Test
    fun `a Zefania name without a title falls back to its identifier`() {
        val czech = ZefaniaCatalog.parse(tree, emptyMap())!!.first { it.language == "CZE" }
        assertEquals("CZE_SP", czech.fileStem)
    }

    @Test
    fun `a truncated Zefania listing is refused rather than shown as complete`() {
        assertNull(ZefaniaCatalog.parse(tree.replace("\"truncated\":false", "\"truncated\":true"), emptyMap()))
        assertNull(ZefaniaCatalog.parse("<html>rate limited</html>", emptyMap()))
    }

    @Test
    fun `Zefania download paths are percent-encoded segment by segment`() {
        assertEquals(
            "https://raw.githubusercontent.com/ChurchPresenter/Zefania-XML-Preservation/main/" +
                "Bibles/RUS/Russian%20Synodal/SF_%28R%C3%9C%29.zip",
            ZefaniaCatalog.downloadUrl("Bibles/RUS/Russian Synodal/SF_(RÜ).zip"),
        )
    }

    @Test
    fun `shouting archive names are title-cased but abbreviations survive`() {
        assertEquals("King James Version PCE", ZefaniaCatalog.titleCase("KING JAMES VERSION PCE"))
        assertEquals("Luther 1912", ZefaniaCatalog.titleCase("Luther 1912"))
    }

    private val beblia = """
        {"schemaVersion":1,"commit":"c0ffee","bibles":[
          {"file":"GermanLutherBible.xml","sha":"s1","size":4800000,"title":"Luther 1912","id":"LUTH1912","lang":"deu","langName":"German","rights":"PD","ot":39,"nt":0},
          {"file":"SpanishRV1909Bible.xml","sha":"s2","size":4500000,"title":"","id":"RV1909","lang":"SPA","langName":"Spanish","ot":39,"nt":27}
        ]}
    """.trimIndent()

    @Test
    fun `Holy Bible XML rows are pinned to the manifest's commit`() {
        val rows = BebliaCatalog.parse(beblia, emptyMap())!!
        val luther = rows.first { it.identifier == "LUTH1912" }
        assertEquals("c0ffee/GermanLutherBible.xml", luther.downloadKey)
        assertEquals(Testament.OLD, luther.testament)
        assertEquals("DEU_LUTH1912", luther.fileStem)
        assertEquals(
            "https://raw.githubusercontent.com/ChurchPresenter/Holy-Bible-XML-Format/c0ffee/GermanLutherBible.xml",
            BebliaCatalog.downloadUrl(luther.downloadKey),
        )
    }

    @Test
    fun `an untitled Holy Bible XML row is named after its file`() {
        assertEquals("SpanishRV1909Bible", BebliaCatalog.parse(beblia, emptyMap())!!.first { it.identifier == "RV1909" }.displayName)
    }

    @Test
    fun `a manifest with no commit or no Bibles is refused`() {
        assertNull(BebliaCatalog.parse(beblia.replace("\"commit\":\"c0ffee\"", "\"commit\":\"\""), emptyMap()))
        assertNull(BebliaCatalog.parse("""{"commit":"c","bibles":[]}""", emptyMap()))
    }

    @Test
    fun `a search matches the name the code and either spelling of the language`() {
        val ukrainian = EBibleCatalog.parse(csv).first { it.identifier == "ukrogi" }
        assertTrue(ukrainian.matches("огієн"))
        assertTrue(ukrainian.matches("ukrainian"))
        assertTrue(ukrainian.matches("Українська"))
        assertTrue(ukrainian.matches("ukr"))
        assertTrue(!ukrainian.matches("german"))
    }

    @Test
    fun `a language spelt two ways takes the spelling most rows use`() {
        val rows = EBibleCatalog.parse(csv).filter { it.language == "UKR" }.first().let { base ->
            listOf(base, base, base.copy(languageName = "Ukranian"))
        }
        assertEquals("Ukrainian", EBibleCatalog.languageNames(rows)["UKR"]?.english)
    }

    @Test
    fun `hand-checked names win over a catalogue's`() {
        val names = BibleLanguageNames.resolve(mapOf("DEU" to LanguageNaming("German, Standard"), "ACH" to LanguageNaming("Acholi")))
        assertEquals("German", names["DEU"]?.english)
        assertEquals("Acholi", names["ACH"]?.english)
    }

    @Test
    fun `built-in language names cover a file converted with no catalogue`() {
        assertEquals("Ukrainian", BibleLanguageNames.nameOf("ukr"))
        assertEquals("XYZ", BibleLanguageNames.nameOf("xyz"))
    }
}
