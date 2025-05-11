package ForkJoinPool;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WordCount_ForkJoinPool {
    static final int maxPages = 20000;
    static final String fileName = "enwiki-20250201.xml";


    public static void main(String[] args) throws Exception {

        ForkJoinPool pool = new ForkJoinPool();
        long start = System.currentTimeMillis();

        Iterable<Page_ForkJoinPool> pages = new Pages_ForkJoinPool(maxPages, fileName);


        List<Page_ForkJoinPool> pageList =
                StreamSupport.stream(pages.spliterator(), false)
                        .collect(Collectors.toList());


        int processedPages = 0;
        ParsePage_ForkJoinPool parsePage = new ParsePage_ForkJoinPool(pageList);
        Map<String, Integer> wordCounts = pool.invoke(parsePage);


        long end = System.currentTimeMillis();
        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        wordCounts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).collect(Collectors.toList()).forEach(x -> System.out.println("Word: \'" + x.getKey() + "\' with total " + x.getValue() + " occurrences!"));

    }
}
