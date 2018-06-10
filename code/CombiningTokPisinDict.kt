import java.io.File

private const val dir = "/Users/adamh/src/tuki-nuwa/offline-saved-resources/tok-pisin-dict"
private val wholeDict = File("$dir/whole-dict.html")
fun main(args: Array<String>){
    combineDict()
    cleanDict()
}

private fun cleanDict(){
    if(!wholeDict.exists()) {
        System.err.println("$wholeDict does not exist")
        return
    }
    System.out.println("old size: "+wholeDict.length())
    val dictContents = wholeDict.readText()
    //<a href="define.php?english=tape-recorder&id=MTkyMQ==">tape-recorder</a>
    //[0-9a-zA-Z ,)('/.;?!&-]
    val reg = Regex("<a href=\"define.php\\?(english|tokpisin)=[a-z0-9_?-]+&id=[a-zA-Z0-9]+=?=?\">(\\p{Print}+)</a>")
    wholeDict.writeText(dictContents.replace(reg, "$2"))
    System.out.println("new size: "+wholeDict.length())
}

private fun combineDict() {
    val dictDir = File(dir)
    val output = StringBuilder()
    output.append("<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head><title>English - Tok Pisin Dictionary</title>\n" +
            "<link rel=\"stylesheet\" href=\"style.css\">\n" +
            "</head>\n<table>")
    //System.out.println("file \"${dictDir.name}\" ")
    for(file : File in dictDir.listFiles().filter { it.isFile && it.extension == "html"
            && it.name != wholeDict.name}) {
        //val fileContents = StringUtils.fileToString(file)
        val fileContents = file.readText()

        System.out.println("file \"${file.name}\" length:"+fileContents.length)
        val regex = Regex(".+<table[^>]+>(.+)</table>.+", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        //val tableContents =regex.
        val tableContents = fileContents.replace(regex, "$1")
        //System.out.println("table length:"+tableContents.length)
        output.append(tableContents)
    }
    output.append("</table></html>")

    wholeDict.writeText(output.toString())
}