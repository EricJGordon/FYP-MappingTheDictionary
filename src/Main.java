import edu.mit.jwi.IDictionary;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static IRAMDictionary dict;
    private static Set<IIndexWord> expandedAndPendingWords;
    private static boolean showPrintStatements;
    private static List<String> stopwords;
    private static Scanner in;
    private static WordnetStemmer stemmer;

    public static void main(String[] args) throws Exception {

        dict = new RAMDictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"), ILoadPolicy.NO_LOAD);
        dict.open();
        dict.load(true);
        stopwords = Files.readAllLines(Paths.get("stopwords.txt"));
        stemmer = new WordnetStemmer(dict);
        in = new Scanner(System.in);
        while(true){
            selectFunction();
        }
    }

    private static void selectFunction() throws IOException {
        System.out.println("Choose: \n" +
                "1 - Recursive expansion\n" +
                "2 - Finding n-cycles\n" +
                "3 - Getting total usage frequency\n" +
                "4 - Checking the status of a given word (in WordNet)\n" +
                "5 - Testing definitionToList() on a given word\n" +   // mostly just for easier debugging
                "6 - Finding definitions that a given word is used in\n" +
                "7 - Finding punctuation edge cases\n");
        String answer = in.nextLine();
        switch (answer) {
            case "1":
                recursivelyExpandDefinitions();
                break;
            case "2":
                findCycles();
                break;
            case "3":
                findUsageFrequencyInDefinitions();
                break;
            case "4":
                System.out.println("Word?");
                System.out.println(statusInWordNet(in.nextLine(), true));
                break;
            case "5":
                System.out.println("Word?");
                String w = in.nextLine();
                for (POS p : POS.values()) {
                    IIndexWord idxWord = dict.getIndexWord(w, p);
                    if (idxWord != null ) {
                        for (IWordID wordID: idxWord.getWordIDs()){
                            String def = dict.getWord(wordID).getSynset().getGloss();
                            System.out.println(def);
                            System.out.println(definitionToList(def, false));
                        }
                    }
                }
                break;
            case "6":
                System.out.println("Word?");
                String targetWord = in.nextLine();
                List<IWord> wordList = dictAsList(dict);
                Set<ISynset> uniqueSynsets = new HashSet<>();
                wordList.forEach((word) -> uniqueSynsets.add(word.getSynset()));
                for (ISynset synset: uniqueSynsets){
                    if (synset.getGloss().contains(targetWord)){
                        System.out.println(synset.getWords());
                        System.out.println(synset.getGloss());
                    }
                }
                break;
            case "7":
                System.out.println("Edge cases");
                int val=0, inval=0, other=0;
                for (IWord word: dictAsList(dict)){
                    List<String> list = definitionToList(word.getSynset().getGloss(), false);
                    for (String s: list) {
                        if (!Character.isLetter(s.charAt(0)) && s.charAt(0)!='`' && s.charAt(0)!='-') {
                            System.out.println(s.charAt(0) + " - " + s);
                            //System.out.println(statusInWordNet(s, true));
                            if (statusInWordNet(s, false).equals("valid")){
                                val++;
                            } else if (statusInWordNet(s, false).equals("invalid")) {
                                inval++;
                            } else {
                                other++;
                            }
                        } else{
                            for (char ch: s.toCharArray()){
                                if (!Character.isLetter(ch) && ch!='-'){
                                    //System.out.print(" --- " + ch + " : " + s + " : ");
                                    //System.out.println(statusInWordNet(s, true));

                                }
                            }
                        }
                    } // Conclusions: Disallow any 'word' not starting with a letter,
                    // allow words to contain dashes and numbers, but not forward slashes, pluses, and equals signs
                    // For those instead replace them with a space first. Similarly replace back tick with apostrophe?
                }
                System.out.println(val + ", " + inval + ", " + other);
                break;

        }
        System.out.println();
    }

    private static void findUsageFrequencyInDefinitions() throws IOException {
        Map<String, Integer> usageCount = new HashMap<>();
        List<IWord> wordList = dictAsList(dict);
        for (IWord w: wordList) {
            usageCount.put(w.getLemma(), 0);
        }
        Set<ISynset> uniqueSynsets = new HashSet<>();
        wordList.forEach((word) -> uniqueSynsets.add(word.getSynset()));
        for (ISynset synset: uniqueSynsets){           // so as not to repeat the exact same definition multiple times for each member of the synset
            List<String> definition = definitionToList(synset.getGloss(), false);
            for (String s: definition) {
                int count = usageCount.getOrDefault(s, 0);
                usageCount.put(s, count + 1);
            }
        }
        FileWriter fr = new FileWriter(new File("usageFrequencyInDefinitions.csv"), false);
        System.out.println("\nResults:");
        fr.write("word, frequency, status, excluded\n");
        for (Map.Entry<String, Integer> entry : usageCount.entrySet()){
            String w = entry.getKey();
            boolean excluded = w.contains("_");
            String line = w + ", " + entry.getValue() + ", " + statusInWordNet(w, false) + ", " + excluded;
            System.out.println(line);
            fr.write(line + "\n");
        }
        fr.close();
    }

    private static String statusInWordNet(String lemma, boolean printStems){
        String[] statuses = {"invalid", "valid (but stopword and stemmed)", "valid (but stopword)", "valid (but stemmed)", "valid"};
        int rank = 0; // When a lemma can lead to multiple statuses/ranks, the "highest" possible one is the one returned
        for (POS pos : POS.values()) {
            for (String stem: stemmer.findStems(lemma, pos)){
                IIndexWord idxWord = dict.getIndexWord(stem, pos);
                if (idxWord != null) {
                    if (printStems) {
                        System.out.println(idxWord.getLemma());
                    }
                    boolean stemmed = !stem.equals(lemma.toLowerCase());
                    boolean stopword = stopwords.contains(lemma.toLowerCase());
                    if (!stemmed && !stopword && rank < 4) {
                        rank = 4;
                    }
                    if (stemmed && !stopword && rank < 3) {
                        rank = 3;
                    }
                    if (stopword && !stemmed  && rank < 2) {
                        rank = 2;
                    }
                    if (stopword && stemmed && rank < 1) {
                        rank = 1;
                    }
                }
            }
        }
        return statuses[rank];
    }

    private static void findCycles() {
        List<IWord> completeList = new ArrayList<>(dictAsList(dict));
        Collections.shuffle(completeList);
        List<IIndexWord> firstDef;
        in = new Scanner(System.in);
        System.out.println("Manually input words? (y/n)");
        String answer = in.nextLine();
        boolean manuallyInputWords = answer.equals("y") || answer.equals("Y");
        /*boolean promptAfterEach = false;
        if (!manuallyInputWords) {
            System.out.println("Prompt after each found cycle? (y/n)");
            answer = in.nextLine();
            promptAfterEach = answer.equals("y") || answer.equals("Y");
        }*/
        System.out.println("Max length of cycle to consider?");
        int maxCycleLength = in.nextInt();
        in.nextLine();
        System.out.println("(Ready)\n--------------------");
        for (IWord iWord : completeList) {  // using this instead of a set of unique lemmas so that lemmas with more senses
                                            // have a likelihood of being chosen proportional to their amount of senses
            String w;
            if (manuallyInputWords) {
                System.out.println("Next Word?");
                w = in.nextLine();
            } else {
                w = iWord.getLemma();
            }
            List<IIndexWord> wordList = new ArrayList<>();
            for (POS p : POS.values()) {
                IIndexWord idxWord = dict.getIndexWord(w, p);
                if (idxWord != null ) {
                    wordList.add(idxWord);
                }
            }
            expandedAndPendingWords = new HashSet<>();
            Set<String> cyclesFoundForCurrentLemma = new HashSet<>();
            List<Integer> cycleCounts = new ArrayList<>();
            for (int i = 0; i < maxCycleLength - 1; i++) {
                cycleCounts.add(0);
            }
            for (IIndexWord indexWord : wordList) {
                for (IWordID wordID : indexWord.getWordIDs()) {
                    firstDef = expandDefinitionOfWord(dict.getWord(wordID), "nvar", true);
                    recursivelyFindCycles(firstDef, List.of(w), List.of(wordID.getPOS().toString()), cyclesFoundForCurrentLemma, cycleCounts,  maxCycleLength - 2);
                }
            }
            System.out.println(w + " - " + cycleCounts.toString());
        }
    }
    private static void recursivelyFindCycles(List<IIndexWord> currentLevelDefs, List<String> comboSoFar, List<String> posSoFar, Set<String> cyclesFoundForCurrentLemma, List<Integer> cycleCounts, int levelsRemaining){
        int cycleLength = comboSoFar.size() + 1;
        for (IIndexWord indexWord : currentLevelDefs) {
            for (IWordID wordID : indexWord.getWordIDs()) {
                List<IIndexWord> nextLevelDefs = expandDefinitionOfWord(dict.getWord(wordID), "nvar", true);
                List<String> nextLevelDefsStrings = new ArrayList<>();
                nextLevelDefs.forEach((word) -> nextLevelDefsStrings.add(word.getLemma()));
                List<String> updatedComboSoFar = new ArrayList<>(comboSoFar);
                updatedComboSoFar.add(wordID.getLemma().toLowerCase());
                List<String> updatedPosSoFar = new ArrayList<>(posSoFar);
                updatedPosSoFar.add(wordID.getPOS().toString());
                Set<String> currentComboSet = new HashSet<>(updatedComboSoFar);
                String currentComboString = String.join(" -> ", updatedComboSoFar) + " ----> " + comboSoFar.get(0);
                if (nextLevelDefsStrings.contains(comboSoFar.get(0)) && !cyclesFoundForCurrentLemma.contains(currentComboString) && currentComboSet.size() == cycleLength) {
                    for (int i = 0; i < cycleLength; i++){
                        System.out.print("* ");
                    }
                    for (int i = 0; i < cycleLength - 1; i++){
                        System.out.print(updatedComboSoFar.get(i) + " (" + updatedPosSoFar.get(i) + ") -> ");
                    }
                    int last = updatedComboSoFar.size() - 1;
                    System.out.println(updatedComboSoFar.get(last) + " (" + updatedPosSoFar.get(last) + ") ----> " + updatedComboSoFar.get(0));
                    cycleCounts.set(cycleLength - 2 , cycleCounts.get(cycleLength - 2) + 1);
                    cyclesFoundForCurrentLemma.add(currentComboString);
                }
                if (levelsRemaining > 0){
                    recursivelyFindCycles(nextLevelDefs, updatedComboSoFar, updatedPosSoFar, cyclesFoundForCurrentLemma, cycleCounts, levelsRemaining - 1);
                }
            }
        }
    }

    private static void recursivelyExpandDefinitions() throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        List<String> userWords = new ArrayList<>();
        List<IWord> completeList;
        List<IWord> randomList = null;
        String pos = null;
        boolean samePOS;
        int numTimes;
        int maxNumDefinitionsConsidered;

        System.out.println("Use presets? (y/n)");
        String answer = in.nextLine();
        boolean usePresets = answer.equals("y") || answer.equals("Y");

        if (usePresets) {
            System.out.println("Getting random words:\n-------------------");
            completeList = dictAsList(dict);
            randomList = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                IWord word = completeList.get(new Random().nextInt(completeList.size()));
                System.out.println(word.getLemma().replace('_', ' '));
                randomList.add(word);
            }
            samePOS = true;
            pos = "nvar";
            numTimes = randomList.size();
            maxNumDefinitionsConsidered = 1;
            showPrintStatements = false;
        } else {
            System.out.println("Use the same Part of Speech for all words? (y/n)");
            answer = in.nextLine();
            samePOS = answer.equals("y") || answer.equals("Y");
            if (samePOS){
                System.out.println("Parts of Speech to include? ('nvar' for all)");  // n for noun, v for verb,
                pos = in.nextLine();                                          // a for adjective, and r for adverb
            }
            System.out.println("Number of word entries?");
            numTimes = in.nextInt();
            in.nextLine();
            System.out.println("Max number of definitions to consider for each?");
            maxNumDefinitionsConsidered = in.nextInt();
            in.nextLine();
            System.out.println("Show print statements? (y/n)");
            answer = in.nextLine();
            showPrintStatements = answer.equals("y") || answer.equals("Y");
        }

        int count = 0;
        for(int i = 0; i < numTimes; i++){
            Map<String, String> expandedWords = new HashMap<>();
            expandedAndPendingWords = new HashSet<>();
            String startingWord;

            if (usePresets) {
                startingWord = randomList.get(i).getLemma();
                System.out.print(++count + " "); // to help eyeball the progress when dealing with a large amount of words
            } else {
                System.out.println("Starting Word?");
                startingWord = in.nextLine();
            }

            userWords.add(startingWord);
            if (!samePOS){
                System.out.println("Parts of Speech to include? ('nvar' for all)");
                pos = in.nextLine();
            }
            List<IIndexWord[]> wordList = new ArrayList<>();
            for (POS p : POS.values()) {    // starting word deliberately expands all parts of speech to begin with,
                IIndexWord idxWord = dict.getIndexWord(startingWord, p);   // to reduce chance of instantly petering out
                if (idxWord != null ) {
                    wordList.add(new IIndexWord[]{idxWord, null});
                }
            }

            int totalIters = 0;
            while (!wordList.isEmpty()){
                IIndexWord[] pair = wordList.remove(0);
                IIndexWord idxWord = pair[0];
                expandedWords.put(idxWord.getLemma(), pair[1] == null ? null : pair[1].getLemma());
                List<IWordID> wordIDS = idxWord.getWordIDs();
                int max = Math.min(maxNumDefinitionsConsidered, wordIDS.size());
                for (int j = 0 ; j < max; j++ ){
                    IWordID wordID = wordIDS.get(j);
                    IWord word = dict.getWord(wordID);
                    expandDefinitionOfWord(word, pos, false).forEach((w) -> wordList.add(new IIndexWord[]{w, idxWord}) );
                    if (showPrintStatements){
                        System.out.println("-" + word.getLemma());
                        System.out.println("Id = " + wordID);
                        System.out.println('"' + word.getSynset().getGloss() + '"');
                        System.out.println("Iter " + totalIters++ + ": \n\tMap size = " + expandedWords.size() + "\n\tList size = " + wordList.size() + "\n");
                    }
                }
            }
            expandedWords.remove(startingWord.replace(' ', '_'));
            result.add(expandedWords);
        }

        FileWriter fr1 = new FileWriter(new File("results.csv"), true);
        FileWriter fr2 = new FileWriter(new File("random_results.csv"), true);
        System.out.println("\nResults:");
        for (int i = 0; i < numTimes; i++){
            System.out.println(userWords.get(i) + " - Size: " + result.get(i).size() + " using '" + pos + "'");
            String line = userWords.get(i) + ", " + result.get(i).size() + ", " + pos + ", " + maxNumDefinitionsConsidered + "\n";
            fr1.write(line);
            if (usePresets){
                fr2.write(line);
            }
        }
        fr1.close();
        fr2.close();

        for (Map<String, String> r: result){
            checkDefinitionCoverage(r.keySet());
        }
        Set<String> set = Set.copyOf(Files.readAllLines(Paths.get("1000words.txt")));
        System.out.print("For comparison: ");
        checkDefinitionCoverage(set);

        if (result.size() == 2){    //when only two words are examined, directly compare and contrast their results
            Map<String, String> first = result.get(0);
            Map<String, String> second = result.get(1);
            HashSet<String> diff1and2 = new HashSet<>(first.keySet());
            diff1and2.removeAll(new HashSet<>(second.keySet()));
            HashSet<String> diff2and1 = new HashSet<>(second.keySet());
            diff2and1.removeAll(new HashSet<>(first.keySet()));
            System.out.println("First size: " + result.get(0).size());
            System.out.println("Second size: " + result.get(1).size());
            System.out.println("In 1 but not 2 (" + diff1and2.size() + ") : " + diff1and2);
            System.out.println("In 2 but not 1 (" + diff2and1.size() + ") : " + diff2and1);

            System.out.println("Check if word 'w' is in set 'n'?  (\"w,n\")");
            String input = in.nextLine();
            while (!input.equals("")) {
                String[] query = input.split(",");
                Map<String, String> resultWordsMap = result.get(Integer.parseInt(query[1]) - 1);
                boolean presentInSet = resultWordsMap.containsKey(query[0]);
                System.out.println(presentInSet);
                if (presentInSet){
                    String word = resultWordsMap.get(query[0]);
                    System.out.println("Child of \"" + word + "\"");
                    word = resultWordsMap.get(word);
                    while (word != null) {
                        System.out.println("which is child of \"" + word + "\"");
                        word = resultWordsMap.get(word);
                    }
                }
                input = in.nextLine();
            }
        }
    }

    private static void checkDefinitionCoverage(Set<String> set){

        boolean covered;
        int countCovered = 0;
        List<IWord> wordList = dictAsList(dict);
        Set<ISynset> uniqueSynsets = new HashSet<>();
        wordList.forEach((word) -> uniqueSynsets.add(word.getSynset()));
        for (ISynset synset: uniqueSynsets){
            List<String> definition = definitionToList(synset.getGloss(), true);
            covered = true;
            for (String s: definition) {
                if (!containsOrContainsStemmed(set, s) && !stopwords.contains(s) /*|| statusInWordNet(s, false).equals("invalid")*/){
                    covered = false;
                    break;
                }
            }
            if (covered){
                countCovered++;
            }
        }
        System.out.println(countCovered + " out of " + uniqueSynsets.size() + " definitions are covered.");

    }

    private static boolean containsOrContainsStemmed(Set<String> set, String lemma){
        for (POS pos : POS.values()) {
            for (String stem: stemmer.findStems(lemma, pos)){
                IIndexWord idxWord = dict.getIndexWord(stem, pos);
                if (idxWord != null && set.contains(idxWord.getLemma())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> definitionToList(String s, boolean excludeExampleSentences){
        String punctuation = "[!._,'@?;():\"/+= ]";
        // TODO: Rethink how apostrophes are handled?
        if (excludeExampleSentences){
            s = s.split("\"")[0];
        }
        StringTokenizer tokenizer = new StringTokenizer(s, punctuation);
        List<String> result = new ArrayList<>();
        while (tokenizer.hasMoreTokens()){
            String word = tokenizer.nextToken();
            if (Character.isLetter(word.charAt(0))) {
                result.add(word);
            }
        }
        return result;
    }

    private static List<IIndexWord> expandDefinitionOfWord(IWord word, String pos, boolean allowRepeats) {

        List<IIndexWord> validUnseenWords = new ArrayList<>();
        int count = 0;
        for (String s : definitionToList(word.getSynset().getGloss(), true)) {
            for (char c : pos.toCharArray()) {
                for (String stem: stemmer.findStems(s, POS.getPartOfSpeech(c))){
                    IIndexWord idxWord = dict.getIndexWord(stem, POS.getPartOfSpeech(c));
                    if (idxWord != null && !stopwords.contains(idxWord.getLemma()) && (allowRepeats || !expandedAndPendingWords.contains(idxWord))) {
                        validUnseenWords.add(idxWord);
                        expandedAndPendingWords.add(idxWord);
                        count++;
                    }
                }
            }
        }
        if (showPrintStatements){
            System.out.println("Returned " + count + " valid unseen senses for words in - "
                    + word.getLemma() + ": \"" + word.getSynset().getGloss().split("\"")[0] + "\"");
        }
        return validUnseenWords;
    }

    private static List<IWord> dictAsList(IDictionary dict) {
        Set<IWord> uniqueWordSet = new HashSet<>();
        for (POS pos : POS.values())
            for (Iterator<IIndexWord> i = dict.getIndexWordIterator(pos); i.hasNext(); )
                for (IWordID wid : i.next().getWordIDs()) {
                    uniqueWordSet.addAll(dict.getWord(wid).getSynset().getWords());
                }
        return List.copyOf(uniqueWordSet);
    }
}
