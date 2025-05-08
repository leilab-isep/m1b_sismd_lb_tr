package ForkJoinPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

public class ParsePage_ForkJoinPool extends RecursiveTask<Map<String, Integer>> {

    private final List<Page_ForkJoinPool> pageList;
    private final int threshold = 500;

    int processedPages;

    public ParsePage_ForkJoinPool(List<Page_ForkJoinPool> pageList) {
        this.pageList = pageList;
    }

    @Override
    protected Map<String, Integer> compute() {
        int pageSize = pageList.size();
        int mid = pageSize / 2;
        if (pageSize >= threshold) {
            List<Page_ForkJoinPool> pageList1 = pageList.subList(0, mid);
            List<Page_ForkJoinPool> pageList2 = pageList.subList(mid, pageSize);

            ParsePage_ForkJoinPool parsePage1 = new ParsePage_ForkJoinPool(pageList1);
            ParsePage_ForkJoinPool parsePage2 = new ParsePage_ForkJoinPool(pageList2);

            parsePage1.fork();
            parsePage2.fork();

            return mergeCounts(parsePage1.join(), parsePage2.join());

        } else {
            Map<String, Integer> localCounts = new HashMap<>();
            for (Page_ForkJoinPool page : pageList) {
                if (page == null) continue;
                Iterable<String> words = new Words_ForkJoinPool(page.getText());
                for (String word : words) {
                    if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                        localCounts.merge(word, 1, Integer::sum);
                    }
                }
            }
            return localCounts;
        }

    }

    private Map<String, Integer> mergeCounts(Map<String, Integer> a, Map<String, Integer> b) {
        for (Map.Entry<String, Integer> entry : b.entrySet()) {
            a.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return a;
    }
}
