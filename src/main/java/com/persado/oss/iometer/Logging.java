package com.persado.oss.iometer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

abstract class Logging {

	protected ExecutorService pool = Executors.newCachedThreadPool();

	protected int threads;
	protected long fileSize;
	protected int bufferSize;
	protected byte[] dataBlock;
	
	protected int availablePoints;
	protected int seeksToTry;



	protected Random random = new Random( System.nanoTime() );
	

	protected HashMap<String, Calculator> statsMap = new HashMap<String, Calculator>();

	protected void calcMBsec(String operation, long start, long end, long bytes) {
		statsMap.put(operation + "-" + Thread.currentThread().getId(),
				new Calculator(operation, start, end, bytes));
	}
	
	class Calculator {
		private long start;
		private long end;
		private long seconds;
		private long bytes;
		double mbPerSec;
		private String operation;

		public Calculator(String op, long st, long ed, long bt) {
			start = st;
			end = ed;
			bytes = bt;
			operation = op;
			seconds = (end - start) / 1000;
			mbPerSec = (bytes * 1.0 / 1024 / 1024) / seconds;
		}

		public String toString() {
			return "[Test " + operation + " duration : " + seconds + " sec, "
					+ mbPerSec + " MB/sec]";
		}
	}
	
	Map<String, Map<Integer, AtomicInteger>> iops = new HashMap<String, Map<Integer,AtomicInteger>>(180);
	
	protected void addIOP(String ofWhichType) {
		int second = (int) (System.currentTimeMillis() / 1000);
		Map<Integer, AtomicInteger> stats = iops.get(ofWhichType);
		synchronized (iops) {
			if (!iops.containsKey(ofWhichType)) {
				stats = new HashMap<Integer,AtomicInteger>(120);
				iops.put(ofWhichType, stats);
			} else if (stats == null) {
				stats = iops.get(ofWhichType);
			}
		}
		
		boolean newinsert = false;
		
		synchronized (stats) {
			if (!stats.containsKey(second)) {
				stats.put(second, new AtomicInteger(1));
				newinsert = true;
			} 
		}
		if (!newinsert) {
			stats.get(second).incrementAndGet();
		}
		
	}
	
	
	enum RWChoice {
		READ,
		WRITE,
		READWRITE
	}
	
	protected void doRandomReadWriteTest(File toTest, RWChoice choice) {
		RandomAccessFile raf = null;
		long start = System.currentTimeMillis();
		try {
			byte[] tempBuff = new byte[bufferSize];
			raf = new RandomAccessFile(toTest,"rw");
			long seekPoint = 0;
			for (int i = 0; i < seeksToTry; i++) {
				
				switch (choice) {
				case READ: {
					seekPoint = ((long) (random.nextDouble() * availablePoints) * bufferSize);
					// log("seekpoint:"+seekPoint);
					raf.seek(seekPoint);
					raf.read(tempBuff);
					addIOP("Read Random");
					break;
				}
				case WRITE: {
					seekPoint = ((long) (random.nextDouble() * availablePoints) * bufferSize);
					// log("seekpoint:"+seekPoint);
					raf.seek(seekPoint);
					raf.write(dataBlock);
					addIOP("Write Random");
					break;
				}
				case READWRITE: {
					seekPoint = ((long) (random.nextDouble() * availablePoints) * bufferSize);
					// log("seekpoint:"+seekPoint);
					raf.seek(seekPoint);
					raf.read(tempBuff);
					seekPoint = ((long) (random.nextDouble() * availablePoints) * bufferSize);
					// log("seekpoint:"+seekPoint);
					raf.seek(seekPoint);
					raf.write(dataBlock);
					addIOP("ReadWrite Random");
					break;
				}
				}
				
				seekPoint = ((long) ( random.nextDouble() * availablePoints) * bufferSize);
				//log("seekpoint:"+seekPoint);
				raf.seek(seekPoint);
				raf.read(tempBuff);
				seekPoint = ((long) ( random.nextDouble() * availablePoints) * bufferSize);
				//log("seekpoint:"+seekPoint);
				raf.seek(seekPoint);
				raf.write(dataBlock);
				
				
				
			}
		} catch (IOException e) {
			log("FAILED TO DO RANDOM RW TEST");
			e.printStackTrace();
		} finally {
			if (raf!=null) {
				try {
					raf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		long end = System.currentTimeMillis();
		
		calcMBsec("READWRITERANDOM", start, end, seeksToTry*bufferSize*2);

	}

	protected void doRandomReadTest(File toTest) {
		RandomAccessFile raf = null;
		long start = System.currentTimeMillis();
		try {
			byte[] tempBuff = new byte[bufferSize];
			raf = new RandomAccessFile(toTest,"r");
			
			for (int i = 0; i < seeksToTry; i++) {
				long seekPoint = ((long) ( random.nextDouble() * availablePoints) * bufferSize);
				raf.seek(seekPoint);
				raf.read(tempBuff);
				addIOP("Read Random");
			}
		} catch (IOException e) {
			log("FAILED TO DO RANDOM TEST");
			e.printStackTrace();
		} finally {
			if (raf!=null) {
				try {
					raf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		long end = System.currentTimeMillis();
		
		calcMBsec("READRANDOM", start, end, seeksToTry*bufferSize);

	}
	
	
	
	
	protected void doSequentialReadTest(File toTest) {
		BufferedInputStream str = null;
		long start = System.currentTimeMillis();

		try {
			str = new BufferedInputStream(new FileInputStream(toTest),
					bufferSize);
			byte[] data = new byte[bufferSize];

			int r = 0;
			while ((r = str.read(data)) != -1) {
				// nothing, just read
				addIOP("Read Sequential");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();

		calcMBsec("READ", start, end, fileSize);
	}

	protected void createFileForTest() {
		long start = System.currentTimeMillis();
		BufferedOutputStream str = null;
		try {
			String fileName = "bench" + Thread.currentThread().getId() + ".dat";
			str = new BufferedOutputStream(new FileOutputStream(new File(
					fileName)), bufferSize);

			long fileSizeNow = 0;//must be a long!
			log("Creating a file of size " + (fileSize/1024/1024)
					+ " MB using a buffer of " + bufferSize + " bytes");
			byte[] temp = Arrays.copyOfRange(dataBlock,0,dataBlock.length);
			while (fileSizeNow < fileSize) {
				str.write(temp);
				addIOP("Write");
				fileSizeNow += temp.length;
			}
			str.flush();
		} catch (IOException ioe) {
			log("Failed to create file, error was " + ioe.getMessage());
		} finally {
			if (str != null) {
				try {
					str.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		long end = System.currentTimeMillis();

		calcMBsec("WRITE", start, end, fileSize);
	}
	
	/**
	 * dummy log entry
	 * 
	 * @param message
	 */
	public static void log(String message) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HH:mm:SS");
		System.out.println(sdf.format(new Date()) + " ["
				+ Thread.currentThread().getName() + "] " + message);
	}
}
