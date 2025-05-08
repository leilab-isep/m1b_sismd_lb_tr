# Link for the wikipedia dump

https://dumps.wikimedia.org/enwiki/20250401/enwiki-20250401-pages-articles-multistream1.xml-p1p41242.bz2

# Completable Futures

# Fork Join Pool

Initially I had done a universal Counter that acts with a lock mechanism.

![Reentrant Lock](images/CounterLock.png)

But that was ended being too slow. Even slower than the sequential one as seen in the image.

![Fork Join Pool time](images/ForkJoinPoolTime.png)

So had to optimize the method. I decided that each Parsing is going to create a HashMap and, then, we merge them
together.

# Without Thread Group

# With Thread Group
