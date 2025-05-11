package CompletableFutures;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WordCount_CompletableFutures {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    private static final HashMap<String, Integer> counts =
            new HashMap<String, Integer>();
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        int numberOfThreads = Runtime.getRuntime().availableProcessors();

        Iterable<Page_CompletableFutures> pages = new Pages_CompletableFutures(maxPages, fileName);

        int chunkValue = 500;
        
        List<CompletableFuture<Map<String,Integer>>> futures = new ArrayList<>();
        List<Page_CompletableFutures> pageChunck = new ArrayList<>(chunkValue);
        int processedPages = 0;


        for (Page_CompletableFutures page : pages) {
            if (page == null) break;
            pageChunck.add(page);
            processedPages++;
            if (pageChunck.size() >= chunkValue) {
                List<Page_CompletableFutures> toProcess = new ArrayList<>(pageChunck);
                futures.add(
                        CompletableFuture.supplyAsync(
                                () -> processpageChunck(toProcess))
                );
                pageChunck.clear();
            }
        }
        if (!pageChunck.isEmpty()) {
            List<Page_CompletableFutures> toProcess = new ArrayList<>(pageChunck);
            futures.add(
                    CompletableFuture.supplyAsync(
                            () -> processpageChunck(toProcess))
            );
        }

        CompletableFuture<Void> allDone = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]));

        CompletableFuture<Map<String,Integer>> globalFuture = allDone.thenApply(v -> {
            for (CompletableFuture<Map<String,Integer>> cf : futures) {
                Map<String,Integer> partial = cf.join();
                partial.forEach((word, cnt) ->
                        counts.merge(word, cnt, Integer::sum)
                );
            }
            return counts;
        });

        Map<String,Integer> counts = globalFuture.get();
        ForkJoinPool.commonPool().awaitTermination(15, TimeUnit.SECONDS);

        long end = System.currentTimeMillis();
        System.out.println("Processed pages: " + processedPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");


        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(4).collect(Collectors.toList()).forEach(x -> System.out.println("Word: \'" + x.getKey() + "\' with total " + x.getValue() + " occurrences!"));

    }

    private static Map<String,Integer> processpageChunck(List<Page_CompletableFutures> pages) {
        Map<String,Integer> counts = new HashMap<>();
        for (Page_CompletableFutures page : pages) {
            for (String word : new Words_CompletableFutures(page.getText())) {
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    counts.merge(word, 1, Integer::sum);
                }
            }
        }
        return counts;
    }
}
