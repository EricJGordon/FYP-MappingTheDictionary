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


    public static void main(String[] args) throws Exception {

        dict = new Dictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"));
        dict.open();
        stopwords = Files.readAllLines(Paths.get("stopwords.txt"));

        //recursivelyExpandDefinitions();
        findBidirectionalNeighbourDefinitions();
    }

    private static void findBidirectionalNeighbourDefinitions() {
        List<IWord> completeList = dictAsList(dict);
        List<IWord> randomList = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            IWord word = completeList.get(new Random().nextInt(completeList.size()));
            System.out.println(word.getLemma().replace('_', ' '));
            randomList.add(word);
        }

        List<IIndexWord> firstDef;
        List<IIndexWord> secondDefs;
        Set<String> secondDefsStrings = new HashSet<>();
        System.out.println("--------------------");
        for (IWord w: randomList){
            expandedAndPendingWords = new HashMap<>();
            firstDef = expandDefinitionOfWord(w, "nvar");
            for (IIndexWord indexWord : firstDef) {
                for (IWordID wordID : indexWord.getWordIDs()) {
                    secondDefs = expandDefinitionOfWord(dict.getWord(wordID), "nvar");
                    secondDefsStrings.clear();
                    secondDefs.forEach((word) -> secondDefsStrings.add(word.getLemma()));
                    if (secondDefsStrings.contains(w.getLemma())) {
                        System.out.println(w.getLemma() + " (" + w.getPOS() + ") and " + wordID.getLemma() + " (" + wordID.getPOS() + ") are Neighbours!");
                    }
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

        Scanner in = new Scanner(System.in);
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
                    wordList.addAll(expandDefinitionOfWord(word, pos));
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

    private static List<IIndexWord> expandDefinitionOfWord(IWord word, String pos) {

        List<IIndexWord> validUnseenWords = new ArrayList<>();
        int count = 0;
        for (String s : definitionToList(word.getSynset().getGloss())) {
            for (char c : pos.toCharArray()) {
                IIndexWord idxWord = dict.getIndexWord(s, POS.getPartOfSpeech(c));
                if (idxWord != null && !expandedAndPendingWords.containsKey(idxWord) && !stopwords.contains(idxWord.getLemma())) {
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
