import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {

    private static IDictionary dict;
    private static Map<IIndexWord, IWord> expandedAndPendingWords = new HashMap<>();


    public static void main(String[] args) throws IOException {

        dict = new Dictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"));
        dict.open();

        Map<IIndexWord, String> expandedWords = new HashMap<>();

        Scanner in = new Scanner(System.in);

        System.out.println("Starting Word?");
        String startingWord = in.nextLine();
        System.out.println("Parts of Speech to include? ('nvar' for all)");  // n for noun, v for verb,
        String pos = in.nextLine();                                          // a for adjective, and r for adverb
        LinkedList<IIndexWord> wordList = new LinkedList<>();
        for (POS p : POS.values()) {    // starting word deliberately expands all parts of speech to begin with,
            IIndexWord idxWord = dict.getIndexWord(startingWord, p);   // to reduce chance of instantly petering out
            if (idxWord != null ) {
                wordList.add(idxWord);
            }
        }

        int i = 0;
        while (!wordList.isEmpty() /*&& i < 300*/){
            IIndexWord idxWord = wordList.remove();
            expandedWords.put(idxWord, dict.getWord(idxWord.getWordIDs().get(0)).getSynset().getGloss() + "\n");
            IWordID wordID = idxWord
                    .getWordIDs()
                    .get(0);
            IWord word = dict.getWord(wordID);
            //System.out.println("Id = " + wordID);
            System.out.println("-" + word.getLemma());
            wordList.addAll(expandDefinitionOfWord(word, pos));
            System.out.println("Iter " + i++ + ": \n\tMap size = " + expandedWords.size() + "\n\tList size = " + wordList.size());
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
                if (idxWord != null && !expandedAndPendingWords.containsKey(idxWord)) {
                    validUnseenWords.add(idxWord);
                    expandedAndPendingWords.put(idxWord, word);
                    count++;
                }
                //TODO: Don't count accidental function words, e.g. 'OR' (for Oregon) or 'At' (for Astatine)
            }
        }
        System.out.println("Returned " + count + " valid unseen senses for words in - "
                + word.getLemma() + ": \"" + word.getSynset().getGloss().split("\"")[0] + "\"");
        return validUnseenWords;
    }
}
