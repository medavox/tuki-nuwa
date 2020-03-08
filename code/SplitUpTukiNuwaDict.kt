import java.io.File
import java.io.IOException

fun main(args:Array<String>) {
    if(args.size < 1) {
        println("provide a file argument!")
        return
    }
    val file = File(args[0])
    val isValid = file.isFile && file.canRead() && file.exists() && file.length() < (10240 * 1024) &&
            file.name.endsWith(".md")
    if(!isValid) {
        println("provide a valid file!")
        return
    }
    val dest = File("./entries")
    dest.mkdir()
    val byLine = file.readText().split("\n".toRegex()).filter { it.isNotEmpty() }
    val wordTypes = arrayOf("Word", "Noun", "Verb", "Modifier", "Other")
    for(line in byLine) {
        val partsOfSpeech = line.split("|")
        //println(partsOfSpeech)
        val lexeme = partsOfSpeech[0].trim()
        val entry = File(dest, "${lexeme.replace(Regex("[^a-zA-Z0-9.-]"), "-")}.md")
        while(true) {
            try {
                entry.createNewFile()
                break
            } catch (ioe: IOException) {
                ioe.printStackTrace()
                println("IOException! offending file: $entry")
                Thread.sleep(300)
            }
        }
        entry.writeText("$lexeme\n===")
        for(pos in 1 until partsOfSpeech.size) {
            if (partsOfSpeech[pos].isNotBlank()) {
                entry.appendText("\n\n"+wordTypes[pos]+"\n---\n\n"+partsOfSpeech[pos].trim())
            }
        }
    }
}