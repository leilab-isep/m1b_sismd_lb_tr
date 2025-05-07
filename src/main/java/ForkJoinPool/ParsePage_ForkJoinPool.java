package ForkJoinPool;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class ParsePage_ForkJoinPool extends RecursiveTask<Integer> {

    private final List<Page_ForkJoinPool> pageList;

    private HashMap<String, Integer> counts;

    int processedPages;

    public ParsePage_ForkJoinPool(List<Page_ForkJoinPool> pageList, HashMap<String, Integer> counts, int processedPages) {
        this.pageList = pageList;
        this.counts = counts;
        this.processedPages = processedPages;
    }

    @Override
    protected Integer compute() {
        int pageSize = pageList.size();
        if (pageList.size() > 100) {
            List<Page_ForkJoinPool> pageList1 = pageList.subList(0, pageSize / 2);
            List<Page_ForkJoinPool> pageList2 = pageList.subList(pageSize / 2, pageSize);

            ParsePage_ForkJoinPool parsePage1 = new ParsePage_ForkJoinPool(pageList1, counts, processedPages);
            ParsePage_ForkJoinPool parsePage2 = new ParsePage_ForkJoinPool(pageList2, counts, processedPages);

            parsePage1.fork();
            parsePage2.fork();

            return parsePage1.join() + parsePage2.join();

        } else {
            for (Page_ForkJoinPool page : pageList) {
                if (page == null)
                    break;
                Iterable<String> words = new Words_ForkJoinPool(page.getText());
                for (String word : words)
                    if (word.length() > 1 || word.equals("a") || word.equals("I"))
                        countWord(word);
                ++processedPages;
            }
            return processedPages;
        }
    }

    private void countWord(String word) {
        Integer currentCount = counts.get(word);
        if (currentCount == null)
            counts.put(word, 1);
        else
            counts.put(word, currentCount + 1);
    }
}
