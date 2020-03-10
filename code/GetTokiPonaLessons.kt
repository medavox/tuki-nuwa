import khttp.get
import java.io.File

fun main(args:Array<String>) {
    val baseAddress = "http://tokipona.net/tp/janpije/okamasona%s.php"
    dl(baseAddress.format(""))
    for(num in 1..19) {
        dl(baseAddress.format(num))
        Thread.sleep(100)
    }
}

private fun dl(url:String) {
    val qwa = url.split("/")
    val dest = File(qwa[qwa.size-1])
    if(dest.exists()){
        System.out.println("$dest already exists, skipping...")
        return
    }
    System.out.println("getting $url...")
    val resp = get(url)

    dest.writeText(resp.text)
}