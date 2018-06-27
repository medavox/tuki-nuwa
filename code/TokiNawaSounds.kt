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
private const val consonants = "hjklmnpstw"
private const val vowels = "aiu"

//ansi colour-terminal escape codes
//https://en.wikipedia.org/wiki/ANSI_escape_code#Escape_sequences

private const val reset = "\u001B[0m"
private const val RED = "\u001B[31m"
private const val YELLOW = "\u001B[33m"

fun main(args: Array<String>) {
    val t = TokiNawaSounds()
    val command: String = args[0].toLowerCase()
    when(command) {
        "syllables", "s" -> { // print all possible syllables
            if("[1-3]".toRegex().matches(args[1])) {
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

        "similar", "si" -> { // print possible words considered similar to the supplied word
            //these are generated from the supplied word
            for(s in similarWordsTo(args[1])) {
                o.println(s)
            }
            return
        }

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
    if(!dict.exists() || !dict.isFile || !dict.canRead() || dict.length() < 5) {
        e.println("invalid file \"$dict\" specified.")
        return
    }

    val dictionary = scrapeWordsFromDictionary(dict)
    if(command == "lint" || command == "l") {
        lintTheDictionary(dictionary)
    }
    else if(command == "lexical-frequency" || command == "f") {
        val firstLetterFreqs: MutableMap<Char, Int> = HashMap()
        val letterFreqs: MutableMap<Char, Int> = HashMap()
        var wordsWithSyllableFinalN = mutableSetOf<String>()
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

        o.println("first-letter frequencies:")
        for((key, value) in firstLetterFreqs.toList().sortedBy {(_, v) -> v}.toMap()) {
            o.println("$key: $value")
        }

        o.println("all-letter frequencies:")
        for((key, value) in letterFreqs.toList().sortedBy {(_, v) -> v}.toMap()) {
            o.println("$key: $value")
        }
    }
    else if(command == "unused" || command == "u") {
        listUnusedWords(t, dictionary, args[2], if(args.size == 4) args[3] else "")
    }

}

private fun listUnusedWords(t:TokiNawaSounds, dictionary:Array<String>, string:String,  query:String) {

    /**list unused potential words which aren't too similar to existing words */
    //populate the list of all potential words,
    // then subtract all the dictionary words (and similar) from it
    var totalSimilarWordsToDictionaryWords = 0
    val allPossibleWords:MutableSet<String> = (
                t.listSingleSyllableWords() +
                t.listDoubleSyllableWords() +
                t.listTripleSyllableWords()
            ).toMutableSet()
    val totalPossibleWords = allPossibleWords.size
    o.println("total words: $totalPossibleWords")
    //String[] wordsFromDictionary = scrapeWordsFromDictionary(dictionaryFile);
    for (word in dictionary) {
        allPossibleWords -= word
        for (similarWord in similarWordsTo(word)) {
            //o.println(similarWord)
            allPossibleWords -= similarWord
            totalSimilarWordsToDictionaryWords++
        }
    }

    o.println("total unused: ${allPossibleWords.size}")
    o.println("total similar words to dictionary words: " +
            "$totalSimilarWordsToDictionaryWords")

    if(query == "") {
        o.println("word containing \"$string\":")
        var matchingWords = 0
        for(unusedWord in allPossibleWords.filter { it.contains(string) }) {
            o.println(unusedWord)
            matchingWords++
        }
        o.println("matching words: $matchingWords")
    }
    else {
        // print 1, 2 or 3 syllable possible words
        //or any combination thereof;
        //or only print words BEGINNING or ending with the given sequence
        //also, validate given string is phonotactically valid

        //to the last argument, add:
        //any (or none) or 1, 2 or 3 to print words with any of those numbers of syllables
        //(both 123 and no numbers print words with either 1, 2 or 3 syllables)
        //either (or neither) of s or e,
        //to print words that start (and/or) end with the provided string
        // adding both s and w prints words that start or end with it,
        // adding neither prints words which contain the string anywhere
        o.println("QUERY MODE")
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
        for(unusedWord in allPossibleWords.filter { when {
            startsWith && endsWith -> it.startsWith(string) || it.endsWith(string)
            startsWith && !endsWith -> it.startsWith(string)
            !startsWith && endsWith -> it.endsWith(string)
            !startsWith && !endsWith -> it.contains(string)
            else -> false//should never reach this
        }}) {
            val syllablesByVowel = unusedWord.count { it in vowels }
            //o.println("syllables of $unusedWord: $syllablesByVowel")
            if(syllablesByVowel in syllableSizes || syllableSizes.isEmpty()) {
                o.println(unusedWord)
                matchingWords++
            }
        }
        o.println("matching words: $matchingWords")

    }
}

private val forbiddenSyllables = arrayOf("ji", "ti", "wu")
private const val allowSyllableFinalN = true

private fun containsForbiddenSyllable(word: String): Boolean {
    for (forbSyl in forbiddenSyllables) {
        if (forbSyl in word) {
            return true
        }
    }
    return false
}

class TokiNawaSounds {

    private val syllables = TreeSet<String>()
    private val wordInitialOnlySyllables = TreeSet<String>()
    private val wordInitialSyllables = TreeSet<String>()

    init{
        //generate all possible syllables

        //firstly, generate initial-only syllables
        for (c in vowels) {
            wordInitialOnlySyllables.add(c.toString())
            wordInitialOnlySyllables.add(c.toString()+"n")
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
                    if((!(firstSyllable.endsWith("n") && secondSyllable.startsWith("n"))
                    || (secondSyllable.endsWith("n") && thirdSyllable.startsWith("n")))) {
                        tris.add(firstSyllable+secondSyllable+thirdSyllable)
                    }
                }
            }
        }
        return tris
    }
}
    internal fun lintTheDictionary(dict: Array<String>) {
        //todo: words with different harmonising vowels
        val dupCheck = TreeSet<String>()
        var complaints = 0
        for (word in dict) {
            //check for illegal letters
            val invalidLetters = word.filter { it !in vowels+consonants }
            if (invalidLetters.isNotEmpty()) {
                o.println(RED+"word \"$word\" contains illegal letters: $invalidLetters"+reset)
                complaints++
            }

            //check for any of the 4 illegal syllables
            for (forb in forbiddenSyllables) {
                if (word.contains(forb)) {
                    o.println(RED+"word \"$word\" contains illegal syllable \"$forb\""+reset)
                    complaints++
                }
            }

            //check for syllable-final Ns
            if (!allowSyllableFinalN) {
                if (word.replace("n", "").length < word.length) {
                    //o.println("i:"+i);
                    for(j in word.indices) {
                        if(word[j] == 'n') {
                            /*if(j == (word.length-1)) {
                                o.println("word \"$word\" contains a word-final N")
                                complaints++
                            }
                            else*/ if (j != (word.length-1)
                                    && consonants.contains(word[j + 1])) {
                                o.println("word \"$word\" contains an N before another consonant")
                                complaints++
                            }
                        }
                    }
                }
            }

            //check if this word contains another dictionary word
            for(otherWord in dict) {
                if(word.contains(otherWord) && otherWord.length > 2 && !word.equals(otherWord)) {
                    o.println("word \"$word\" contains other dictionary word \"$otherWord\"")
                    complaints++
                }
            }

            //check for exact-duplicate words
            if (dupCheck.contains(word)) {
                o.println(RED+"word \"$word\" already exists!"+reset)
                complaints++
            } else {
                dupCheck.add(word)
            }

            //check for similar words
            val similarWords = similarWordsTo(word)
            for (similarWord in similarWords) {
                //allPossibleWords.remove(similarWord)
                for (otherWord in dict) {
                    if (otherWord == similarWord) {
                        o.println("word \"$word\" is very similar to \"$otherWord\"")
                        complaints++
                    }
                }
            }
        }
        o.println("total complaints: $complaints")
    }


    internal fun similarWordsTo(word: String): Array<String> {
        if (word.length == 1) {
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
                if (i != word.length - 1) {//replace non-final n with m
                    similarWords.add(replaceCharAt(word, i, 'm'))
                } else {
                    similarWords.add(word.substring(0, word.length - 1))
                }
                if (i == word.length - 2) {//if there's a penultimate n, remove the final vowel
                    similarWords.add(word.substring(0, word.length - 1))
                }
            }

            if (word[i] == 'm') {//replace m with n
                similarWords.add(replaceCharAt(word, i, 'n'))
            }
            if (word[i] == 't') {//replace t with k
                similarWords.add(replaceCharAt(word, i, 'k'))
                //similarWords.add(replaceCharAt(word, i, 'p'))
            }
            if (word[i] == 'k') {//replace k with t
                val wurd = replaceCharAt(word, i, 't')
                if(!containsForbiddenSyllable(wurd)){
                    similarWords.add(wurd)
                }
                //similarWords.add(replaceCharAt(word, i, 'p'))
            }
            if (word[i] == 'w') {//replace k with t
                similarWords.add(replaceCharAt(word, i, 'l'))
                //similarWords.add(replaceCharAt(word, i, 'p'))
            }
            if (word[i] == 'l') {//replace k with t
                val wurd = replaceCharAt(word, i, 'w')
                if(!containsForbiddenSyllable(wurd)){
                    similarWords.add(wurd)
                }
                //similarWords.add(replaceCharAt(word, i, 'p'))
            }

            //add phonotactically-valid anagrams beginning with the same letter
            val afterFirst = word.substring(1)
            val firstLetter = word[0]

            val anagrams = anagram(firstLetter.toString(), afterFirst, TreeSet<String>()).toMutableSet()
            anagrams.remove(word)//remove the word itself from the list of anagrams
            similarWords += anagrams
        }

        //val ret = arrayOfNulls<String>(similarWords.size)
        return similarWords.toTypedArray()
    }

    private fun replaceCharAt(victim: String, index: Int, replacement: Char): String {
        val myName = StringBuilder(victim)
        myName.setCharAt(index, replacement)
        return myName.toString()
    }

    internal fun scrapeWordsFromDictionary(dictFile: File): Array<String> {
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

    internal fun anagram(wordSoFar: String, lettersLeft: String,
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

