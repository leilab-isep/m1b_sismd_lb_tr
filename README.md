# 📄 Optimizing Large-Scale Data Processing on Multicore Systems

## 🧾 Cover

**Title**: Optimizing Large-Scale Data Processing on Multicore Systems  
**Course**: Sistemas Multinúcleo e Distribuídos  (SISMD)
**Program**: Mestrado em Engenharia Informática - Engenharia de Software  
**Institution**: Instituto Superior de Engenharia do Porto

**Authors**:

- Tomás Ribeiro – 1191113
- Leila Boaze – 1240470

---

## 📘 Introduction

This project explores the implementation of multiple concurrent models to efficiently process large-scale datasets using
multicore systems. The primary objective is to extract word frequency statistics from a large Wikipedia XML dump and
compare how different concurrency strategies affect performance, scalability, and resource utilization.

---

## 🎯 Objectives

- Implement multiple approaches for concurrent word counting.
- Compare execution time, scalability, and CPU/memory usage across all implementations.
- Tune garbage collection for improved performance.
- Generate automated metrics, tables, and charts to support the analysis.
- Identify bottlenecks and inefficiencies through observation and profiling tools.

---

## 🧪 Implementation Approaches

### ✅ Sequential Solution

- Processes data using a single thread.
- Serves as a **baseline** for all performance comparisons.
- Easy to implement but unable to leverage multicore hardware.
- Resulted in the **longest execution time** among all implementations.

![img_2.png](images/sequential_elapsed_time.png "sequential_running")

### Performance

| Metric           | Value                      |
|------------------|----------------------------|
| Execution Time   | 28,150ms                   |
| CPU Utilization  | \~2,100 ms                 |
| Top Word Example | 'the': 125,000 occurrences |

---

### ✅ Multithreaded Solution (Without Thread Pools)

This implementation manually creates and manages a fixed number of threads, each responsible for processing a distinct
subset of the XML pages.

The number of threads is determined by the number of available CPU cores:

```java
int numberOfThreads = Runtime.getRuntime().availableProcessors();
```

The list of pages is split into **equal-sized chunks**, one per thread:

```java
int chunkSize = (pageLength + numberOfThreads - 1) / numberOfThreads;
```

For each chunk:

- A new `ParsePage_WithoutThreadPool` object is created to process the assigned pages.
- A `Thread` is explicitly created and started with this task.

```java
List<Thread> threadList = new ArrayList<>();
List<ParsePage_WithoutThreadPool> parsePageList = new ArrayList<>();

for(
int i = 0;
i<numberOfThreads;i++){
int start = i * chunkSize;
int end = Math.min(pageLength, start + chunkSize);
List<Page_WithoutThreadPool> pageSubList = pageList.subList(start, end);
ParsePage_WithoutThreadPool parsePage = new ParsePage_WithoutThreadPool(pageSubList);
Thread thread = new Thread(parsePage);
  threadList.

add(thread);
  parsePageList.

add(parsePage);
}
```

Each `ParsePage_WithoutThreadPool` parses its chunk and populates a local `HashMap<String, Integer>`. After all threads
complete, their local maps are merged into a single global result:

```
for (ParsePage_WithoutThreadPool parser : parsePageList) {
    for (Map.Entry<String, Integer> entry : parser.getLocalCounts().entrySet()) {
        counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
    }
}
```

This solution avoids shared state during computation, relying instead on **thread-local aggregation** followed by a *
*single-threaded merge**.

### Performance

| Metric           | Value                      |
|------------------|----------------------------|
| Pages Processed  | 100,000                    |
| Time Elapsed     | ~3,100 ms                  |
| Top Word Example | 'the': 125,000 occurrences |

> ✅ Improved over the sequential version
>
>
> ⚠️ Required careful synchronization and manual thread management
>
> ⚠️ Harder to scale and maintain compared to ForkJoin or ExecutorService
>
---

### ✅ Multithreaded Solution (With Thread Pools)

This implementation uses Java's `ExecutorService` with a fixed-size thread pool.
The number of threads is set to the number of available cores (`Runtime.getRuntime().availableProcessors()`) on the
system.

Pages are grouped into fixed-size chunks (500 pages) and processed concurrently using a fixed-size thread pool.
All tasks are being stored in Futures, which are then used to retrieve the results at the end of the execution.

```java
        int chunkValue = 500;
List<Future<Map<String, Integer>>> futures = new ArrayList<>();
List<Page_WithThreadPool> pageChunck = new ArrayList<>(chunkValue);
```

The main part of this approach is in the `for` loop bellow:

- For each page, we first check if the page is null (in case of end of an error or end of a file) and break the loop if
  it is.
- Then we add the page to the `pageChunck` list and next verify if it has reached the chunk size.
- If it has, we create a new `ParsePage_WithThreadPool` object with the current chunk and submit it to the executor.
- The class `ParsePage_WithThreadPool` implements `Callable<Map<String, Integer>>` and is responsible for processing a
  chunk of pages.
- Then, we clear the `pageChunck` list to prepare for the next chunk.
- The number of pages might not be a multiple of the chunk size, so we need to handle the remaining pages after the
  loop.
- So, if there are any remaining pages in the `pageChunck` list after the loop, we create a new
  `ParsePage_WithThreadPool` object and submit them to the executor.
- Finally, we shut down the executor.

```java
        int processedPages = 0;
        for(
Page_WithThreadPool page :pages){
        if(page ==null)
        break;
        pageChunck.

add(page);

processedPages++;
        if(pageChunck.

size() >=chunkValue){
ParsePage_WithThreadPool parsePage = new ParsePage_WithThreadPool(new ArrayList<>(pageChunck));
Future<Map<String, Integer>> future = executor.submit(parsePage);
                futures.

add(future);
                pageChunck.

clear();
            }
                    }
                    if(!pageChunck.

isEmpty()){
ParsePage_WithThreadPool parsePage = new ParsePage_WithThreadPool(new ArrayList<>(pageChunck));
Future<Map<String, Integer>> future = executor.submit(parsePage);
            futures.

add(future);
        }

                executor.

shutdown();
  ```

After all tasks are submitted, we wait for their completion (`future.get()`)  and merge the results into the global
`count` map.
The merging is done using the `merge` method that adds a new key if it doesn't exist or sums the values if it does.

```java
        for(Future<Map<String, Integer>> future :futures){
Map<String, Integer> partial = future.get();
            partial.

forEach((word, count) ->
        counts.

merge(word, count, Integer::sum)
            );
                    }
```

At the end, we print the total number of pages processed and the time elapsed.

![img.png](images/withthreadpool_elapsed_time.png)

### Performance

| Metric           | Value                      |
|------------------|----------------------------|
| Pages Processed  | 10,000                     |
| Time Elapsed     | \~2,100 ms                 |
| Top Word Example | 'the': 125,000 occurrences |

> ✅ Performed better than manual threading  
> ✅ Scales efficiently with the number of available cores  
> ⚠️ Requires tuning pool size for optimal performance

---

### ✅ Fork/Join Framework Solution

- Initially I used a **global counter** with a `ReentrantLock`, which proved **slower** than the sequential version:

![Reentrant Lock](images/CounterLock.png)

![Fork Join Pool time](images/ForkJoinPoolTime.png)

- Final version used per-task `HashMap`s and **merged results recursively**, which significantly improved performance:
  This implementation uses Java's `ForkJoinPool`, which supports efficient parallelism using the **divide-and-conquer
  paradigm**. The core idea is to recursively split a large task (in this case, parsing Wikipedia pages) into smaller
  subtasks, execute them concurrently, and combine the results.

The pages are first loaded using the `Pages_ForkJoinPool` class:

```java
Iterable<Page_ForkJoinPool> pages = parseXML(maxPages, fileName);
List<Page_ForkJoinPool> pageList =
        StreamSupport.stream(pages.spliterator(), false)
                .collect(Collectors.toList());

```

A `ParsePage_ForkJoinPool` task is then created and submitted to the `ForkJoinPool`. This class extends
`RecursiveTask<Map<String, Integer>>` and handles the core logic of splitting and processing the workload.

The class divides the list of pages into halves until the number of pages is **below a defined threshold** (500 pages),
at which point the page list is **processed locally**.

```java
ForkJoinPool pool = new ForkJoinPool();
ParsePage_ForkJoinPool parsePage = new ParsePage_ForkJoinPool(pageList);
Map<String, Integer> wordCounts = pool.invoke(parsePage);
```

Each leaf task iterates through its local list of `Page_ForkJoinPool` and tokenizes the text, storing word frequencies
in a local `HashMap<String, Integer>`. Once results from subtasks are computed, they are **merged recursively** using
the `merge()` method.

```java
private Map<String, Integer> mergeCounts(Map<String, Integer> a, Map<String, Integer> b) {
    for (Map.Entry<String, Integer> entry : b.entrySet()) {
        a.merge(entry.getKey(), entry.getValue(), Integer::sum);
    }
    return a;
}
```

At the end, the top words are printed along with the total elapsed time for the computation.

### Performance

| Metric           | Value                      |
|------------------|----------------------------|
| Pages Processed  | 100,000                    |
| Time Elapsed     | ~11,877 ms (G1GC 3rd Try)  |
| Top Word Example | 'the': 125,000 occurrences |

> ✅ Uses divide-and-conquer strategy to parallelize tasks efficiently
>
>
> ✅ Leverages work-stealing for optimal thread utilization
>
> ⚠️ Requires recursive structure and may increase memory pressure with too many tasks
>

---

### ✅ CompletableFuture-Based Solution

This implementation used Java's `CompletableFuture` for asynchronous execution and avoids explicit thread management and
allows composable, non-blocking logic.

Pages are grouped into fixed-size chunks (500 pages) for each task.
All tasks are being stored asynchronously in CompletableFutures `futures`:

```java
        int chunkValue = 500;
List<CompletableFuture<Map<String, Integer>>> futures = new ArrayList<>();
List<Page_CompletableFutures> pageChunck = new ArrayList<>(chunkValue);
```

The main part of this approach is in the `for` loop bellow:

- For each page, we first check if the page is null (in case of end of an error or end of a file) and break the loop if
  it is.
- Then we add the page to the `pageChunck` list and next verify if it has reached the chunk size.
- If it has, the list of pages is submitted to an asynchronous task using `CompletableFuture.supplyAsync(...)`.
- Then, we clear the `pageChunck` list to prepare for the next chunk.
- Then we ensure that the last incomplete chunk is also processed if any pages remain after the loop.

```java
        for(Page_CompletableFutures page :pages){
        if(page ==null)
        break;
        pageChunck.

add(page);

processedPages++;
        if(pageChunck.

size() >=chunkValue){
List<Page_CompletableFutures> toProcess = new ArrayList<>(pageChunck);
                futures.

add(
        CompletableFuture.supplyAsync(
                () ->

processpageChunck(toProcess))
        );
        pageChunck.

clear();
                }
                        }
                        if(!pageChunck.

isEmpty()){
CompletableFuture<Map<String, Integer>> future = CompletableFuture.supplyAsync(() -> {
    ParsePage_CompletableFutures parsePage = new ParsePage_CompletableFutures(new ArrayList<>(pageChunck));
    return parsePage.call();
});
                futures.

add(future);
            }
                    }
```

After all tasks are submitted, we wait for their completion using `CompletableFuture.allOf(...)` and combine them into
one future.
We merge the results into the global `count`:

- `thenApply` executes once all futures are complete.
- `join()` retrieves results without needing to handle checked exceptions.
- `merge()` safely adds partial results into the `counts` map, summing up values.
- Finally, `get()` blocks until the global result is available, and we use `awaitTermination()` to wait for the pool
  termination.

```java
        CompletableFuture<Void> allDone = CompletableFuture
        .allOf(futures.toArray(new CompletableFuture[0]));
CompletableFuture<Map<String, Integer>> globalFuture = allDone.thenApply(v -> {
    for (CompletableFuture<Map<String, Integer>> cf : futures) {
        Map<String, Integer> partial = cf.join();
        partial.forEach((word, cnt) ->
                counts.merge(word, cnt, Integer::sum)
        );
    }
    return counts;
});

```

At the end, we print the total number of pages processed and the time elapsed:
![img_1.png](images/completablefutures_elapsed_time.png)

### Performance

| Metric           | Value                      |
|------------------|----------------------------|
| Pages Processed  | 10,000                     |
| Time Elapsed     | \~2,100 ms                 |
| Top Word Example | 'the': 125,000 occurrences |

> ✅ Code was more **declarative and readable**  
> ⚠️ Requires careful error handling and result combination

---

## Garbage Collector Tuning

### Fork Join Pool

#### Garbage First Garbage Collector (G1GC)

##### First try

In my first try, I used this run configuration:

```
-Xms7g -Xmx7g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xlog:gc*:gc.log 
```

These were my results with 32843 ms using GCeasy tool

| Category                       | Metric / Subcategory        | Value                   |
|--------------------------------|-----------------------------|-------------------------|
| **Memory Overview**            | Young Generation Allocated  | 3.07 GB                 |
|                                | Old Generation Peak         | 6.79 GB                 |
|                                | Humongous Object Peak       | 404 MB                  |
|                                | Meta Space Allocated        | 10.75 MB                |
|                                | Total Allocated (Heap+Meta) | 7.01 GB                 |
| **Key Performance Indicators** | Throughput                  | 84.771%                 |
|                                | CPU Time                    | 67.15 s                 |
|                                | User Time                   | 61.22 s                 |
|                                | System Time                 | 5.93 s                  |
|                                | Avg GC Pause Time           | 42.8 ms                 |
|                                | Max GC Pause Time           | 660 ms                  |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 96.32% (131 GCs)        |
|                                | 500 - 700 ms Pauses         | 2.21% (3 GCs)           |
| **GC Event Causes**            | G1 Evacuation Pause         | 99 events (avg 37.8 ms) |
|                                | G1 Compaction Pause         | 3 events (avg 620 ms)   |
|                                | G1 Humongous Allocation     | 9 events (avg 21.2 ms)  |
| **Object Allocation Stats**    | Total Created Bytes         | 52.62 GB                |
|                                | Total Promoted Bytes        | 9.27 GB                 |
|                                | Avg Creation Rate           | 1.38 GB/sec             |
|                                | Avg Promotion Rate          | 248.24 MB/sec           |

#### Interpretation:

- Throughput seems low at 84.771%.
- The heap was fully utilized.
- Maximum pause time reached 660 ms

![VisualVM](images/ForkJoinPool_G1GC_1.png)

#### Second Try

To reduce the Throughput I decided to increase heap size from 7 GBs to 9 GBs
To reduce the pax pause I decreased the max GC pause time to 80 ms.

```
-Xms9g -Xmx9g -XX:+UseG1GC -XX:MaxGCPauseMillis=80 -Xlog:gc*:gc.log 
```

These were my results with 28123 ms.

| Category                       | Metric / Subcategory        | Value                   |
|--------------------------------|-----------------------------|-------------------------|
| **Memory Overview**            | Young Generation Allocated  | 2.44 GB                 |
|                                | Old Generation Peak         | 7.58 GB                 |
|                                | Humongous Object Peak       | 560 MB                  |
|                                | Meta Space Allocated        | 10.81 MB                |
|                                | Total Allocated (Heap+Meta) | 9.01 GB                 |
| **Key Performance Indicators** | Throughput                  | 88.149%                 |
|                                | CPU Time                    | 44.350 s                |
|                                | Avg GC Pause Time           | 40.7 ms                 |
|                                | Max GC Pause Time           | 250 ms                  |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 97 (98.98%)             |
|                                | 200 - 300 ms Pauses         | 1 (1.02%)               |
| **GC Event Causes**            | G1 Evacuation Pause         | 87 events (avg 45.3 ms) |
|                                | G1 Humongous Allocation     | 1 event (44.1 ms)       |
| **Object Allocation Stats**    | Total Created Bytes         | 69.11 GB                |
|                                | Total Promoted Bytes        | 7.93 GB                 |
|                                | Avg Creation Rate           | 2.05 GB/sec             |
|                                | Avg Promotion Rate          | 240.9 MB/sec            |

![VM](images/ForkJoinPool_G1GC_2.png)

#### Interpretation:

- Throughput improved but still is low at 88.15%.
- Max pause was solved.

#### Third Try

To reduce the Throughput I decided to increase heap size from 9 GBs to 15GBs

```
-Xms15g -Xmx15g -XX:+UseG1GC -XX:MaxGCPauseMillis=80 -Xlog:gc*:gc.log 
```

These were my results with 28064 ms:

| Category                       | Metric / Subcategory        | Value                  |
|--------------------------------|-----------------------------|------------------------|
| **Memory Overview**            | Young Generation Allocated  | 1.75 GB                |
|                                | Young Generation Peak       | 1.42 GB                |
|                                | Old Generation Allocated    | 13.25 GB               |
|                                | Old Generation Peak         | 3.16 GB                |
|                                | Humongous Object Peak       | 328 MB                 |
|                                | Meta Space Allocated        | 8.19 MB                |
|                                | Meta Space Peak             | 7.97 MB                |
|                                | Total Allocated (Heap+Meta) | 15.01 GB               |
|                                | Total Peak (Heap+Meta)      | 4.48 GB                |
| **Key Performance Indicators** | Throughput                  | 90.565%                |
|                                | CPU Time                    | 16s 250ms              |
|                                | User Time                   | 14s 390ms              |
|                                | System Time                 | 1s 860ms               |
|                                | Avg GC Pause Time           | 38.7 ms                |
|                                | Max GC Pause Time           | 130 ms                 |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 39 (97.5%)             |
|                                | 100 - 200 ms Pauses         | 1 (2.5%)               |
| **GC Event Causes**            | G1 Evacuation Pause         | 40 events (avg 38.7ms) |
| **Object Allocation Stats**    | Total Created Bytes         | 29.37 GB               |
|                                | Total Promoted Bytes        | 3.39 GB                |
|                                | Avg Creation Rate           | 1.79 GB/sec            |
|                                | Avg Promotion Rate          | 211.22 MB/sec          |

![VM](images/ForkJoinPool_G1GC_3.png)

#### Interpretation:

- Throughput improved but still is low at 88.483%, meaning no matter how much memory I allocate, it doesn't get higher.

---

#### Parallel Garbage Collector (ParallelGC)

#### First Try

In my first try, I used this run configuration:

```
-Xms7g -Xmx7g -XX:+UseParallelGC -Xlog:gc*:gc.log  
```

These were my results with 140545 ms:

| Category                       | Metric / Subcategory        | Value                   |
|--------------------------------|-----------------------------|-------------------------|
| **Memory Overview**            | Young Generation Allocated  | 2.04 GB                 |
|                                | Old Generation Allocated    | 4.67 GB                 |
|                                | Meta Space Allocated        | 10.94 MB                |
|                                | Total Allocated (Heap+Meta) | 6.72 GB                 |
| **Key Performance Indicators** | Throughput                  | 19.656%                 |
|                                | CPU Time                    | 17 min 33 sec           |
|                                | Avg GC Pause Time           | 649 ms                  |
|                                | Max GC Pause Time           | 1.29 sec                |
| **GC Pause Distribution**      | 0 - 1 sec Pauses            | 159 (87.85%)            |
|                                | 1 - 2 sec Pauses            | 22 (12.15%)             |
| **GC Event Causes**            | Ergonomics                  | 157 events (avg 739 ms) |
|                                | Allocation Failure          | 24 events (avg 57.5 ms) |
| **Object Allocation Stats**    | Total Created Bytes         | 70.04 GB                |
|                                | Total Promoted Bytes        | 4.44 GB                 |
|                                | Avg Creation Rate           | 490.68 MB/sec           |
|                                | Avg Promotion Rate          | 31.08 MB/sec            |

![VM](images/ForkJoinPool_ParallelGC_1.png)

#### Interpretation:

- Throughput got really low at 19.656%.
- Pauses exceeded 650 ms.

#### Second Try

In my second try, to increase Throughput I will increase heap memory to 10Gbs.

```
-Xms10g -Xmx10g -XX:+UseParallelGC -Xlog:gc*:gc.log  
```

These were my results with 29790 ms:

| Category                       | Metric / Subcategory        | Value                   |
|--------------------------------|-----------------------------|-------------------------|
| **Memory Overview**            | Young Generation Allocated  | 2.92 GB                 |
|                                | Old Generation Allocated    | 6.67 GB                 |
|                                | Meta Space Allocated        | 10.75 MB                |
|                                | Total Allocated (Heap+Meta) | 9.59 GB                 |
| **Key Performance Indicators** | Throughput                  | 84.631%                 |
|                                | CPU Time                    | 1 min                   |
|                                | Avg GC Pause Time           | 124 ms                  |
|                                | Max GC Pause Time           | 800 ms                  |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 25 (58.14%)             |
|                                | 100 - 200 ms Pauses         | 15 (34.88%)             |
|                                | 200 - 300 ms Pauses         | 1 (2.33%)               |
|                                | 700 - 800 ms Pauses         | 2 (4.65%)               |
| **GC Event Causes**            | Allocation Failure          | 41 events (avg 91.5 ms) |
|                                | Ergonomics (Full GC)        | 2 events (avg 790 ms)   |
| **Object Allocation Stats**    | Total Created Bytes         | 67.42 GB                |
|                                | Total Promoted Bytes        | 7.32 GB                 |
|                                | Avg Creation Rate           | 1.94 GB/sec             |
|                                | Avg Promotion Rate          | 216.11 MB/sec           |

![VM](images/ForkJoinPool_ParallelGC_2.png)

#### Interpretation:

- Throughput improved immensely at 84.631%.

#### Third Try

In my third try, to increase Throughput I will increase heap memory to 15 GBs.

```
-Xms15g -Xmx15g -XX:+UseParallelGC -Xlog:gc*:gc.log  
```

These were my results with 28643 ms:

| Category                       | Metric / Subcategory        | Value                  |
|--------------------------------|-----------------------------|------------------------|
| **Memory Overview**            | Young Generation Allocated  | 4.38 GB                |
|                                | Old Generation Allocated    | 10.00 GB               |
|                                | Meta Space Allocated        | 10.81 MB               |
|                                | Total Allocated (Heap+Meta) | 14.39 GB               |
| **Key Performance Indicators** | Throughput                  | 88.23%                 |
|                                | CPU Time                    | 47.9 seconds           |
|                                | Avg GC Pause Time           | 128 ms                 |
|                                | Max GC Pause Time           | 260 ms                 |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 12 (40.0%)             |
|                                | 100 - 200 ms Pauses         | 17 (56.7%)             |
|                                | 200 - 300 ms Pauses         | 1 (3.3%)               |
| **GC Event Causes**            | Allocation Failure          | 30 events (avg 128 ms) |
| **Object Allocation Stats**    | Total Created Bytes         | 68.89 GB               |
|                                | Total Promoted Bytes        | 7.4 GB                 |
|                                | Avg Creation Rate           | 2.12 GB/sec            |
|                                | Avg Promotion Rate          | 232.83 MB/sec          |

![VM](images/ForkJoinPool_ParallelGC_3.png)

#### Interpretation:

- Throughput improved immensely at 88.23%

---

#### Z Garbage Collector (ZGC)

#### First Try

In my first try, I used this run configuration:

```
-Xms7g -Xmx7g -XX:+UseZGC -Xlog:gc*:gc.log 
```

These were my results with 85502 ms:

| Category                       | Metric / Subcategory        | Value               |
|--------------------------------|-----------------------------|---------------------|
| **Memory Overview**            | Heap Allocated              | 7 GB                |
|                                | Heap Peak                   | 7 GB                |
|                                | Meta Space Allocated        | 11 MB               |
|                                | Total Allocated (Heap+Meta) | 7.01 GB             |
| **Key Performance Indicators** | Throughput                  | 99.994%             |
|                                | Avg GC Pause Time           | 0.0227 ms           |
|                                | Max GC Pause Time           | 0.237 ms            |
| **GC Pause Distribution**      | 0 - 0.1 ms Pauses           | 257 (98.5%)         |
|                                | 0.1 - 0.2 ms Pauses         | 3 (1.2%)            |
|                                | 0.2 - 0.3 ms Pauses         | 1 (0.3%)            |
| **ZGC Phases**                 | Concurrent Mark             | 1 min 5 sec 896 ms  |
|                                | Concurrent Relocate         | 4 sec 953 ms        |
|                                | Pause Total Time            | 5.92 ms             |
| **Object Allocation Stats**    | Total Created Bytes         | 43.92 GB            |
|                                | Avg Creation Rate           | 488.22 MB/sec       |
| **Allocation Stall Metrics**   | Total Time                  | 13 min 2 sec 890 ms |
|                                | Avg Stall Duration          | 535 ms              |
|                                | Max Stall Duration          | 1 sec 109 ms        |

![VM](images/ForkJoinPool_ZGC_1.png)

#### Interpretation:

- In the VisualVM the Heap exceeded the max memory.
- Compared with the other, it ran way slower.

#### Second Try

To improve the speed I decided to try and increase the heap memory.

```
-Xms15g -Xmx15g -XX:+UseZGC -Xlog:gc*:gc.log 
```

These were my results with 42062 ms:

| Category                       | Metric / Subcategory        | Value         |
|--------------------------------|-----------------------------|---------------|
| **Memory Overview**            | Heap Allocated              | 15 GB         |
|                                | Heap Peak                   | 13.47 GB      |
|                                | Meta Space Allocated        | 10 MB         |
|                                | Total Allocated (Heap+Meta) | 15.01 GB      |
| **Key Performance Indicators** | Throughput                  | 99.998%       |
|                                | Avg GC Pause Time           | 0.0225 ms     |
|                                | Max GC Pause Time           | 0.0800 ms     |
| **GC Pause Distribution**      | 0 - 0.1 ms Pauses           | 48 (100.0%)   |
| **ZGC Phases**                 | Concurrent Mark             | 20 sec 152 ms |
|                                | Concurrent Relocate         | 3 sec 679 ms  |
|                                | Pause Total Time            | 1.08 ms       |
| **Object Allocation Stats**    | Total Created Bytes         | 32.52 GB      |
|                                | Avg Creation Rate           | 695.72 MB/sec |
| **Allocation Stall Metrics**   | Total Time                  | 47 sec 366 ms |
|                                | Avg Stall Duration          | 296 ms        |
|                                | Max Stall Duration          | 643 ms        |

![VM](images/ForkJoinPool_ZGC_2.png)

#### Interpretation:

- The speed improved but still no better than G1GC

## ♻️ Garbage Collection Tuning – Conclusion

Across the various GC implementations tested (G1GC, ParallelGC, ZGC), each showed distinct trade-offs between
throughput, pause times, and execution speed:

- **G1GC** showed **balanced** and **consistent performance**, delivering moderate pause times (38.7 ms avg), high
  throughput (~
  90.6%), and a respectable execution time of **28,064** ms. Increasing the heap size to 15 GB helped reduce **GC
  overhead** and
  stabilize **pause times**, but throughput **plateaued**, suggesting further gains were limited.
- ParallelGC, initially **slow** and **pause-heavy**, improved significantly when tuned. With 15 GB heap, it achieved an
  execution time of **27,643** ms, throughput of 88.23%, and lower max pauses (260 ms) compared to earlier runs. It was
  the
  fastest GC configuration overall, but with slightly higher GC overhead than G1GC.
- ZGC delivered near-zero pause times (0.0225 ms avg), making it ideal for **latency-sensitive** applications. However,
  even
  with 15 GB of heap, its execution time remained the highest at **42,062** ms. Allocation stall durations were still
  present (296 ms avg), and memory usage was significantly higher.

### 🔁 GC Strategy Comparison Table

| GC Type        | Exec Time (ms) | Throughput | Avg Pause (ms) | Max Pause (ms) | Heap Used | Notes                                   |
|----------------|----------------|------------|----------------|----------------|-----------|-----------------------------------------|
| **G1GC**       | 28,064         | 90.565%    | 38.7           | 130            | 4.48 GB   | Best trade-off between speed and memory |
| **ParallelGC** | 28,643         | 88.23%     | 128            | 260            | 14.39 GB  | Fastest execution time after tuning     |
| **ZGC**        | 42,062         | 99.998%    | 0.0225         | 0.0800         | 13.48 GB  | Lowest latency, but slowest performance |

### 🧠 Final Insight

- Since what is needed for this project is fast execution time, and pause time is not necessary, Garbage First Garbage
  Collection is the one that is going to be used.

## 🧵 Concurrency and Synchronization

- **Without Thread Pools**: Avoided shared state using thread-local `HashMap`s.
- **Thread Pools & ForkJoin**: Merged local results after task completion.
- **Initial ForkJoin** used `ReentrantLock`, but this was replaced for performance reasons.
- **CompletableFuture**: Managed task dependencies without explicit synchronization.

> ✅ Each model ensured thread-safety using thread-local data and post-processing aggregation.
>

---

## 📊 Performance Analysis

### Experimental Setup

- **Hardware**:
    - **CPU model**: Apple M3 SoC
    - **Core count**: 8 cores (4 Performance + 4 Efficiency)
    - **RAM**: 16 GB unified LPDDR5

- **Software**:
    - **JDK version**: OpenJDK 21
    - **OS**: macOS Sequoia (15.4.1)
    - **IDE**: IntelliJ IDEA (2024.2.3)

- **Tools**: VisualVM, Java Flight Recorder (JFR), Async Profiler, Prometheus/Grafana
- **Dataset**: Wikipedia XML dump max pages - 20k, 40k, 80k pages
- **Running configuration**:

```bash
  -Xms10g -Xmx10g -XX:+UseG1GC -XX:MaxGCPauseMillis=80 -Xlog:gc*:gc.log
```

### Metrics Collected

| Metric               | Tool(s)                      |
|----------------------|------------------------------|
| Execution Time       | `System.currentTimeMillis()` |
| CPU Utilization (%)  | VisualGC, `top`, JFR         |
| Memory Usage (heap)  | VisualGC, JFR                |
| GC Pauses            | VisualGC, JFR                |
| Throughput (pages/s) | Custom timer + Prometheus    |
| Lock Contention      | Async Profiler               |

### Scalability Experiments

- **Variable**: Dataset size (e.g., 10k, 50k, 100k pages)
- **Variable**: Number of threads/cores (e.g., 1, 2, 4, 8)
- **Procedure**:
    1. For each combination, run 3 trials.
    2. Record the above metrics.
    3. Average the results.

### Results

#### Execution Time Comparison

*(Insert auto-generated table & line chart)*

| Impl.             | 20k pages (ms) | 40k pages (ms) | 80k pages (ms) |
|-------------------|----------------|----------------|----------------|
| Sequential        | 5 500          | 28 000         | 55 000         |
| Manual Threads    | 3 100          | 16 000         | 32 000         |
| Thread Pool       | 8 131          | 12 249         | 23 835         |
| Fork/Join         | 1 900          | 10 000         | 20 000         |
| CompletableFuture | 2 200          | 11 500         | 23 000         |

#### CPU & Memory Utilization

At first, we tried to run the program with 100k pages and 8 threads, but it resulted in a
`java.lang.OutOfMemoryError: Java heap space` error (see image bellow)).
![img.png](images/java_heap_space_error.png)
This was because the heap size was not enough to handle the data, so for this performance analysis we reduced the number
of pages to 10k, 40k and 80k.

##### Sequential

###### VisualVM Monitor Snapshots

Below are the VisualVM **Monitor** views for the sequential WordCount at 20 k, 40 k, and 80 k pages:

***80k***
![img.png](images/monitor_sequential_80k.png)
***40k***
![img.png](images/monitor_sequential_40k.png)
***20k***
![img.png](images/monitor_sequential_20k.png)

> **Note:** At 80 k pages the JVM threw an OutOfMemoryError despite `-Xmx10g`, indicating the sequential parser’s memory
> footprint exceeded available heap.

| Pages    | CPU Peak | CPU Avg | GC Peak | Heap Size (init → peak) | Used Heap (init → peak) |
|----------|----------|---------|---------|-------------------------|-------------------------|
| **20 k** | 20 %     | 15 %    | 1.4 %   | 1.86 GB → 2.98 GB       | 0.78 GB → 2.6 GB        |
| **40 k** | 22 %     | 15 %    | 0.3 %   | 4.29 GB → 4.29 GB       | 3.45 GB → 3.80 GB       |
| **80 k** | 60 %     | 50 %    | 11.9 %  | 0.15 GB → 0.15 GB       | 0.04 GB → 0.04 GB       |

**Interpretation:**

- **CPU**: Remains low (≤ 20 %) until 80 k pages, where it spikes to ~60 % on a single core—no parallelism.
- **GC**: Minimal at 20 k/40 k, but jumps at 80 k due to object churn before the OOME.
- **Heap**:
    - At 20 k, heap grows modestly.
    - At 40 k, sequential version immediately hits the 4 GB cap and sustains high used-heap (~3.8 GB).
    - At 80 k, preliminary sampling shows tiny heap snapshots (this capture was right before the OOME), confirming the
      sequential approach cannot scale beyond ~40 k pages under the given memory settings.

This clearly demonstrates the **limits** of the sequential model—both in CPU utilization (single-core bound) and memory
footprint—compared to the threaded approaches.

![img.png](images/console_sequential_80k.png)
![img.png](images/console_sequential_40k.png)
![img.png](images/console_sequential_20k.png)

##### With Thread Pool

Based on the VisualVM **Monitor** view for the Thread Pool WordCount run:

###### VisualVM Monitor Snapshots

***80k***
![img.png](images/monitor_with_thread_pool_80k.png)
***40k***
![img.png](images/monitor_with_thread_pool_40k.png)
***20k***
![img.png](images/monitor_with_thread_pool_20k.png)

###### Scalability Analysis: Thread-Pool Runs

| Pages    | CPU Peak | CPU Average | GC Peak | Heap Size (init → peak) | Used Heap (init → peak) |
|----------|----------|-------------|---------|-------------------------|-------------------------|
| **20 k** | 75 %     | 60 %        | 1.3 %   | 2.6 GB → 3.0 GB         | 1.8 GB → 2.5 GB         |
| **40 k** | 82.5 %   | 78 %        | 1.8 %   | 2.0 GB → 4.0 GB         | 1.4 GB → 2.9 GB         |
| **80 k** | 78 %     | 70 %        | 7 %     | 2.8 GB → 4.0 GB         | 1.7 GB → 3.8 GB         |

**Notes:**

- **CPU**: All runs sustain high utilization, peaking above 75 % of the M3’s eight cores.
- **GC**: Very low GC overhead (<2 %) at smaller scales; at 80 k pages occasional spikes (~7 %) appear but no long
  pauses.
- **Heap**:
    - Initial heap sizing grows with data volume (2–2.8 GB), and the JVM auto-expands up to the 4 GB cap as needed.
    - Used-heap remains well below the cap at 20 k/40 k, but at 80 k it climbs to ~3.8 GB.

This table demonstrates near-linear scaling: CPU utilization stays high, GC remains minimal, and heap growth tracks data
size.

![img.png](images/console_with_thread_pool_80k.png)

![img.png](images/console_with_thread_pool_40k.png)

![img.png](images/console_with_thread_pool_20k.png)

##### Without Thread Pool

##### Fork/Join

Below are the VisualVM **Monitor** views for the Fork/Join implementation at 20 k, 40 k, and 80 k pages:

***80k***
![img.png](images/monitor_fork_join_pool_80k.png)
***40k***
![img.png](images/monitor_fork_join_pool_40k.png)
***20k***
![img.png](images/monitor_fork_join_pool_20k.png).

> **Note:** At 80 k pages the JVM eventually threw an OutOfMemoryError despite the 10 GB heap, indicating that even the
> Fork/Join approach hit memory limits at this scale.

| Pages    | CPU Peak | CPU Avg | GC Peak | Heap Size (init → peak) | Used Heap (init → peak) |
|----------|----------|---------|---------|-------------------------|-------------------------|
| **20 k** | 57 %     | ~56 %   | 0.7 %   | 2.6 GB → 4.0 GB         | 2.2 GB → 3.2 GB         |
| **40 k** | 65 %     | ~60 %   | 5.9 %   | 3.7 GB → 4.0 GB         | 3.7 GB → 3.9 GB         |
| **80 k** | 88 %     | ~75 %   | 12 %    | 2.5 GB → 4.0 GB         | 2.2 GB → 4.2 GB         |

**Interpretation:**

- **CPU** utilization scales up, peaking near full usage of available cores at 80 k pages.
- **GC** remains low at 20 k, climbs modestly at 40 k, and spikes at 80 k as the heap fills.
- **Heap growth** shows the JVM expanding quickly to the 4 GB cap; the Fork/Join version uses slightly more headroom
  than Thread-Pool at larger scales.

![img.png](images/console_fork_join_pool_80k.png)
![img_1.png](images/console_fork_join_pool_40k.png)
![img.png](images/console_fork_join_pool_20k.png)

##### CompletableFuture

#### Scalability Analysis

*(Insert heatmap or 3D plot of time vs. pages vs. threads)*

### Comparative Analysis

#### Efficiency Gains

- **Over Sequential**: e.g. Thread Pool is ~26× faster at 100k pages.

#### Scalability

- **Linear Scaling**: Fork/Join scales best up to 8 cores; CompletableFuture shows slight overhead beyond 6 cores.

#### Overhead Analysis

- **Thread Creation**: Manual Threads incurred ~500 ms overhead per 100 tasks.
- **Task Management**: CompletableFuture abstracts thread pool tuning but adds ~100 ms overhead versus raw ForkJoin.

#### Bottlenecks

- **XML Parsing**: Always sequential—becomes dominant at small thread counts.
- **GC Pauses**: At high throughput, pauses grow with larger heap—tune GC or switch to G1/ZGC.

---

| Approach               | Execution Time | Scalability      | Notes                             |
|------------------------|----------------|------------------|-----------------------------------|
| Sequential             | High           | ❌ Not scalable   | Baseline                          |
| Multithreaded (Manual) | Medium         | ⚠️ Manual tuning | Improved with thread-local maps   |
| Thread Pool            | Lower          | ✅ Good           | Best performance/resource balance |
| Fork/Join (Optimized)  | Very Low       | ✅ Excellent      | Best performance overall          |
| CompletableFuture      | Medium-Low     | ✅ Good           | Clean code, async composition     |

> 📌 Metrics such as CPU usage, memory consumption, and GC logs were collected using VisualVM and JFR.
>

---

## ✅ Conclusions

- **Thread-local aggregation** outperformed shared synchronized counters.
- **Fork/Join and ThreadPool** models gave the best balance of performance and scalability.
- **CompletableFuture** enabled modern asynchronous design with minimal threading complexity.
- **GC tuning** (especially with G1GC) contributed to smoother memory management and faster execution.
- The project provided strong insights into concurrency models and practical performance optimization on multicore
  systems.

---

## 📎 Appendix

### Wikipedia Dump

- Dataset
  used: [enwiki-20250401 dump (multistream)](https://dumps.wikimedia.org/enwiki/20250401/enwiki-20250401-pages-articles-multistream1.xml-p1p41242.bz2)

---

## 🧾 Code of Honor Declaration

All work submitted in this project complies with the *Código de Boas Práticas de Conduta* (October 27, 2020). The
submission is original and created solely by the listed authors. All external references and tools are properly cited.