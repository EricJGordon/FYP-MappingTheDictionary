import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static IDictionary dict;
    private static Map<IIndexWord, IWord> expandedAndPendingWords;
    private static boolean showPrintStatements;
    private static List<String> stopwords;
    private static Scanner in;


    public static void main(String[] args) throws Exception {

        dict = new Dictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"));
        dict.open();
        stopwords = Files.readAllLines(Paths.get("stopwords.txt"));
        in = new Scanner(System.in);
        System.out.println("Choose: \n1 - Recursive Expansion\n2 - Finding n-cycles");
        String answer = in.nextLine();
        switch (answer) {
            case "1":
                recursivelyExpandDefinitions();
                break;
            case "2":
                findCycles();
                break;
        }
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
            expandedAndPendingWords = new HashMap<>();
            Set<String> cyclesFoundForCurrentLemma = new HashSet<>();
            List<Integer> cycleCounts = new ArrayList<>();
            for (int i = 0; i < maxCycleLength - 1; i++) {
                cycleCounts.add(0);
            }
            for (IIndexWord indexWord0 : wordList) {  // should rename to indexWord1 at some point, and shift later names accordingly
                for (IWordID wordID : indexWord0.getWordIDs()) {
                    firstDef = expandDefinitionOfWord(dict.getWord(wordID), "nvar", true);
                    recursivelyFindCycles(firstDef, List.of(w), cyclesFoundForCurrentLemma,  2, cycleCounts,  maxCycleLength - 2);
                }
            }
            System.out.println(w + " - " + cycleCounts.toString());
        }
    }
    private static void recursivelyFindCycles(List<IIndexWord> currentLevelDefs, List<String> comboSoFar, Set<String> cyclesFoundForCurrentLemma, int cycleLength, List<Integer> cycleCounts, int levelsRemaining){
        for (IIndexWord indexWord : currentLevelDefs) {
            for (IWordID wordID : indexWord.getWordIDs()) {
                List<IIndexWord> nextLevelDefs = expandDefinitionOfWord(dict.getWord(wordID), "nvar", true);
                List<String> nextLevelDefsStrings = new ArrayList<>();
                nextLevelDefs.forEach((word) -> nextLevelDefsStrings.add(word.getLemma()));
                List<String> updatedComboSoFar = new ArrayList<>(comboSoFar);
                updatedComboSoFar.add(wordID.getLemma());
                Set<String> currentComboSet = new HashSet<>(updatedComboSoFar);
                String currentComboString = String.join(" -> ", updatedComboSoFar) + " ----> " + comboSoFar.get(0);
                if (nextLevelDefsStrings.contains(comboSoFar.get(0)) && !cyclesFoundForCurrentLemma.contains(currentComboString) && currentComboSet.size() == cycleLength) {
                    /*for (int i = 0; i < cycleLength; i++){
                        System.out.print("* ");
                    }
                    for (String s )
                    System.out.println("*** " + w + " (" + wordID0.getPOS() + "), " + wordID.getLemma() + " (" + wordID.getPOS() + "), "
                            + wordID2.getLemma() + " (" + wordID2.getPOS() + ") and " + wordID3.getLemma() + " (" + wordID3.getPOS()+ ") form a " + cycleLength + "-cycle!"); */
                    //TODO: add back detail to printed output
                    System.out.println(currentComboString);
                    cycleCounts.set(cycleLength -2 , cycleCounts.get(cycleLength - 2) + 1);
                    cyclesFoundForCurrentLemma.add(currentComboString);
                }
                if (levelsRemaining > 0){
                    recursivelyFindCycles(nextLevelDefs, updatedComboSoFar, cyclesFoundForCurrentLemma, cycleLength + 1, cycleCounts, levelsRemaining - 1);
                }
            }
        }
    }

    private static void recursivelyExpandDefinitions() throws IOException, InterruptedException {
        ArrayList<Map<IIndexWord, String>> result = new ArrayList<>();
        ArrayList<String> userWords = new ArrayList<>();
        List<IWord> completeList;
        List<IWord> randomList = null;
        String pos = null;
        boolean samePOS;
        int numTimes;
        int maxNumDefinitionsConsidered;

        System.out.println("Use presets?");
        String answer = in.nextLine();
        boolean usePresets = answer.equals("y") || answer.equals("Y");

        if (usePresets) {
            IRAMDictionary ramDict = new RAMDictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"), ILoadPolicy.NO_LOAD);
            ramDict.open();
            ramDict.load(true);

            System.out.println("Getting random words:\n-------------------");
            completeList = dictAsList(ramDict);
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
            Map<IIndexWord, String> expandedWords = new HashMap<>();
            expandedAndPendingWords = new HashMap<>();
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
            LinkedList<IIndexWord> wordList = new LinkedList<>();
            for (POS p : POS.values()) {    // starting word deliberately expands all parts of speech to begin with,
                IIndexWord idxWord = dict.getIndexWord(startingWord, p);   // to reduce chance of instantly petering out
                if (idxWord != null ) {
                    wordList.add(idxWord);
                }
            }

            int totalIters = 0;
            while (!wordList.isEmpty() /*&& totalIters < 20*/ ){
                IIndexWord idxWord = wordList.remove();
                expandedWords.put(idxWord, dict.getWord(idxWord.getWordIDs().get(0)).getSynset().getGloss() + "\n");
                List<IWordID> wordIDS = idxWord.getWordIDs();
                int max = Math.min(maxNumDefinitionsConsidered, wordIDS.size());
                for (int j = 0 ; j < max; j++ ){
                    IWordID wordID = wordIDS.get(j);
                    IWord word = dict.getWord(wordID);
                    wordList.addAll(expandDefinitionOfWord(word, pos, false));
                    if (showPrintStatements){
                        System.out.println("-" + word.getLemma());
                        System.out.println("Id = " + wordID);
                        System.out.println('"' + word.getSynset().getGloss() + '"');
                        System.out.println("Iter " + totalIters++ + ": \n\tMap size = " + expandedWords.size() + "\n\tList size = " + wordList.size() + "\n");
                    }
                }
            }
            result.add(expandedWords);
        }

        FileWriter fr1 = new FileWriter(new File("results.csv"), true);
        FileWriter fr2 = new FileWriter(new File("random_results.csv"), true);
        System.out.println("\nResults:");
        for (int i = 0; i < numTimes; i++){
            System.out.println(userWords.get(i) + " - Size: " + result.get(i).size() + " using '" + pos + "'");
            String line = userWords.get(i) + ", " + result.get(i).size() + ", " + pos +", " + maxNumDefinitionsConsidered + "\n";
            fr1.write(line);
            if (usePresets){
                fr2.write(userWords.get(i) + ", " + result.get(i).size() + ", " + pos +", " + maxNumDefinitionsConsidered + "\n");
            }
        }
        fr1.close();
        fr2.close();

        if (result.size() == 2){    //when only two words are examined, directly compare and contrast their results
            Map<IIndexWord, String> first = result.get(0);
            Map<IIndexWord, String> second = result.get(1);
            HashSet<IIndexWord> diff1and2 = new HashSet<>(first.keySet());
            diff1and2.removeAll(new HashSet<>(second.keySet()));
            HashSet<IIndexWord> diff2and1 = new HashSet<>(second.keySet());
            diff2and1.removeAll(new HashSet<>(first.keySet()));
            System.out.println("First size: " + result.get(0).size());
            System.out.println("Second size: " + result.get(1).size());
            System.out.println("In 1 but not 2 (" + diff1and2.size() + ") : " + diff1and2);
            System.out.println("In 2 but not 1 (" + diff2and1.size() + ") : " + diff2and1);

            while (true) {
                System.out.println("Check if word 'w' is in set 'n'?  (\"w,n\")");
                String[] query = in.nextLine().split(",");
                System.out.println(result.get(Integer.parseInt(query[1]) - 1).containsKey(dict.getIndexWord(query[0], POS.getPartOfSpeech(pos.charAt(0)))));
                //TODO: Have it also work when pos is multiple characters
            }
        }
    }

    private static List<String> definitionToList(String s){
        String punctuation = "[!._,'@?; ]";
        String glossOnly = s.split("\"")[0];
        //excludes example sentences
        StringTokenizer tokenizer = new StringTokenizer(glossOnly, punctuation);
        List<String> result = new ArrayList<>();
        while (tokenizer.hasMoreTokens()){
            result.add(tokenizer.nextToken());
        }
        //System.out.println(result);
        return result;
    }

    private static List<IIndexWord> expandDefinitionOfWord(IWord word, String pos, boolean allowRepeats) {

        List<IIndexWord> validUnseenWords = new ArrayList<>();
        int count = 0;
        for (String s : definitionToList(word.getSynset().getGloss())) {
            for (char c : pos.toCharArray()) {
                IIndexWord idxWord = dict.getIndexWord(s, POS.getPartOfSpeech(c));
                if (idxWord != null && !stopwords.contains(idxWord.getLemma()) && (allowRepeats || !expandedAndPendingWords.containsKey(idxWord))) {
                    validUnseenWords.add(idxWord);
                    expandedAndPendingWords.put(idxWord, word);
                    count++;
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
