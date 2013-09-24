package com.persado.oss.iometer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class IOMeter extends Logging {

	private static final int DEFAULT_BUFSIZE = 4 * 1024;
	private static final long DEFAULT_FILESIZE = 2 * 1024 * 1024 * 1024L;
	private static final int DEFAULT_THREADS = Runtime.getRuntime()
			.availableProcessors() * 2;

	public IOMeter(int _threads, long _filesize, int _buffersize) {
		threads = _threads;
		fileSize = _filesize;
		bufferSize = _buffersize;
		long fileSizeMB = fileSize / 1024 / 1024;
		log("Configured with " + threads + " Threads, " + fileSizeMB
				+ "MB test file size, " + bufferSize + " bytes buffer size.");
		dataBlock = new byte[bufferSize];
		for (int i = 0; i < bufferSize; i++) {
			dataBlock[i] = (byte) (Math.random() * 255.0);
		}

		availablePoints = (int) (fileSize / bufferSize) - 2;
		seeksToTry = Math.max(10000, availablePoints / 100);
		log("Will create temporary files of " + threads * fileSizeMB + "MB. ");
		log("WARNING: If your machine has more memory than this, the test may be invalid.");
		log("Out of " + availablePoints + " slots, will try " + seeksToTry
				+ " in the generated files.");

	}

	public static void main(String[] args) {
		log("IOMeter --threads=N --filesize=Y (MB) --buffersize=Z (bytes)");
		IOMeter meter = null;
		int threads = DEFAULT_THREADS;
		long filesize = DEFAULT_FILESIZE;
		int buffersize = DEFAULT_BUFSIZE;

		if (args.length > 0) {
			threads = parseArgument("--threads", args, threads);
			buffersize = parseArgument("--buffersize", args, buffersize);
			filesize = (parseArgument("--filesize", args, filesize) * 1024) * 1024;
		}

		meter = new IOMeter(threads, filesize, buffersize);

		meter.doBenchmark();

	}

	private static <T> T parseArgument(String argumentName, String[] args,
			T defaultValue) {

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(argumentName)) {
				String[] nameValue = args[i].split("=");
				String value = nameValue[1];
				if (defaultValue.getClass().isAssignableFrom(
						java.lang.Integer.class)) {
					return (T) Integer.valueOf(value);
				} else if (defaultValue.getClass().isAssignableFrom(
						java.lang.Long.class)) {
					return (T) Long.valueOf(value);
				}

			}
		}

		return defaultValue;
	}

	private void doBenchmark() {
		CyclicBarrier fileCreationBarrier = new CyclicBarrier(threads); // threads
																		// all
																		// create
																		// files
																		// on
																		// this
																		// barrier
		CyclicBarrier ioTestBarrier = new CyclicBarrier(threads + 1); // threads
																		// all
																		// do
																		// tests
																		// on
																		// this
																		// barrier
		for (int i = 0; i < threads; i++) {
			pool.execute(new IOThread(fileCreationBarrier, ioTestBarrier));
		}

		try {
			// log("waiting for IO Tests to complete");
			ioTestBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Thread.currentThread().setName("Statistics");
		printStats(statsMap);
		printIOPs();

		log("Starting a 25% read test");
		mixerTest(ioTestBarrier, 0.25f);
		log("Starting a 75% read test");
		mixerTest(ioTestBarrier, 0.75f);
		log("Test complete, threads exited.");

		pool.shutdownNow();
	}

	/**
	 * Do a mixer test with a given percentage of read over write
	 * 
	 * @param ioTestBarrier
	 * @param pctRead
	 */
	private void mixerTest(CyclicBarrier ioTestBarrier, double pctRead) {
		ioTestBarrier.reset();

		mixerReaders.set(0);
		mixerWriters.set(0);
		int calc = (int) ((1.0 * threads) * pctRead);
		for (int i = 0; i < threads; i++) {
			pool.execute(new IOMixer(ioTestBarrier, threads, i < calc,
					mixerReaders, mixerWriters));
		}
		try {
			ioTestBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Thread.currentThread().setName("Statistics");
		log("IO Mixer thread mix : " + mixerReaders.get() + " Readers, "
				+ mixerWriters.get() + " Writers.");
		printIOPs();

	}

	private void printIOPs() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int count = 0;
		int sum = 0;
		// log("\n------\n"+iops+"\n------");
		for (String statType : iops.keySet()) {
			Map<Integer, AtomicInteger> stats = iops.get(statType);
			for (int second : stats.keySet()) {
				int value = stats.get(second).intValue();
				if (value != 0) {
					min = Math.min(min, value);
					max = Math.max(max, value);
					count++;
					sum += value;
				}
			}
			double avg = sum * 1.0 / count;
			log(" * IOPS (" + statType + "): min : " + min + ", max: " + max
					+ ", average: " + avg);
		}
		iops.clear();
	}

	private void printStats(HashMap<String, Calculator> smaps) {

		HashMap<String, Double> totals = new HashMap<String, Double>();
		// totals
		for (String operation : smaps.keySet()) {
			String[] split = operation.split("-");
			if (totals.containsKey(split[0])) {
				totals.put(split[0],
						totals.get(split[0]) + smaps.get(operation).mbPerSec);
			} else {
				totals.put(split[0], smaps.get(operation).mbPerSec);
			}
		}
		log(" ");
		for (String total : totals.keySet()) {
			log(" * " + total + " " + totals.get(total) + " MB/sec");
		}
		smaps.clear();
	}

	AtomicInteger mixerReaders = new AtomicInteger(0);
	AtomicInteger mixerWriters = new AtomicInteger(0);

}
