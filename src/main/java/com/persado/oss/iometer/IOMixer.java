package com.persado.oss.iometer;

import java.io.File;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class IOMixer extends Logging implements Runnable {

	CyclicBarrier myBarrier;
	int maxThreads;
	String fileName;
	boolean read;
	AtomicInteger readers,writers;
	
	public IOMixer(CyclicBarrier ioTestBarrier, int threads,
			boolean readOnly, AtomicInteger mixerReaders, AtomicInteger mixerWriters) {
		myBarrier = ioTestBarrier;
		maxThreads = threads;
		fileName = "bench" + Thread.currentThread().getId() + ".dat";
		read = readOnly;
		readers = mixerReaders;
		writers = mixerWriters;
	}

	@Override
	public void run() {

		if (read) {
			readers.incrementAndGet();
			File toTest = new File(fileName);
			doRandomReadWriteTest(toTest, RWChoice.READ);
		} else {
			writers.incrementAndGet();
			File toTest = new File(fileName);
			doRandomReadWriteTest(toTest, RWChoice.WRITE);
		}

		try {
			myBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
