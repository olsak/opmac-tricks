package opmac.tokenizer

/**
 *
 */
data class GenFormat (

        val name: String = "?",
        val escape: List<Pair<String, String>>,

        val sectionTitleStart: String,
        val sectionTitleEnd: String,
        val inlineCodeStart: String,
        val inlineCodeEnd: String,
        val blockCodeStart: String,
        val blockCodeEnd: String,
        val emphasisStart: String,
        val emphasisEnd: String,
        val paragraphStart: String,
        val paragraphEnd: String,
        val newLine: String,
        val notBreakingSpace: String,

        val image: (alt: String, uri: String) -> String,
        val link: (text: String, uri: String) -> String,

        val extra: (id: String, authors: Array<Pair<String, String?>>, date: String, lang: String) -> String,

        val sectionStart: String,
        val sectionEnd: String,

        val postProcessArticles: (article: String, meta: HashMap<String, String>) -> String = { article, meta -> article },
        val postProcessSections: (sections: ArrayList<Section>) -> String = { it.map { "${it.titleInLang}: ${it.articlesCode}\n" }.reduce { s1, s2 -> "${s1}${s2}" } },

        val generateToc: (sections: ArrayList<Section>) -> String = { "" },

        val mostRecent: (articleList: ArrayList<Triple<String, String, Map<String, String>>>, count: Int, langCode: String) -> String = { _, _, _ -> "" }

) {

    companion object {

        val HTML: GenFormat = GenFormat(
                "html",
                listOf(Pair("&", "&amp;"), Pair("<", "&lt;"), Pair(">", "&gt;")),

                "<header><div class='pb-2'><h3 class='d-inline'>",
                "</h3> <a href='#\$ANCHOR_NAME\$'><i class='fas fa-link'></i></a></div></header><section>",
                "<code>",
                "</code>",
                "<pre class='pr-3'><code>",
                "</code></pre>",
                "<strong>",
                "</strong>",
                "<p>",
                "</p>",
                "<br>",
                "&nbsp;",

                { alt, link -> "<img src='${link}' alt='${alt}' title='${alt}' class='img-fluid rounded mx-auto my-2 d-block'>" },
                { text, link -> "<a href='${link}' title='${text}'>${text}</a>" },

                { id, authors, date, lang ->
                    val otherLang = if (lang.equals("cs")) "en" else "cs"
                    "</section><footer class='text-right'>" +
                            "<a href='opmac-tricks-${otherLang}.html#\$ANCHOR_NAME\$' class='float-left badge badge-secondary'>${Translations.get(otherLang, "langName")} <img src='img/${otherLang}.png' style='max-height: 1rem;'></a> " +
                            "<span class='badge badge-info'>ID: ${id}</span> " +
                            authors.map { "<span class='badge badge-info'>\$LANG_AUTHOR\$: ${if (it.second != null && it.second!!.isNotBlank()) "<a href='mailto:${it.second}'>${it.first}</a>" else it.first}</span>" }.reduce { _1, _2 -> "${_1} ${_2}" } +
                            " <span class='badge badge-info'>\$LANG_DATE\$ <time datetime='${date}'>${Translations.getDate(lang, date)}</time></span> " +
                    "</footer>"
                },

                "<article id='\$ANCHOR_NAME\$' class='mb-2 p-3 bg-white rounded border'>",
                "</article>",

                { article, meta ->
                    article .replace("\$ANCHOR_NAME\$", meta.getOrDefault("anchor-name", ""))
                            .replace("\$LANG_AUTHOR\$", Translations.get(meta["lang"]!!, "author"))
                            .replace("\$LANG_DATE\$", Translations.get(meta["lang"]!!, "published"))
                },

                { sections ->
                    sections.map {

                        """ <section id="${it.id}" class="mb-5 rounded my-shadow" style="background-color: #778c94">
                                <header class="text-center">
                                  <div class="py-2">
                                    <h2 class="d-inline text-light font-weight-light">${it.titleInLang}</h2>
                                    <a href="#${it.id}"><i class="fas fa-link"></i></a>
                                  </div>
                                </header>
                                ${it.articlesCode}
                            </section>
                        """.trimIndent()

                    }.reduce{ s1, s2 -> "${s1} ${s2}" }
                },

                { sections ->
                    "<ul class='my-3 p-0'>" +
                    sections.map {
                        """
                        <li><a href='#${it.id}'>${it.titleInLang}</a>
							<ul>
                                ${it.articleTitles.map { "<li><a href='#${it.first}'>${it.second}</a></li>" }.reduce{ s1, s2 -> s1 + s2 }}
							</ul>
						</li>
                        """.trimIndent()
                    }.reduce{ s1, s2 -> s1 + s2 } + "</ul>"
                },

                { articleList, count, lang ->

                    """

                    <div class="card mx-auto mb-5 my-shadow" style="max-width: 520px">
                    <div class="card-header text-center">
                        <strong>${Translations.get(lang, "mostRecent")}</strong>
                    </div>
                    <div class="list-group list-group-flush">

                    ${articleList.take(count).map {

                        """
                        <a href="#${it.second}" class="list-group-item list-group-item-action d-flex justify-content-between">
                            ${it.third[lang]}
                            <small>${Translations.getDate(lang, it.first)}</small>
                        </a>
                        """.trimIndent()

                    }.reduce { s1, s2 -> "${s1} ${s2}"}}

                    </div></div>

                    """.trimIndent()

                }
        )


        val TEX: GenFormat = GenFormat(
                "tex",
                // {\tt\char`\{}
                listOf(Pair("#", "§#"), Pair("%", "§%"), Pair("{", "{§tt§char123!§§§§§§"), Pair("}", "{§tt§char125}"), Pair("!§§§§§§", "}"), Pair("$", "{§tt§char36\$}"), Pair("`", "{§tt§char96}"), Pair("<", "{§tt§char60}"), Pair(">", "{§tt§char62}"), Pair("&", "§&"), Pair("\$\$", ""), Pair("~", "§~{}"), Pair("_", "§_"), Pair("^", "§^{}"), Pair("\\", "{\\tt\\char92}"), Pair("§", "\\")),

                "\\trick[\$ARTICLE_ID\$]{",
                "}{\$ARTICLE_AUTHOR\$}{\$ARTICLE_DATE\$}\n",
                "`",
                "`",
                "\\begtt\n",
                "\\endtt\n",
                "\\textbf{",
                "}",
                "\n\n",
                "\n\n",
                " \\\n",
                "~",

                { alt, link -> "\\inspic {$link}" },
                { text, link -> if (link.startsWith("#")) "\\link[ref:${Parser.getIdFromAnchor(link.substring(1))}]{}{$text}" else "\\ulink[$link]{$text}" },

                { _, _, _, _ -> "" },

                "\n",
                "\n",

                { article, meta ->
                    article .replace("\$ANCHOR_NAME\$", meta.getOrDefault("anchor-name", ""))
                            .replace("\$LANG_AUTHOR\$", Translations.get(meta["lang"]!!, "author"))
                            .replace("\$LANG_DATE\$", Translations.get(meta["lang"]!!, "published"))
                            .replace("\$ARTICLE_ID\$", meta["id"]!!)
                            .replace("\$ARTICLE_AUTHOR\$", meta["author"]!!)
                            .replace("\$ARTICLE_DATE\$", Translations.getDate(meta["lang"]!!, meta["date"]!!))
                },

                { sections ->
                    sections.map { "\\sec ${it.titleInLang} \n\n ${it.articlesCode}\n" }.reduce{ s1, s2 -> "${s1} ${s2}" }
                },

                { _ -> "" },

                { _, _, _ -> "" }
        )

        val FORMATS: Array<GenFormat> = arrayOf(HTML, TEX)

        fun byName (name: String) = FORMATS.filter { it.name.equals(name, ignoreCase = true) }.getOrNull(0)

    }

}
