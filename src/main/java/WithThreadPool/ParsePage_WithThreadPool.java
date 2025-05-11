package WithThreadPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ParsePage_WithThreadPool implements Callable<Map<String,Integer>> {
    private final List<Page_WithThreadPool> pageList;

    public ParsePage_WithThreadPool(List<Page_WithThreadPool> pageList) {
        this.pageList = pageList;
    }


    @Override
    public Map<String, Integer> call() {
        Map<String,Integer> localCounts = new HashMap<>();
        for (Page_WithThreadPool page : pageList) {
            Iterable<String> words = new Words_WithThreadPool(page.getText());
            for (String word : words) {
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    localCounts.merge(word, 1, Integer::sum);
                }
            }
        }
        return localCounts;
    }
}
