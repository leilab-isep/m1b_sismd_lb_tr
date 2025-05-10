# 📄 Optimizing Large-Scale Data Processing on Multicore Systems

## 🧾 Cover

**Title**: Optimizing Large-Scale Data Processing on Multicore Systems  
**Course**: Sistemas Multinúcleo e Distribuídos  
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

---

### ✅ Multithreaded Solution (Without Thread Pools)

- Workload is manually split among threads.
- Each thread processes a subset of the pages and computes a local word count using a `HashMap`.
- After all threads finish, the results are merged.

> ✅ Improved over the sequential version  
> ⚠️ Required careful synchronization and manual thread management

---

### ✅ Multithreaded Solution (With Thread Pools)

- Utilizes `ExecutorService` to manage threads and optimize reuse.
- Reduces overhead from thread creation and termination.
- Tasks are distributed dynamically, and local word count maps are aggregated at the end.

> ✅ Performed better than manual threading  
> ✅ Scales efficiently with the number of available cores  
> ⚠️ Requires tuning pool size for optimal performance

---

### ✅ Fork/Join Framework Solution

- Initially used a **global counter** with a `ReentrantLock`, which proved **slower** than the sequential version:

![Reentrant Lock](images/CounterLock.png)

![Fork Join Pool time](images/ForkJoinPoolTime.png)

- Final version used per-task `HashMap`s and **merged results recursively**, which significantly improved performance:

> ✅ Final version was among the **fastest**  
> ✅ Demonstrated excellent scalability for divide-and-conquer workloads

---

### ✅ CompletableFuture-Based Solution

- Built using Java's `CompletableFuture` for asynchronous execution.
- Avoids explicit thread management and allows composable, non-blocking logic.

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

These were my results with 13457 ms using GCeasy tool

| Category                       | Metric / Subcategory        | Value                  |
|--------------------------------|-----------------------------|------------------------|
| **Memory Overview**            | Young Generation Allocated  | 2.51 GB                |
|                                | Young Generation Peak       | 1.87 GB                |
|                                | Avg Promotion Rate          | 188 MB/sec             |
|                                | Old Generation Allocated    | 4.49 GB                |
|                                | Old Generation Peak         | 3.07 GB                |
|                                | Humongous Object Peak       | 312 MB                 |
|                                | Meta Space Allocated        | 10.62 MB               |
|                                | Meta Space Peak             | 10.25 MB               |
|                                | Total Allocated (Heap+Meta) | 7.01 GB                |
|                                | Total Peak (Heap+Meta)      | 4.59 GB                |
| **Key Performance Indicators** | Throughput                  | 90.839%                |
|                                | CPU Time                    | 18s 970ms              |
|                                | User Time                   | 15s 310ms              |
|                                | System Time                 | 3s 660ms               |
|                                | Avg GC Pause Time           | 33.0 ms                |
|                                | Max GC Pause Time           | 180 ms                 |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 49 (98.0%)             |
|                                | 100 - 200 ms Pauses         | 1 (2.0%)               |
| **GC Event Causes**            | G1 Evacuation Pause         | 47 events (avg 34.7ms) |
|                                | G1 Humongous Allocation     | 1 event (20.0ms)       |
| **Object Allocation Stats**    | Total Created Bytes         | 29.29 GB               |
|                                | Total Promoted Bytes        | 3.31 GB                |
|                                | Avg Creation Rate           | 1.62 GB/sec            |

#### Interpretation:

- Throughput seems low at 90.839%.
- Max pause at 180s was a bit too much.

![VisualVM](images/ForkJoinPool_G1GC.png)

#### Second Try

To reduce the Throughput I decided to increase heap size from 7 GBs to 9 GBs
To reduce the pax pause I decreased the max GC pause time to 80 ms.

```
-Xms9g -Xmx9g -XX:+UseG1GC -XX:MaxGCPauseMillis=80 -Xlog:gc*:gc.log 
```

These were my results with 12527ms

| Category                       | Metric / Subcategory        | Value                  |
|--------------------------------|-----------------------------|------------------------|
| **Memory Overview**            | Young Generation Allocated  | 1.81 GB                |
|                                | Young Generation Peak       | 1.7 GB                 |
|                                | Old Generation Allocated    | 7.19 GB                |
|                                | Old Generation Peak         | 3.02 GB                |
|                                | Humongous Object Peak       | 208 MB                 |
|                                | Meta Space Allocated        | 10.56 MB               |
|                                | Meta Space Peak             | 10.23 MB               |
|                                | Total Allocated (Heap+Meta) | 9.01 GB                |
|                                | Total Peak (Heap+Meta)      | 4.82 GB                |
| **Key Performance Indicators** | Throughput                  | 90.87%                 |
|                                | CPU Time                    | 15s 530ms              |
|                                | User Time                   | 12s 990ms              |
|                                | System Time                 | 2s 540ms               |
|                                | Avg GC Pause Time           | 32.3 ms                |
|                                | Max GC Pause Time           | 110 ms                 |
| **GC Pause Distribution**      | 0 - 100 ms Pauses           | 47 (97.92%)            |
|                                | 100 - 200 ms Pauses         | 1 (2.08%)              |
| **GC Event Causes**            | G1 Evacuation Pause         | 48 events (avg 32.3ms) |
| **Object Allocation Stats**    | Total Created Bytes         | 29.16 GB               |
|                                | Total Promoted Bytes        | 3.22 GB                |
|                                | Avg Creation Rate           | 1.72 GB/sec            |
|                                | Avg Promotion Rate          | 194.38 MB/sec          |

![VM](images/ForkJoinPool_G1GC_2Try.png)

#### Interpretation:

- Throughput improved but still is low at 90.87%.
- Max pause was solved

#### Third Try

To reduce the Throughput I decided to increase heap size from 9 GBs to 15GBs

```
-Xms15g -Xmx15g -XX:+UseG1GC -XX:MaxGCPauseMillis=80 -Xlog:gc*:gc.log 
```

These were my results with 11877ms:

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

![VM](images/ForkJoinPool_G1GC_3Try.png)

#### Interpretation:

- Throughput lowered but still is low at 90.565%, meaning no matter how much memory I allocate, it doesn't get lower.

---

#### Parallel Garbage Collector (ParallelGC)

I used this run configuration:

```
-Xms7g -Xmx7g -XX:+UseParallelGC
```

With that said, it took `11921 ms` to finish.

![VisualVM](images/ForkJoinPool_ParallelGC.png)

#### Z Garbage Collector (ZGC)

I used this run configuration:

```
-Xms7g -Xmx7g -XX:+UseZGC
```

With that said, it took `18406 ms` to finish.

![VisualVM](images/ForkJoinPool_ZGC.png)

#### Serial Garbage Collector (SGC)

I used this run configuration:

```
-Xms7g -Xmx7g -XX:+UseSerialGC
```

With that said, it took `17410 ms` to finish.

![VisualVM](images/ForkJoinPool_SGC.png)





---

## 🧵 Concurrency and Synchronization

- **Without Thread Pools**: Avoided shared state using thread-local `HashMap`s.
- **Thread Pools & ForkJoin**: Merged local results after task completion.
- **Initial ForkJoin** used `ReentrantLock`, but this was replaced for performance reasons.
- **CompletableFuture**: Managed task dependencies without explicit synchronization.

> ✅ Each model ensured thread-safety using thread-local data and post-processing aggregation.
>

---

## 📊 Performance Analysis

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