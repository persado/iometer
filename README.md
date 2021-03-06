#IOMeter

IOMeter is our attempt to measure the effect of different block size on I/O and how this translates to disks on magnetic media, SSD and the cloud (especially EC2). We use this tool internally to measure performance of EC2 Elastic Block Storage with and without IOPS, so it can be translated to a number most of us understand (MB/s).

##Usage
IOMeter understands the following parameters:
 * --threads=<..> number of concurrent readers/writers to allocate, default is 2 x the CPU cores
 * --filesize=<..> the amount of MB to generate for each reader/writer, default is 2GB.
 * --buffersize=<..> the amount (in bytes) for the data to use, default is 4096 bytes (1 page).

Note: the total filesize used (shown when the application is running) should exceed the amount of RAM on the machine tested by _at least twice_ otherwise the result will not be accurate.
Examples for correct usage:
 * Machine with 4 CPU-cores and 8GB RAM (e.g. MacBook Pro 2011, 8GB RAM) use: --filesize=2048 (16GB test files)
 * Machine with 8 CPU-cores and 16GB RAM (e.g. MacBook Pro Retina 16GB) use: --filesize=4096 (64GB test files)
 * Machine with 8 CPU-cores and 8GB RAM (e.g. Desktop i7 with 8GB RAM) use: --filesize=2048 (32GB test files)

##Tests executed
The following tests are executed in the sequence as described below:
###Write test
This test writes concurrently for all threads specified up to the filesize specified. Measures aggregate performance of the medium for writing and it is important that the total filesize exceeds available RAM so the machine is forced to write to disk. The number reported is the total average speed achieved.

###Read test
This test reads concurrently, for all threads, the complete files created from beginning to end. Measures aggregate medium performance for a threaded load - this number is smaller than the pure read speed due to multiple readers contenting for the medium.

###Random read test
For this test, each thread picks randomly some points (no less than 10000 points) in a file and reads each of them to completion. This is a thrashing test, trying to read as fast as possible - in real life this resembles a [MongoDB](http://www.mongodb.org) instance doing lots of queries that page-fault most of the time (depending on size of the files chosen).

###Random read-write test
This is a variation of the previous test, in this case each thread selects 20000 points in the file, doing a seek, a read, another seek and a write in this order. The workload here resembles a [MongoDB](http://www.mongodb.org) under 50% read 50% write load.

##Checking I/O size
Modifying the parameter --buffersize changes the I/O for the device used. The default is 4K which resembles a single page (4K) on Intel architectures, useful for benchmarking storage for memory-mapped applications, e.g [MongoDB](http://www.mongodb.org).

##Sample runs
###MacBook Pro Retina: 16GB RAM, 512GB SSD
```
java -cp iometer-0.10.0-SNAPSHOT.jar  com.persado.oss.iometer.IOMeter --filesize=4000
130917-17:53:990 [main] IOMeter --threads=N --filesize=Y (MB) --buffersize=Z (bytes)
130917-17:53:02 [main] Configured with 16 Threads, 4000MB test file size, 4096 bytes buffer size.
130917-17:53:06 [main] Will create temporary files of 64000MB.
130917-17:53:06 [main] WARNING: If your machine has more memory than this, the test may be invalid.
130917-17:53:06 [main] Out of 1023998 slots, will try 10239 in the generated files.
...
130917-17:58:431 [Statistics]  * READRANDOM 279.97265625 MB/sec
130917-17:58:431 [Statistics]  * READ 492.3076923076923 MB/sec
130917-17:58:432 [Statistics]  * READWRITERANDOM 182.83928571428575 MB/sec
130917-17:58:432 [Statistics]  * WRITE 369.2777888512391 MB/sec
130917-17:58:432 [Statistics]  * IOPS (Read Random): min : 16418, max: 58869, average: 40956.0
130917-17:58:432 [Statistics]  * IOPS (Read Sequential): min : 16418, max: 126977, average: 121675.17647058824
130917-17:58:433 [Statistics]  * IOPS (ReadWrite Random): min : 6700, max: 126977, average: 115252.74482758621
130917-17:58:433 [Statistics]  * IOPS (Write): min : 6700, max: 127401, average: 103423.9
```

So linear reads are 492MB/s, linear writes 369MB/s, random reads at 279MB/s and random read-writes at 182MB/s.

###MacBook Pro (early 2011): 8GB RAM, 512GB @ 5400rpm HDD
```
java -cp target/iometer-0.10.0-SNAPSHOT.jar  com.persado.oss.iometer.IOMeter
130917-16:59:140 [main] IOMeter --threads=N --filesize=Y (MB) --buffersize=Z (bytes)
130917-16:59:145 [main] Configured with 8 Threads, 2048MB test file size, 4096 bytes buffer size.
130917-16:59:149 [main] Will create temporary files of 16384MB.
130917-16:59:149 [main] WARNING: If your machine has more memory than this, the test may be invalid.
130917-16:59:149 [main] Out of 524286 slots, will try 10000 in the generated files.
...
130917-17:45:715 [Statistics]  * READRANDOM 0.3995701330554561 MB/sec
130917-17:45:715 [Statistics]  * READ 36.90150895753381 MB/sec
130917-17:45:715 [Statistics]  * READWRITERANDOM 0.553208539894697 MB/sec
130917-17:45:715 [Statistics]  * WRITE 41.72248881530127 MB/sec
130917-17:45:716 [Statistics]  * IOPS (Read Random): min : 13, max: 180, average: 101.1378002528445
130917-17:45:717 [Statistics]  * IOPS (Read Sequential): min : 13, max: 13666, average: 3447.0193548387097
130917-17:45:718 [Statistics]  * IOPS (ReadWrite Random): min : 13, max: 13666, average: 1821.1225428690925
130917-17:45:718 [Statistics]  * IOPS (Write): min : 8, max: 36096, average: 3060.7261009667027
```
Situation here is largely different: linear reads at 36.9MB/s, writes at 41MB/s, random reads at 0.4MB/s and random read-writes at 0.55MB/s.

Your own results may differ - _note that you should be using files that total at least 70-80% more than available memory._

###Amazon AWS hi1.xlarge high IOPS machine type
The following are results of the hi1.xlarge machine type in Amazon AWS:

####Test with default (512) readahead:
java -jar iometer-0.10.0-SNAPSHOT.jar --filesize=4096
#####Single volume (1 SSD)
```
131018-14:26:901 [Statistics]  * READRANDOM 41.02692758934061 MB/sec
131018-14:26:901 [Statistics]  * READ 916.3071897609489 MB/sec
131018-14:26:901 [Statistics]  * READWRITERANDOM 205.96939609486716 MB/sec
131018-14:26:901 [Statistics]  * WRITE 579.2481623848327 MB/sec
131018-14:26:902 [Statistics]  * IOPS (Read Random): min : 38, max: 12677, average: 3607.7419354838707
131018-14:26:902 [Statistics]  * IOPS (Read Sequential): min : 38, max: 534201, average: 125055.17343173432
131018-14:26:902 [Statistics]  * IOPS (ReadWrite Random): min : 38, max: 534201, average: 113329.3774834437
131018-14:26:902 [Statistics]  * IOPS (Write): min : 38, max: 577786, average: 128128.3629489603
```
#####Two volumes (2 x SSD, RAID 0)
```
131018-14:35:197 [Statistics]  * READRANDOM 85.12278672855781 MB/sec
131018-14:35:197 [Statistics]  * READ 1305.7095058667892 MB/sec
131018-14:35:197 [Statistics]  * READWRITERANDOM 969.31640625 MB/sec
131018-14:35:197 [Statistics]  * WRITE 1083.5872093476285 MB/sec
131018-14:35:198 [Statistics]  * IOPS (Read Random): min : 99, max: 91626, average: 15977.142857142857
131018-14:35:198 [Statistics]  * IOPS (Read Sequential): min : 99, max: 1025422, average: 271119.616
131018-14:35:198 [Statistics]  * IOPS (ReadWrite Random): min : 99, max: 1025422, average: 261263.14503816795
131018-14:35:198 [Statistics]  * IOPS (Write): min : 99, max: 1025422, average: 265803.5450980392
```
#####Test with readahead 16 sectors, two volumes (2 x SSD, RAID 0):
java -jar iometer-0.10.0-SNAPSHOT.jar --filesize=4096
```
131018-14:58:869 [Statistics]  * READRANDOM 559.74609375 MB/sec
131018-14:58:869 [Statistics]  * READ 805.6073620623773 MB/sec
131018-14:58:870 [Statistics]  * READWRITERANDOM 1215.05859375 MB/sec
131018-14:58:870 [Statistics]  * WRITE 1084.0859504132231 MB/sec
131018-14:58:870 [Statistics]  * IOPS (Read Random): min : 4257, max: 85366, average: 37280.0
131018-14:58:870 [Statistics]  * IOPS (Read Sequential): min : 3782, max: 253145, average: 190392.98876404495
131018-14:58:871 [Statistics]  * IOPS (ReadWrite Random): min : 961, max: 253145, average: 184007.91397849462
131018-14:58:871 [Statistics]  * IOPS (Write): min : 961, max: 635765, average: 220064.62337662338
```
###Comparing Hard disks to SSD
In the examples above, you can see that the random read result shows 0.55 MB/sec for the hard disk on the MacbookPro laptop. The SSD laptop does 182.83 MB/sec _on average_; this is a great order of magnitude faster. The enterprise-class SSDs in AWS hi1.xlarge machine do 1215.05 MB/sec using a readahead of 16 sectors which is very impressive!




