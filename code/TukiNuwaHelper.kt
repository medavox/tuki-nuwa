//package default;

import java.util.*

import java.io.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

private val e = System.err
private val o = System.out

/**
I will definitely not add sounds (phonemes) to the phonology (well, except for h);
I don't know enough about linguistic typology to choose new sounds
(or new phonotactic rules) that are as easy for all humans to pronounce.

Instead, I'm merely using the raw material given to me
by Toki Pona's phonology and phonotactics
to construct new words.

 This file helps with the boilerplate maths of it all:

 * given 3 vowels and 10 consonants, how many possible single-syllable words are there?<br />
 * how many possible 2-syllable words?<br />
 * and 3-syllable words?<br />
<br />
 * Do all the words in the dictionary follow the phonological rules (as they currently stand)?

 * Given the words in the dictionary, what are some unused sounds that could be used for new words?
 */
private const val stops = "ptk"
private const val nasals = "mn"
private const val fricatives = "hs"
private const val approximants = "lwj"

private const val consonants = stops+nasals+fricatives+approximants
private const val vowels = "aiu"

private val forbiddenSyllables = arrayOf("ji", "ti", "wu")
private val lel = mapOf(
        'a' to setOf('u'),
        'm' to setOf('n', 'p'),
        'w' to setOf('m', 'l', 'j')
)
private const val allowSyllableFinalN = true

//ansi colour-terminal escape codes
//https://en.wikipedia.org/wiki/ANSI_escape_code#Escape_sequences
private const val CSI = "\u001B["
private const val reset = CSI+"0m"
private const val RED = CSI+"31m"
private const val YELLOW = CSI+"33m"
private val validWord = Regex("[$consonants]?[$vowels]n?([$consonants][$vowels]n?){0,2}")

fun main(args: Array<String>) {
    if(args.isEmpty()){
        e.println("ERROR: at least command is required.")
        return
    }
    val t = SyllableGenerator()
    val command: String = args[0].toLowerCase()
    when(command) {
        // print all possible syllables
        "syllables", "s" -> {
            if(Regex("[1-3]").matches(args[1])) {
                //print syllables
                val words: Set<String> = when(args[1].toInt()) {
                    1 -> t.listSingleSyllableWords()
                    2 -> t.listDoubleSyllableWords()
                    3 -> t.listTripleSyllableWords()
                    else -> emptySet()
                }

                for (s: String in words) {
                    o.println(s)
                }
                o.println("total: "+words.size)
            }else {
                e.println("was expecting a number argument 1-3.")
            }
            return
        }

        // print possible words considered similar to the supplied word
        "similar", "si" -> {//these are generated from the supplied word
            for(s in similarWordsTo(args[1])) {
                o.println(s)
            }
            return
        }

        //print anagrams of the supplied word that are valid tuki nuwa words
        "anagram", "a" -> {
            //val anagrams: Set<String> = t.anagram("", args[1].toList(), TreeSet<String>())
            val afterLastLetterNoHyphen = args[1].substring(1).replace("-", "")
            val anagrams: Set<String> = anagram(args[1][0].toString(), afterLastLetterNoHyphen, TreeSet<String>())
            for (anagram in anagrams) {
                if(anagram != args[1]) {
                    o.println("ANAGRAM:$anagram")
                }
            }
            return
        }
    }

    //the other commands require a second argument of a dictionary file,
    //so parse the second argument as that
    val dict = File(args[1])
    if(!dict.isValidFile()) {
        e.println("invalid file \"$dict\" specified.")
        return
    }

    val dictionary = scrapeWordsFromDictionary(dict)
    when(command) {
        //check the dictionary for errors, both proper and bad ideas
        "lint", "l" -> {
            var complaints = 0
            if(args.size == 3) {
                complaints += lintAWordAgainstTheDictionary(args[2], dictionary)
            }else{
                for(word in dictionary) {
                    val dictionaryWithoutWord = (dictionary.toMutableList() - word).toTypedArray()
                    complaints += lintAWordAgainstTheDictionary(word, dictionaryWithoutWord)
                }
            }
            o.println("total complaints: "+complaints)
        }
        //display frequencies of letter usage in the dictionary words
        "frequency", "f" -> o.println(analyseLexicalFrequency(dictionary))
        //display all or some (according ot a query) unused lexemes
        "unused", "u", "query", "q" -> {
            val query = if(args.size > 3) args[3] else ""
            o.println(queryUnusedWords(t, dictionary, args[2], query))
        }
        //display 10 or a specified number of random unused lexemes
        "random", "r" -> {
            val numberOfRandomWords:Int = if(args.size > 2) args[2].toInt() else 10
            o.println(randomUnusedWords(t, dictionary, numberOfRandomWords, args.size > 3))
        }
        //screen potential lexemes against our linting, and the dictionary
        "screen" -> {
            if(args.size < 3) {
                e.println(RED+"ERROR: must specify a file listing potential words as well"+reset)
            }
            else {
                val potFile = File(args[2])
                if(!potFile.isValidFile()) {
                    e.println("invalid file \"$dict\" specified.")
                    return
                }
                val potentials = potFile.readText()
                        .split("\n".toRegex())
                        .filter { it.isNotEmpty() }
                        .toTypedArray()
                screenPotentials(potentials, dictionary)
            }
        }
    }
}

private fun File.isValidFile(): Boolean {
    return (this.exists() && this.isFile && this.canRead() && this.length() > 5)

}

fun screenPotentials(potentials:Array<String>, dict:Array<String>) {
    val uniq = potentials.distinct().toTypedArray()
    val screened = uniq.filter { lintAWordAgainstTheDictionary(it, dict) == 0 }
    for(item in screened) {
        o.println(item)
    }
    o.println("items before: "+potentials.size+"; items after: "+screened.size)
}

fun randomUnusedWords(t:SyllableGenerator, dictionary:Array<String>, number:Int,
                      noTrisyllabics:Boolean = false) :String {
    val ret = StringBuilder()

    var unusedLexemes:MutableSet<String> = getUnusedLexemes(t, dictionary).toMutableSet()
    if(noTrisyllabics) {
        unusedLexemes = unusedLexemes.filter { it.count { it in vowels } < 3 }.toMutableSet()
    }
    o.println("usable unused lexemes:"+unusedLexemes.size)
    if(number >= unusedLexemes.size) {
        ret.appendln(RED+"there aren't that many unused lexemes left."+reset)
        ret.appendln("remaining unused lexemes:")
        for(lex in unusedLexemes) {
            ret.appendln(lex)
        }
        return ret.toString()
    }
    val r = Random()
    for(i in 1..number) {
        var word:String
        do{
            word = unusedLexemes.elementAt(r.nextInt(unusedLexemes.size))
        }
        while(lintAWordAgainstTheDictionary(word, dictionary, false) > 0)
        unusedLexemes = (unusedLexemes - word).toMutableSet() //bag random
        ret.appendln(word)
    }
    return ret.toString()
}

fun analyseLexicalFrequency(dictionary:Array<String>): String {
    val ret = StringBuilder()
    val firstLetterFreqs: MutableMap<Char, Int> = HashMap()
    val letterFreqs: MutableMap<Char, Int> = HashMap()
    val wordsWithSyllableFinalN = mutableSetOf<String>()
    for(word in dictionary) {
        val c = word[0]
        if (consonants.contains(c) || vowels.contains(c)) {
            firstLetterFreqs[c] = firstLetterFreqs[c]?.plus(1) ?: 1
        }
        if(word.endsWith("n")) {
            wordsWithSyllableFinalN.add(word)
        }else {
            for(i in word.indices) {
                if(vowels.contains(word[i])
                        && word.length > i+2
                        && word[i+1] == 'n'
                        && consonants.contains(word[i+2]) ){
                    wordsWithSyllableFinalN.add(word)
                }
            }
        }
        for(letter in word) {
            if (consonants.contains(c) || vowels.contains(c)) {
                letterFreqs[letter] = letterFreqs[letter]?.plus(1) ?: 1
            }
        }
    }

    //o.println("${wordsWithSyllableFinalN.size} words with syllable-final n: $wordsWithSyllableFinalN")

    ret.appendln("first-letter frequencies:")
    for((key, value) in firstLetterFreqs.toList().sortedBy {(_, v) -> v}.toMap()) {
        ret.appendln("$key: $value")
    }

    ret.appendln("all-letter frequencies:")
    for((key, value) in letterFreqs.toList().sortedBy {(_, v) -> v}.toMap()) {
        ret.appendln("$key: $value")
    }
    return ret.toString()
}

private fun isAValidTukiNuwaSubstring(string:String): Boolean {
    if(containsForbiddenSyllable(string)) {
        return false
    }
    //check if string contains illegal characters,
    //by removing all valid characters and seeing if the string is empty or not afterwards
    var removingValidChars = string
    for(conso in consonants) {
        removingValidChars = removingValidChars.replace(conso.toString(), "")
    }
    for(vowel in vowels) {
        removingValidChars = removingValidChars.replace(vowel.toString(), "")
    }
    if(!removingValidChars.isEmpty()) {
        return false
    }
    //check characters in provided string are ordered into valid sequences
    val regex = Regex("[$vowels]?([$consonants][$vowels]n?){0,2}(n|[$consonants])?")
    return regex.matches(string)
}

private fun getUnusedLexemes(t:SyllableGenerator, dictionary: Array<String>): Set<String> {
    var totalSimilarWordsToDictionaryWords = 0
    val allPossibleWords:MutableSet<String> = (
            t.listSingleSyllableWords() +
                    t.listDoubleSyllableWords() +
                    t.listTripleSyllableWords()
            ).toMutableSet()

    //String[] wordsFromDictionary = scrapeWordsFromDictionary(dictionaryFile);
    for (word in dictionary) {
        allPossibleWords -= word
        for (similarWord in similarWordsTo(word)) {
            //o.println(similarWord)
            allPossibleWords -= similarWord
            totalSimilarWordsToDictionaryWords++
        }
    }
    o.println("unused lexemes: ${allPossibleWords.size}")
    return allPossibleWords
}

/**list unused potential words which aren't too similar to existing words */
fun queryUnusedWords(t:SyllableGenerator, dictionary:Array<String>, string:String,
                     query:String):String {
    val ret = StringBuilder()

    val usageString = "Print 1, 2 or 3 syllable possible words, or any combination thereof." +
            "or only print words BEGINNING or ending with the given sequence\n" +
            "(the given string must be phonotactically valid.)\n" +
            "\n" +
            "The query string can contain:\n" +
            "* any (or none) or 1, 2 or 3 to print words with any of those numbers of syllables\n" +
            "    (both 123 and no numbers print words with either 1, 2 or 3 syllables)\n" +
            "* either, neither or both of s or e,\n" +
            "    to print words that start (and/or) end with the provided string\n" +
            "    adding both s and e prints words which either start or end with it,\n" +
            "    adding neither prints words which contain the string anywhere"
    //populate the list of all potential words,
    // then subtract all the dictionary words (and similar) from it
    if(!isAValidTukiNuwaSubstring(string)) {
        return ret.append(RED)
                .append("ERROR: supplied string \"$string\" cannot occur in a Tuki Nuwa word.")
                .appendln(reset)
                .appendln(usageString)
                .toString()
    }

    val unusedLexemes = getUnusedLexemes(t, dictionary)

    if(query == "") {
        ret.appendln("word containing \"$string\":")
        var matchingWords = 0
        for(unusedWord in unusedLexemes.filter { it.contains(string) }) {
            ret.appendln(unusedWord)
            matchingWords++
        }
        ret.appendln("matching words: $matchingWords")
    }
    else {

        ret.appendln("QUERY MODE")
        var startsWith = false
        var endsWith = false
        val syllableSizes:MutableList<Int> = mutableListOf()
        for(character in query) {
            when {
                character.isDigit() -> syllableSizes.add(character.toString().toInt())
                character == 's' -> startsWith = true
                character == 'e' -> endsWith = true
            }
        }
        //syllableSizes = mutableListOf<Int>(1, 2, 3)
        var matchingWords = 0
        for(unusedWord in unusedLexemes.filter { when {
            startsWith && endsWith -> it.startsWith(string) || it.endsWith(string)
            startsWith && !endsWith -> it.startsWith(string)
            !startsWith && endsWith -> it.endsWith(string)
            !startsWith && !endsWith -> it.contains(string)
            string == "-" -> true //don't filter by starts or ends with
            else -> false//should never reach this
        }}) {
            val syllablesByVowel = unusedWord.count { it in vowels }
            //o.println("syllables of $unusedWord: $syllablesByVowel")
            if(syllablesByVowel in syllableSizes || syllableSizes.isEmpty()) {
                if(lintAWordAgainstTheDictionary(unusedWord, dictionary, false) == 0) {
                    o.println(unusedWord)
                    matchingWords++
                }
            }
        }
        ret.appendln("matching words: $matchingWords")
    }
    return ret.toString()
}



private fun containsForbiddenSyllable(word: String): Boolean {
    for (forbSyl in forbiddenSyllables) {
        if (forbSyl in word) {
            return true
        }
    }
    return false
}

fun lintAWordAgainstTheDictionary(word: String, dict: Array<String>, complain:Boolean=true): Int {
    //todo: words with different harmonising vowels
    var complaints = 0

    val appearances = dict.count { it == word }
    if(appearances > 1) {
        //if the dictionary contains this word more than once
        if(complain) o.println(RED+"dictionary contains word \"$word\" $appearances times"+reset)
        complaints++
    }

    //check for illegal letters
    val invalidLetters = word.filter { it !in vowels+consonants }
    if (invalidLetters.isNotEmpty()) {
        if(complain) o.println(RED+"word \"$word\" contains illegal letters: $invalidLetters"+reset)
        complaints++
    }

    //check for any of the 4 illegal syllables
    for (forb in forbiddenSyllables) {
        if (word.contains(forb)) {
            if(complain) o.println(RED+"word \"$word\" contains illegal syllable \"$forb\""+reset)
            complaints++
        }
    }

    for(otherWord in dict) {
        //check if this word differs from another dictionary word by 1 letter
        /*if(word.length >= 4) {//only check words with >= 4 letters
            var oneLetterDifference = (word.length - otherWord.length == 1
                    && word.contains(otherWord)
                    && word != otherWord)

            || (otherWord.length - word.length == 1 && otherWord.contains(word) && otherWord != word)

            //if our word is the same length as another word,
            //check if it only differs from that word by 1 letter
            if (word.length == otherWord.length) {
                var differences = word.length
                for (i in 0 until word.length) {
                    if (word[i] == otherWord[i]) {
                        differences--
                    }
                }
                oneLetterDifference = oneLetterDifference || differences == 1

            }
            //if the first letters are the differing ones, they're different enough!
            oneLetterDifference = oneLetterDifference && (word[0] == otherWord[0])
            if (oneLetterDifference) {
                if(complain) o.println(YELLOW+"word \"$word\" only differs from " +
                        "other dictionary word \"$otherWord\" by 1 letter"+reset)
                complaints++
            }
        }*/
        //check if this word starts with an existing dictionary word
        if(otherWord.length > 2) {
            if (word.startsWith(otherWord)) {
                if (complain) o.println(YELLOW + "word \"$word\" starts with" +
                        " existing dictionary word \"$otherWord\"" + reset)
                complaints++
            }
        }
        //check if a dictionary word starts with this word
        if(word.length > 2) {
            if (otherWord.startsWith(word)) {
                if(complain) o.println(YELLOW + "existing dictionary word \"$otherWord\" starts" +
                        " with word \"$word\""+ reset)
                complaints++
            }
        }
    }

    //check for similar words
    val similarWords = similarWordsTo(word)
    for (otherWord in dict) {
        for (similarWord in similarWords) {
        //allPossibleWords.remove(similarWord)
            if (otherWord == similarWord) {
                if(complain) o.println("word \"$word\" is very similar to \"$otherWord\"")
                complaints++
                break//only report similarity of this word once
            }
        }
    }

    return complaints
}

internal fun similarWordsTo(word: String): Array<String> {
    val letterGroups = setOf(stops, approximants, fricatives, nasals, vowels)
    if (word.count { it in vowels } == 1) {
        return arrayOf()
    }
    val similarWords = LinkedList<String>()
    val vowelsInWord = word.count { it in vowels }
    if(vowelsInWord > 1) {//if the word contains >1 vowel
        for(i in 0 until vowels.length) {
            //and they are all the same vowel
            if((word.length - word.replace(vowels[i].toString(), "").length) == vowelsInWord ) {
                //add the words where we replace that vowel with the other two vowels
                //eg, kipisi => kupusu, kapasa
                similarWords.add(word.replace(vowels[i], vowels[(i+1) % vowels.length]))
                similarWords.add(word.replace(vowels[i], vowels[(i+2) % vowels.length]))
            }
        }
    }
    for (i in 0 until word.length) {
        //instead of words that just differ by one letter,
        //that letter has to be the same type to count as similar (vowel, stop, fricative etc)
        for(group in letterGroups) {
            if(word[i] in group) {
                for(letter in (group.toSet()-word[i])) {
                    val potentialSimilarWord = replaceCharAt(word, i, letter)
                    if(validWord.matches(potentialSimilarWord)) { //if the word is valid
                        similarWords.add(potentialSimilarWord)
                    }
                }
            }
        }
        //replace u with the other vowels, and the other vowels for u
        if (word[i] == 'a') {//replace a with u
            similarWords.add(replaceCharAt(word, i, 'u'))
        }

        if (word[i] == 'u') {//replace u with a and i
            similarWords.add(replaceCharAt(word, i, 'a'))
            similarWords.add(replaceCharAt(word, i, 'i'))
        }

        if (word[i] == 'i') {//replace i with u
            similarWords.add(replaceCharAt(word, i, 'u'))
        }

        if (word[i] == 'n') {
            if (i == word.length - 1 || i == 0 ||
                (vowels.contains(word[i-1]) && consonants.contains(word[i+1]))) {
                //remove word-final n,
                //word-initial n,
                //and syllable-final n (Ns which occur after a vowel and before another consonant)
                similarWords.add(word.removeRange(i, i+1))
            }
            if (i == word.length - 2) {//if there's a penultimate n, remove the final vowel
                similarWords.add(word.substring(0, word.length - 1))
            }
        }
    }
    //add phonotactically-valid anagrams beginning with the same letter
    val afterFirst = word.substring(1)

    val anagrams = anagram(word[0].toString(), afterFirst, TreeSet<String>()).toMutableSet()
    anagrams.remove(word)//remove the word itself from the list of anagrams
    similarWords += anagrams

    return similarWords.toTypedArray()
}


private fun replaceCharAt(victim: String, index: Int, replacement: Char): String {
    val myName = StringBuilder(victim)
    myName.setCharAt(index, replacement)
    return myName.toString()
}

private fun scrapeWordsFromDictionary(dictFile: File): Array<String> {
    //String wholeDict = fileToString(new File("dictionary.md"));
    val wholeDict = dictFile.readText()
    val byLine = wholeDict.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val words = mutableListOf<String>()
    for (i in 2 until byLine.size) {//start after the table heading
        //o.println("line: "+byLine[i]);
        val pat = Pattern.compile("([$consonants$vowels]+)[ \t]*\\|.*")
        val mat = pat.matcher(byLine[i])
        if(!mat.matches()) {
            //e.println("line ${i+1} is not a word row:\"${byLine[i]}\"")
            continue
        }
        //o.println("group count: "+mat.groupCount());
        //o.println("group :"+mat.group());
        //o.println("matches: "+mat.matches());
        words.add(mat.replaceAll("$1"))
    }
    o.println("dictionary words: ${words.size}")
    return words.toTypedArray()
}

private fun anagram(wordSoFar: String, lettersLeft: String,
                    accum: MutableSet<String> ): Set<String> {
    //o.println("letters left:"+lettersLeft.length)
    //o.println("word so far:"+wordSoFar)
    if (lettersLeft.isEmpty()) {
        //o.println("it's empty")
        //word is complete
        //o.println("anagram:"+wordSoFar)
        accum.add(wordSoFar)
        return accum
    }
    else {
        for(i in 0 until lettersLeft.length) {
            val thisChar = lettersLeft[i]
            if(wordSoFar[wordSoFar.length-1] in vowels && thisChar in consonants) {
                //last letter was a vowel; next letter should be a consonant
                //val minusTheLetter:MutableList<Char> = lettersLeft.toMutableList()
                //minusTheLetter.removeAt(i)
                val minusTheLetter = lettersLeft.removeRange(i, i+1)
                anagram(wordSoFar + thisChar,
                        minusTheLetter, accum)
            }else if(wordSoFar[wordSoFar.length-1] in consonants && thisChar in vowels) {
                //last letter was a consonant; next letter should be a vowel
                //val minusTheLetter:MutableList<Char> = lettersLeft.toMutableList()
                //minusTheLetter.removeAt(i)
                val minusTheLetter = lettersLeft.removeRange(i, i+1)
                anagram(wordSoFar + thisChar,
                        minusTheLetter, accum)
            }
        }
        return accum
    }
}

class SyllableGenerator {

    private val syllables = TreeSet<String>()
    private val wordInitialOnlySyllables = TreeSet<String>()
    private val wordInitialSyllables = TreeSet<String>()

    init{
        //generate all possible syllables

        //firstly, generate initial-only syllables
        for (v in vowels) {
            wordInitialOnlySyllables.add(v.toString())
            wordInitialOnlySyllables.add(v.toString()+"n")
        }

        //then, generate all other possible syllables
        for (cc in consonants) {
            val c = cc.toString()
            for (vv in vowels) {
                val v = vv.toString()
                if ((c + v) in forbiddenSyllables) {
                    //if it's a forbidden syllable, skip adding it to the list
                    continue
                }
                syllables.add(c + v)
                syllables.add(c + v + "n")
            }
        }

        wordInitialSyllables += wordInitialOnlySyllables
        wordInitialSyllables += syllables

        val numSingleSyllableWords = wordInitialSyllables.size
        val dualSyllableWords = numSingleSyllableWords * syllables.size
        val tripleSyllableWords = dualSyllableWords * syllables.size

        e.println("possible single-syllable words: $numSingleSyllableWords")
        e.println("possible double-syllable words: $dualSyllableWords")
        e.println("possible triple-syllable words: $tripleSyllableWords")
    }

    /**list all possible single-syllable words (glue words)*/
    internal fun listSingleSyllableWords(): Set<String> {
        return wordInitialSyllables
    }

    /**list all possible double-syllable words*/
    internal fun listDoubleSyllableWords(): Set<String> {
        val twos = TreeSet<String>()

        //empty string represents missing initial consonant
        for(firstSyllable in wordInitialSyllables) {
            for(secondSyllable in syllables) {
                if(!(firstSyllable.endsWith("n") && secondSyllable.startsWith("n"))) {
                    twos.add(firstSyllable+secondSyllable)
                }
            }
        }
        return twos
    }

    /**list all possible triple-syllable words */
    internal fun listTripleSyllableWords(): Set<String> {
        val tris = TreeSet<String>()

        for(firstSyllable in wordInitialSyllables) {
            for(secondSyllable in syllables) {
                for(thirdSyllable in syllables) {
                    //both of the following have to be true:
                    //1) the end of the 1st syllable and the start of the 2nd can't both be n
                    //1) the end of the 2nd syllable and the start of the 3rd can't both be n
                    if(!(firstSyllable.endsWith("n") && secondSyllable.startsWith("n"))

                    && !(secondSyllable.endsWith("n") && thirdSyllable.startsWith("n")) ) {
                        tris.add(firstSyllable+secondSyllable+thirdSyllable)
                    }
                }
            }
        }
        return tris
    }
}

