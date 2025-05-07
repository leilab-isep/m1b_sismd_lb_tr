package ForkJoinPool;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WordCount_ForkJoinPool {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    private static final HashMap<String, Integer> counts =
            new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {

        ForkJoinPool pool = new ForkJoinPool();

        long start = System.currentTimeMillis();
        Iterable<Page_ForkJoinPool> pages = new Pages_ForkJoinPool(maxPages, fileName);


        List<Page_ForkJoinPool> pageList =
                StreamSupport.stream(pages.spliterator(), false)
                        .collect(Collectors.toList());


        int processedPages = 0;
        ParsePage_ForkJoinPool parsePage = new ParsePage_ForkJoinPool(pageList, counts, processedPages);
        pool.invoke(parsePage);
        processedPages = parsePage.join();


        long end = System.currentTimeMillis();
        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).collect(Collectors.toList()).forEach(x -> System.out.println("Word: \'" + x.getKey() + "\' with total " + x.getValue() + " occurrences!"));
    }

    private static void countWord(String word) {
        Integer currentCount = counts.get(word);
        if (currentCount == null)
            counts.put(word, 1);
        else
            counts.put(word, currentCount + 1);
    }
}
