package opmac.tokenizer

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 *
 */


object Translations {

    private val translations = HashMap<String, String>()

    fun load(f: File) {
        val reader = BufferedReader(FileReader(f))
        reader.readLines().filter { it.isNotBlank() && it.indexOf('.') < it.indexOf(':') && it .indexOf('.') != -1 }.forEach { translations[it.substring(0, it.indexOf(":")).toUpperCase()] = it.substring(it.indexOf(":") + 1) }
    }


    fun get(langIso: String, query: String): String {
        val lq = translations["${langIso.toUpperCase()}.${query.toUpperCase()}"]
        if (lq == null) {
            val dlq = translations["CS.${query.toUpperCase()}"]
            if (dlq == null) {
                genError(true, "Missing translation for query \$${query.toUpperCase()}\$")
                return "\$${query.toUpperCase()}\$"
            }
            genError(false, "Missing '${langIso}' translation for query \$${query.toUpperCase()}\$, using 'cs' translation instead")
            return dlq
        }
        return lq
    }

    fun getDate(langIso: String, dateIso: String): String {
        val yyyy = dateIso.substring(0, 4)
        val mm = dateIso.substring(5, 7)
        val dd = dateIso.substring(8, 10)

        return when (langIso) {
            "en" -> { "${mm}/${dd}/${yyyy}" }
            "cs" -> { "${dd}. ${mm}. ${yyyy}" }
            else -> dateIso
        }

    }

}