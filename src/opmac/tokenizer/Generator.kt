package opmac.tokenizer

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 *
 */
object Generator {

    private val REGEX_SECTION_CONFIG = Regex("-+lang:([a-zA-Z]+)-+name:(.*)")

    private fun generateSectionsWithArticles(srcDataFileRoot: String, genFormat: GenFormat): HashMap<String, ArrayList<Section>> {
        val sectionFolders = Files.walk(Paths.get(srcDataFileRoot)).filter { it.toFile().isDirectory && it.fileName.toString().matches(Regex("^[0-9][0-9][0-9]-[^/]*")) }.sorted().toArray()

        val sections = HashMap<String, ArrayList<Section>>()

        sectionFolders.forEach {
            val articleFiles = Files.walk(Paths.get(it.toString())).filter { it.toFile().isFile && it.fileName.toString().matches(Regex("^[0-9][0-9][0-9]-[^/]*")) }.sorted().toArray()

            val articlesInSection = HashMap<String, ArrayList<Triple<String, String, String>>>()

            articleFiles.forEach {

                Parser.parseFile(it.toString(), genFormat).forEach {
                    val list = articlesInSection.getOrDefault(it.key, ArrayList())
                    list.add(it.value)
                    articlesInSection[it.key] = list
                }

            }

            if (!File(it.toString() + "/config.txt").isFile() || !File(it.toString() + "/config.txt").canRead()) parsingError(it.toString() + "/config.txt", -1, true, "File '${it}/config.txt' could not be read.")

            val sectionNames = HashMap<String, String>()
            val sectionConfigReader = BufferedReader(FileReader(it.toString() + "/config.txt"))
            sectionConfigReader.readLines().forEach { line ->
                val match = REGEX_SECTION_CONFIG.matchEntire(line)
                if (match != null) {
                    val lang = match.groups[1]!!.value
                    val name = match.groups[2]!!.value
                    sectionNames[lang] = name
                }
            }

            val sectionCode = it.toString().substring(it.toString().lastIndexOf("/") + 5)

            articlesInSection.forEach { lang, articles ->
                if (sectionNames.containsKey(lang)) {
                    val sectionList = sections.getOrDefault(lang, ArrayList())
                    sectionList.add(Section(sectionCode, sectionNames[lang]!!, articles.map { it.first }.reduce { s1, s2 -> "${s1} ${s2}" }, articles.map { Pair(it.second, it.third) }))
                    sections[lang] = sectionList
                } else {
                    parsingError(it.toString() + "/config.txt", -1, true, "Does not contain section name for lang '${lang}'")
                }
            }

        }

        return sections
    }

    private fun generateForLang(langIso: String, genFormat: GenFormat, template: String, sections: ArrayList<Section>, mostRecentCount: Int): String {

        return template
                .replace("\$LANG_ISO\$", langIso)
                .replace("\$LANG_NAME\$", Translations.get(langIso, "langName"))
                .replace("\$PAGE_TITLE\$", Translations.get(langIso, "pageTitle"))
                .replace("\$PAGE_TITLE_THIS_PAGE\$", Translations.get(langIso, "pageTitleThisPage"))
                .replace("\$BACK_TO_TOP\$", Translations.get(langIso, "backToTop"))
                .replace("\$PDF_VERSION_INFO\$", Translations.get(langIso, "pdfVersionInfo"))
                .replace("\$TOC\$", Translations.get(langIso, "toc"))
                .replace("\$INTRO_PAR_1\$", Translations.get(langIso, "introPar1"))
                .replace("\$INTRO_PAR_2\$", Translations.get(langIso, "introPar2"))
                .replace("\$INTRO_PAR_3\$", Translations.get(langIso, "introPar3"))
                .replace("\$FULL_TOC\$", genFormat.generateToc(sections))

                .replace("\$OTHER_LANG\$", if (langIso.equals("cs")) "en" else "cs")
                .replace("\$OTHER_LANG_NAME\$", if (langIso.equals("cs")) Translations.get("en", "langName") else Translations.get("cs", "langName"))

                .replace("\$SECTIONS\$", genFormat.postProcessSections(sections))

                .replace("\$MOST_RECENT\$", if (mostRecentCount == 0) "" else genFormat.mostRecent(Parser.articleList, mostRecentCount, langIso))

    }

    fun generateForFormat(genFormat: GenFormat, template: String, outDir: String, dataFileRoot: String, mostRecentCount: Int) {

        Parser.articleList.clear()
        val sections = Generator.generateSectionsWithArticles(dataFileRoot, genFormat)

        Parser.articleList.sortByDescending { it.first }

        if (!File(outDir).exists()) {
            File(outDir).mkdirs()
        }

        sections.forEach {
            val writer = FileWriter("${outDir}/opmac-tricks-${it.key}.${genFormat.name}")
            writer.write(Generator.generateForLang(it.key, genFormat, template, it.value, mostRecentCount))
            writer.close()
        }

    }

}

data class Section(val id: String, val titleInLang: String, val articlesCode: String, val articleTitles: List<Pair<String, String>>)