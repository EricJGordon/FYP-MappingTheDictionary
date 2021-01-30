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
    private static Map<String, IWord> map;


    public static void main(String[] args) throws IOException {

        dict = new Dictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"));
        dict.open();

        map = new HashMap<>();

        String gloss;
        LinkedList<IIndexWord> wordList = new LinkedList<>();
        for (POS pos : POS.values()) {
            IIndexWord idxWord = dict.getIndexWord("apple", pos);
            if (idxWord != null ) {
                wordList.add(idxWord);
            }
        }

        //for (int i = 0; i < 10; i++) {

            //System.out.println(idxWords.size() + " ---");
            //for (IIndexWord idxWord : idxWords) {
        int i = 0;
        while (!wordList.isEmpty() /*&& i < 300*/){
            IIndexWord idxWord = wordList.remove();
            IWordID wordID = idxWord
                    .getWordIDs()
                    .get(0);
            IWord word = dict.getWord(wordID);
            //System.out.println("Id = " + wordID);
            System.out.println("-" + word.getLemma());
            gloss = word.getSynset().getGloss();
            //System.out.println("Gloss = " + gloss);
            wordList.addAll(expandDefinitionOfWord(word));
            for (String s : definitionToList(gloss)) {
                map.putIfAbsent(s, word);
            }
            //}
            System.out.println("Iter " + i++ + ": \n\tMap size = " + map.size() + "\n\tList size = " + wordList.size());
        }
        System.out.println("End: " + map.size() + " total senses");
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

    private static List<IIndexWord> expandDefinitionOfWord(IWord word) {

        List<IIndexWord> validUnseenWords = new ArrayList<>();
        int count = 0;
        for (String s : definitionToList(word.getSynset().getGloss())) {
            //for (POS pos : POS.values()) {
                IIndexWord idxWord = dict.getIndexWord(s, POS.ADJECTIVE);
                if (idxWord != null && !map.containsKey(idxWord.getLemma())) {
                    validUnseenWords.add(idxWord);
                    count++;
                }
                //TODO: Don't count multiple occurrences of the same word
                //TODO: Don't count accidental function words, e.g. 'OR' (for Oregon) or 'At' (for Astatine)
                //TODO: Don't count self
            //}
        }
        System.out.println("Returned " + count + " valid unseen senses for words in - "
                + word.getLemma() + ": \"" + word.getSynset().getGloss().split("\"")[0] + "\"");
        return validUnseenWords;
    }
}
