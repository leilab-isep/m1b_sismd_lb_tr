package WithThreadPool;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WordCount_WithThreadPool {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    private static final HashMap<String, Integer> counts =
            new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {

        long start = System.currentTimeMillis();
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        Iterable<Page_WithThreadPool> pages = new Pages_WithThreadPool(maxPages, fileName);
        int chunkValue = 500;
        List<Future<Map<String,Integer>>> futures = new ArrayList<>();
        List<Page_WithThreadPool> pageChunck = new ArrayList<>(chunkValue);

        int processedPages = 0;
        for (Page_WithThreadPool page : pages) {
            if (page == null)
                break;
            pageChunck.add(page);
            processedPages++;
            if (pageChunck.size() >= chunkValue) {
                ParsePage_WithThreadPool parsePage = new ParsePage_WithThreadPool(new ArrayList<>(pageChunck));
                Future<Map<String, Integer>> future = executor.submit(parsePage);
                futures.add(future);
                pageChunck.clear();
            }
        }
        if (!pageChunck.isEmpty()) {
            ParsePage_WithThreadPool parsePage = new ParsePage_WithThreadPool(new ArrayList<>(pageChunck));
            Future<Map<String, Integer>> future = executor.submit(parsePage);
            futures.add(future);
        }

        executor.shutdown();

        for (Future<Map<String,Integer>> future : futures) {
            Map<String,Integer> partial = future.get();
            partial.forEach((word, count) ->
                    counts.merge(word, count, Integer::sum)
            );
        }

        long end = System.currentTimeMillis();
        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(4).collect(Collectors.toList()).forEach(x -> System.out.println("Word: \'" + x.getKey() + "\' with total " + x.getValue() + " occurrences!"));
    }
}
