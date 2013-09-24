package com.persado.oss.iometer;

import java.io.File;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public
class IOThread extends Logging implements Runnable {

	CyclicBarrier fileBarrier;
	CyclicBarrier ioTestBarrier;
	String fileName;

	public IOThread(CyclicBarrier fileBarrier, CyclicBarrier ioTestBarrier) {
		this.fileBarrier = fileBarrier;
		this.ioTestBarrier = ioTestBarrier;
	}

	@Override
	public void run() {
		try {
			createFileForTest();
			fileBarrier.await();
		} catch (InterruptedException e) {
			log(e.getMessage());
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			log(e.getMessage());
			e.printStackTrace();
		}
		
		
		try {
			doIOBenchmark();
			ioTestBarrier.await();
		} catch (InterruptedException e) {
			log(e.getMessage());
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			log(e.getMessage());
			e.printStackTrace();
		}

	}

	private void doIOBenchmark() {
		File toTest = new File(fileName);
		if (!toTest.exists() || !toTest.canWrite()) {
			log("File not found or cannot write to it.");
		} else {
			toTest.deleteOnExit(); // cleanup hook
			log("Starting sequential read test");
			doSequentialReadTest(toTest);
			log("Starting random read test");
			doRandomReadTest(toTest);
			log("Starting random read-write test");
			doRandomReadWriteTest(toTest, RWChoice.READWRITE);
			log("worker done.");
		}
	}

	

}