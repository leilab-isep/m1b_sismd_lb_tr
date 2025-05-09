package WithoutThreadPool;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WordCount_WithoutThreadPool {
    static final int maxPages = 100000;
    static final String fileName = "enwiki.xml";

    private static final HashMap<String, Integer> counts =
            new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();
        Iterable<Page_WithoutThreadPool> pages = new Pages_WithoutThreadPool(maxPages, fileName);
        int numberOfThreads = Runtime.getRuntime().availableProcessors();

        List<Page_WithoutThreadPool> pageList =
                StreamSupport.stream(pages.spliterator(), false)
                        .collect(Collectors.toList());

        int pageLength = pageList.size();

        int chunkSize = (pageLength + numberOfThreads - 1) / numberOfThreads;

        List<Thread> threadList = new ArrayList<>();
        List<ParsePage_WithoutThreadPool> parsePageList = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = Math.min(pageLength, start + chunkSize);
            if (start >= end) break;

            List<Page_WithoutThreadPool> pageSubList = pageList.subList(start, end);

            ParsePage_WithoutThreadPool parsePage = new ParsePage_WithoutThreadPool(pageSubList);
            Thread thread = new Thread(parsePage);
            threadList.add(thread);
            parsePageList.add(parsePage);
        }

        for (int i = 0; i < threadList.size(); i++) {
            threadList.get(i).start();
        }

        for (Thread thread : threadList) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Processed pages: " + pageLength);
        System.out.println("Elapsed time: " + (endTime - startTime) + "ms");


        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        for (ParsePage_WithoutThreadPool parser : parsePageList) {
            for (Map.Entry<String, Integer> entry : parser.getLocalCounts().entrySet()) {
                counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
        commonWords.entrySet().stream().limit(3).collect(Collectors.toList()).forEach(x -> System.out.println("Word: \'" + x.getKey() + "\' with total " + x.getValue() + " occurrences!"));
    }

}
