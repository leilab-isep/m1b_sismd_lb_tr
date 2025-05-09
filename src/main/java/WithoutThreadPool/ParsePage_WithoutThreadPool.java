package WithoutThreadPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParsePage_WithoutThreadPool implements Runnable {

    private final List<Page_WithoutThreadPool> pageList;
    private final int threshold = 500;
    private final Map<String, Integer> localCounts = new HashMap<>();

    public ParsePage_WithoutThreadPool(List<Page_WithoutThreadPool> pageList) {
        this.pageList = pageList;
    }


    @Override
    public void run() {
        for (Page_WithoutThreadPool page : pageList) {
            if (page == null) continue;
            Iterable<String> words = new Words_WithoutThreadPool(page.getText());
            for (String word : words) {
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    localCounts.merge(word, 1, Integer::sum);
                }
            }
        }
    }


    public Map<String, Integer> getLocalCounts() {
        return localCounts;
    }
}
