package opmac.tokenizer

import opmac.tokenizer.Generator.generateForFormat
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader

/**
 *
 */


fun parsingError(filename: String, linenumber: Int, critical: Boolean, message: String) {
    System.err.println("Parsing problem ${if (critical) "[ERROR]" else "[WARN]"} in ${filename}:${linenumber}\n${message}\n")
    if (critical) throw Error("Parsing parsingError")
}
fun genError(critical: Boolean, message: String) {
    System.err.println("Generating problem ${if (critical) "[ERROR]" else "[WARN]"} ${message}\n")
    if (critical) throw Error("Parsing genError")
}

fun main(args: Array<String>) {
    var nextInput = false
    var nextOutput = false
    var nextTemplate = false
    var nextMostRecent = false
    var input: String? = null
    var output: String? = null
    var templates: String? = null
    var mostRecentCount = 0

    args.forEach {
        if (nextInput) {
            input = it
            nextInput = false
        } else if (nextOutput) {
            output = it
            nextOutput = false
        } else if (nextTemplate) {
            templates = it
            nextTemplate = false
        } else if (nextMostRecent) {
            try {
                mostRecentCount = it.toInt()
            } catch (e: NumberFormatException) {
                System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
                System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
                System.exit(1)
            }
            nextMostRecent = false
        } else if (it.equals("-i") || it.equals("--input")) {
            if (input == null) {
                nextInput = true
            } else {
                System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
                System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
                System.exit(1)
            }
        } else if (it.equals("-o") || it.equals("--output")) {
            if (output == null) {
                nextOutput = true
            } else {
                System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
                System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
                System.exit(1)
            }
        } else if (it.equals("-t") || it.equals("--template")) {
            if (templates == null) {
                nextTemplate = true
            } else {
                System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
                System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
                System.exit(1)
            }
        } else if (it.equals("-r") || it.equals("--recents")) {
            nextMostRecent = true
        } else {
            System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
            System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
            System.exit(1)
        }
    }

    if (input == null) {
        System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
        System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
        System.exit(1)
    }
    if (output == null) {
        System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
        System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
        System.exit(1)
    }
    if (templates == null) {
        System.err.println("Argument parsingError. Expected args (tuples in arbitrary order): ")
        System.err.println("    -i|--input path/to/folder/with/input/files -o|--output path/where/to/place/generated/stuff -t|--template path/to/dir/with/template/files [-r|--recents amount]")
        System.exit(1)
    }

    val tFile = File(File(templates), "translations.txt")
    if (!tFile.canRead()) {
        System.err.println("Translations file was not found (${tFile.absolutePath})")
        System.exit(1)
    }

    if (!File(input).isDirectory) {
        System.err.println("Directory with input files '${tFile.absolutePath}' was not found")
        System.exit(2)
    }

    Translations.load(tFile)

    GenFormat.FORMATS.forEach {
        var templateReader: BufferedReader? = null
        try {
            templateReader = BufferedReader(FileReader(templates!! + "/template.${it.name}"))
        } catch (e: FileNotFoundException) {
            System.err.println("Could not find file '" + templates!! + "/template.${it.name}")
            System.exit(1)
        }

        generateForFormat(it, templateReader!!.readText(), output!!, input!!, mostRecentCount)
    }

    if (!File(templates).absoluteFile.equals(File(output).absoluteFile)) {
        val skipFiles = listOf("translations.txt", "template.html", "template.tex")
        val filesToCopy = File(templates).listFiles().filter { !skipFiles.contains(it.name.toString()) }
        for (f in filesToCopy) {
            val src = File(f.toString())
            if (src.isDirectory) {
                src.copyRecursively(File(output, src.name), overwrite = true)
            } else {
                src.copyTo(File(output, src.name), overwrite = true)
            }

        }
    }

    if (File(input, "img").exists()) {
        File(input, "img").copyRecursively(File(output, "img"), overwrite = true)
    }


    println("Done. Results stored in '${output}'")

}


