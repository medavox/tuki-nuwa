import java.util.*

internal class SyllableGenerator(vowels:String, consonants:String, forbiddenSyllables:Array<String>) {

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

        System.err.println("possible single-syllable words: $numSingleSyllableWords")
        System.err.println("possible double-syllable words: $dualSyllableWords")
        System.err.println("possible triple-syllable words: $tripleSyllableWords")
    }

    /**list all possible single-syllable words*/
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