import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static IDictionary dict;

    public static void main(String[] args) throws IOException {

        dict = new Dictionary(new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict"));
        dict.open();

        Map<String, IWord> map = new HashMap<>();

        String current = "apple";
        String gloss;
        for (int i = 0; i < 10; i++) {
            List<IIndexWord> idxWords = expandDefinition(current);
            //System.out.println(idxWords.size() + " ---");
            for (IIndexWord idxWord : idxWords) {

                IWordID wordID = idxWord.getWordIDs().get(0);
                IWord word = dict.getWord(wordID);
                //System.out.println("Id = " + wordID);
                System.out.println("-" + word.getLemma());
                gloss = word.getSynset().getGloss();
                //System.out.println("Gloss = " + gloss);
                for (String s : gloss.split(" ")) {
                    map.putIfAbsent(s, word);
                }
                current = gloss;
            }
            System.out.println("Iter " + i + ": " + map.size() + " total senses");
        }
    }

    private static List<IIndexWord> expandDefinition(String gloss) {

        List<IIndexWord> validWords = new ArrayList<>();
        int count = 0;
        for (String s : gloss.split(" ")) {
            //TODO: Refine punctuation parsing, and remove example sentences
            for (POS pos : POS.values()) {
                IIndexWord idxWord = dict.getIndexWord(s, pos);
                if (idxWord != null && !idxWord.getLemma().equals("a")) {
                    validWords.add(idxWord);
                    count++;
                }
                //TODO: Don't count multiple occurrences of the same word
                //TODO: Don't count accidental function words, e.g. 'OR' (for Oregon) or 'At' for Astatine
                //TODO: Don't count self
            }
        }
        System.out.println("Returned " + count + " valid senses for words in \"" + gloss + "\"");
        return validWords;
    }
}
