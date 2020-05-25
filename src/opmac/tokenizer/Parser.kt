package opmac.tokenizer

import java.io.BufferedReader
import java.io.FileReader
import kotlin.collections.HashMap

/**
 *
 */
object Parser {

    fun parseFile(filename: String, genFormat: GenFormat): HashMap<String, Triple<String, String, String>> {
        val reader = BufferedReader(FileReader(filename))

        var line = reader.readLine()
        var lineNumber = 1

        var parsingLang: String? = null

        val meta: HashMap<String, String> = HashMap()
        val titles: HashMap<String, Pair<Int, String>> = HashMap()
        val sections: HashMap<String, String> = HashMap()
        val tokenizedSections: HashMap<String, Triple<String, String, String>> = HashMap()

        var sectionBuffer = ""

        while (line != null) {
            if (line.startsWith("----")) { // Next lang part

                var newLang: String? = null
                var newTitle: String? = null

                while (line.isNotEmpty()) {
                    while (line.startsWith("-")) line = line.substring(1)
                    if (line.startsWith("lang:")) {
                        if (newLang != null) {
                            parsingError(filename, lineNumber, true, "Lang was already defined for this section.")
                        }
                        newLang = if (line.indexOf("-") == -1) line.substring(5) else line.substring(5, line.indexOf("-"))
                        line = if (line.indexOf("-") == -1) "" else line.substring(line.indexOf("-"))
                    } else if (line.startsWith("title:")) {
                        newTitle = line.substring(6)
                        line = ""
                    }
                }

                if (newLang == null) parsingError(filename, lineNumber, true, "Lang is not defined for this section.")
                if (newTitle == null) parsingError(filename, lineNumber, true, "Title is not defined for this section.")

                if (titles.containsKey(newLang!!)) parsingError(filename, lineNumber, true, "Section with that Lang was already defined above.")
                if (parsingLang != null) sections.put(parsingLang, sectionBuffer)
                titles.put(newLang, Pair(lineNumber, newTitle!!))
                parsingLang = newLang
                sectionBuffer = ""

            } else if (parsingLang == null) { // Parsing meta
                if (line.isNotEmpty()) {
                    val splitIdx = line.indexOf(":")
                    if (splitIdx == -1) {
                        parsingError(filename, lineNumber, false, "Invalid meta tag format '${line}'. Tags expected in format 'key:value'. Ignoring."  )
                    }
                    meta.put(line.substring(0, splitIdx), line.substring(splitIdx + 1))
                }
            } else { // Parsing section
                sectionBuffer += "${line} \n"
            }


            line = reader.readLine()
            lineNumber ++
        }

        if (parsingLang != null) sections.put(parsingLang, sectionBuffer)


        sections.forEach { tokenizedSections[it.key] = Triple(parseSection(filename, titles[it.key]!!.second, it.key, titles[it.key]!!.first, genFormat, meta, it.value), meta["anchor-name"]!!, titles[it.key]!!.second) }

        articleList.add(Triple(meta["date"]!!, meta["anchor-name"]!!, titles.mapValues { it.value.second }))
        idFromAnchor.put(meta["anchor-name"]!!, meta["id"]!!)

        return tokenizedSections
    }

    private val PARSING_NOTHING = 0
    private val PARSING_PARAGRAPH = 1
    private val PARSING_BLOCK_CODE = 2

    private val PARSING_INLINE_TEXT = 1
    private val PARSING_INLINE_CODE_BLOCK = 2

    // We don't need to support images/links definitions accorss multiple lines. Regex will do.
    private val REGEX_IMAGE = Regex("!\\[([^]]*)]\\(([^)]*)\\)")
    private val REGEX_LINK = Regex("\\[([^]]*)]\\(([^)]*)\\)")


    private val idFromAnchor = HashMap<String, String>()
    fun getIdFromAnchor(anchor: String) = idFromAnchor.getOrDefault(anchor, "0000")


    private fun parseLine(builder: StringBuilder, parsingInlineType: Int, lNumber: Int, genFormat: GenFormat, meta: HashMap<String, String>, line: String): Int {

        fun findExtraInLine(s: String): String {
            val imRes = REGEX_IMAGE.find(s)
            if (imRes != null) {
                return escape(s.substring(0, imRes.range.start), genFormat.escape) + genFormat.image(escape(imRes.groupValues[1], genFormat.escape), imRes.groupValues[2]) + findExtraInLine(s.substring(imRes.range.last + 1))
            } else {
                val linkRes = REGEX_LINK.find(s)
                if (linkRes != null) {
                    return escape(s.substring(0, linkRes.range.start), genFormat.escape) + genFormat.link(escape(linkRes.groupValues[1], genFormat.escape), linkRes.groupValues[2]) + findExtraInLine(s.substring(linkRes.range.last + 1))
                }
            }

            if (s.trimEnd().endsWith("\\\\")) {
                return escape(s.trimEnd().substring(0, s.trimEnd().length - 2), genFormat.escape) + genFormat.newLine
            } else {
                return escape(s, genFormat.escape)
            }
        }

        fun nonBreakingSpace(string: String): String {
            return " ${string}".replace(Regex(" ([a-zA-Z0-9]) "), { " ${it.groupValues[1]}${genFormat.notBreakingSpace}" }).replace("~", genFormat.notBreakingSpace).replace(genFormat.notBreakingSpace + genFormat.notBreakingSpace, "~").substring(1)
        }

        val indexOfInlineCodeBlock = line.indexOf("`")

        return if (indexOfInlineCodeBlock != -1) {
            val s = line.substring(0, indexOfInlineCodeBlock)

            builder.append(if (parsingInlineType == PARSING_INLINE_TEXT) nonBreakingSpace(findExtraInLine(s)) else escapeInlineBlock(s, genFormat))

            if (line.substring(indexOfInlineCodeBlock + 1).startsWith("`")) {
                if (genFormat == GenFormat.TEX) {
                    if (parsingInlineType == PARSING_INLINE_CODE_BLOCK) builder.append(genFormat.inlineCodeEnd)
                    builder.append(escape("`", genFormat.escape))
                    if (parsingInlineType == PARSING_INLINE_CODE_BLOCK) builder.append(genFormat.inlineCodeStart)
                }

                parseLine(builder, parsingInlineType, lNumber, genFormat, meta, line.substring(indexOfInlineCodeBlock + 2))
            } else {

                if (parsingInlineType == PARSING_INLINE_CODE_BLOCK) {
                    builder.append(genFormat.inlineCodeEnd)
                    parseLine(builder, PARSING_INLINE_TEXT, lNumber, genFormat, meta, line.substring(indexOfInlineCodeBlock + 1))
                } else {
                    builder.append(genFormat.inlineCodeStart)
                    parseLine(builder, PARSING_INLINE_CODE_BLOCK, lNumber, genFormat, meta, line.substring(indexOfInlineCodeBlock + 1))
                }
            }
        } else {
            builder.append(if (parsingInlineType == PARSING_INLINE_TEXT) nonBreakingSpace(findExtraInLine(line)) else escapeInlineBlock(line, genFormat))
            parsingInlineType
        }
    }

    private fun parseSection(filename: String, title: String, lang: String, baseLineNumber: Int, genFormat: GenFormat, meta: HashMap<String, String>, section: String): String {

        val builder = StringBuilder(genFormat.sectionStart)

        builder .append(genFormat.sectionTitleStart)
                .append(escape(title, genFormat.escape))
                .append(genFormat.sectionTitleEnd)

        val lines = section.split("\n")

        var parsingParagraphType = PARSING_NOTHING
        var parsingInlineType = PARSING_NOTHING

        var lNumber = baseLineNumber
        for (l in lines) {
            if (l.trim() == "```") {
                if (parsingInlineType != PARSING_INLINE_TEXT) {
                    if (parsingInlineType == PARSING_INLINE_CODE_BLOCK) {
                        parsingError(filename, lNumber, false, "Found opened inline code block. Closing")
                        builder.append(genFormat.inlineCodeEnd)
                        parsingInlineType = PARSING_INLINE_TEXT
                    }
                }
                if (parsingParagraphType == PARSING_NOTHING || parsingParagraphType == PARSING_PARAGRAPH) {
                    if (parsingParagraphType == PARSING_PARAGRAPH) builder.append(genFormat.paragraphEnd)
                    parsingParagraphType = PARSING_BLOCK_CODE
                    parsingInlineType = PARSING_INLINE_TEXT
                    builder.append(genFormat.blockCodeStart)
                } else if (parsingParagraphType == PARSING_BLOCK_CODE) {
                    parsingParagraphType = PARSING_NOTHING
                    parsingInlineType = PARSING_INLINE_TEXT
                    builder.append(genFormat.blockCodeEnd)
                }
            } else {
                if (parsingParagraphType == PARSING_BLOCK_CODE) {
                    if (genFormat == GenFormat.TEX) {
                        builder.append(l.replace("\\endtt", "/endtt")).append(System.lineSeparator())
                    } else {
                        builder.append(escape(l, genFormat.escape)).append(System.lineSeparator())
                    }
                } else if (parsingParagraphType == PARSING_PARAGRAPH && l.isNotBlank()) {
                    parsingInlineType = parseLine(builder, parsingInlineType, lNumber, genFormat, meta, l)
                } else if (parsingParagraphType == PARSING_PARAGRAPH && l.isBlank()) {
                    if (parsingInlineType != PARSING_INLINE_TEXT) {
                        if (parsingInlineType == PARSING_INLINE_CODE_BLOCK) {
                            parsingError(filename, lNumber, false, "Found opened inline code block. Closing")
                            builder.append(genFormat.inlineCodeEnd)
                            parsingInlineType = PARSING_INLINE_TEXT
                        }
                    }
                    builder.append(genFormat.paragraphEnd)
                    parsingParagraphType = PARSING_NOTHING
                } else if (parsingParagraphType == PARSING_NOTHING && l.isNotBlank()) {
                    builder.append(genFormat.paragraphStart)
                    parsingInlineType = parseLine(builder, PARSING_INLINE_TEXT, lNumber, genFormat, meta, l)
                    parsingParagraphType = PARSING_PARAGRAPH
                } else if (parsingParagraphType == PARSING_NOTHING && l.isBlank()) {
                    // Skip blank line
                } else {
                    parsingError(filename, lNumber, true, "Parser got to unexpected state: PARSING_PARAGRAPH=${ when (parsingParagraphType) {
                            PARSING_NOTHING -> "NOTHING"
                            PARSING_PARAGRAPH -> "PARAGRAPH"
                            PARSING_BLOCK_CODE -> "BLOCK_CODE"
                            else -> "(${parsingParagraphType})"
                        }
                    }")
                }
            }

            lNumber++
        }
        if (parsingParagraphType == PARSING_PARAGRAPH) {
            if (parsingInlineType != PARSING_INLINE_TEXT) {
                if (parsingInlineType == PARSING_INLINE_CODE_BLOCK) {
                    parsingError(filename, lNumber, false, "Found opened inline code block. Closing")
                    builder.append(genFormat.inlineCodeEnd)
                    parsingInlineType = PARSING_INLINE_TEXT
                }
            }
            builder.append(genFormat.paragraphEnd)
            parsingParagraphType = PARSING_NOTHING
        }

        if (parsingParagraphType != PARSING_NOTHING) {
            parsingError(filename, lNumber, true, "Parser got to unexpected state. Expected PARSING_PARAGRAPH=PARSING_NOTHING, but instead PARSING_PARAGRAPH=${ when (parsingParagraphType) {
                PARSING_NOTHING -> "NOTHING"
                PARSING_PARAGRAPH -> "PARAGRAPH"
                PARSING_BLOCK_CODE -> "BLOCK_CODE"
                else -> "(${parsingParagraphType})"
            }
            }")
        }

        val id = meta.get("id")?.toInt() ?: -1
        if (id == -1) {
            parsingError(filename, 0, true, "File does not contain meta tag 'id'")
        }

        val authors = meta.getOrDefault("authors", meta.get("author"))
        if (authors == null) {
            parsingError(filename, 0, true, "File does not contain meta tag 'author'")
        }

        val date = meta.get("date")
        if (date == null) {
            parsingError(filename, 0, true, "File does not contain meta tag 'date'")
        }

        val authArray = authors!!.split(",").map { if (it.contains("<") && it.contains(">")) Pair(it.substring(0, it.indexOf("<")).trim(), it.substring(it.indexOf("<") + 1, it.indexOf(">")).trim()) else Pair(it.trim(), null) }.toTypedArray()

        builder.append(genFormat.extra(meta.get("id")!!, authArray, date!!, lang))

        meta.put("lang", lang)
        return genFormat.postProcessArticles(builder.append(genFormat.sectionEnd).toString(), meta)

    }

    val articleList = ArrayList<Triple<String, String, Map<String, String>>>()

    private fun escape (source: String, escape: List<Pair<String, String>>): String {
        var data = source
        escape.forEach { data = data.replace(it.first, it.second) }
        return data
    }
    private fun escapeInlineBlock (source: String, format: GenFormat): String {
        return if (format == GenFormat.TEX) {
            var data = source
            format.escape.filter { it.first == "<" || it.first == ">" }.forEach { data = data.replace(it.first, format.inlineCodeEnd + it.second + format.inlineCodeStart) }
            data
        } else source
    }


}