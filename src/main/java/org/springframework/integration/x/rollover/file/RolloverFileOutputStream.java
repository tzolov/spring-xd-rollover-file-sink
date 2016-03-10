//
//========================================================================
//Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//All rights reserved. This program and the accompanying materials
//are made available under the terms of the Eclipse Public License v1.0
//and Apache License v2.0 which accompanies this distribution.
//
//  The Eclipse Public License is available at
//  http://www.eclipse.org/legal/epl-v10.html
//
//  The Apache License v2.0 is available at
//  http://www.opensource.org/licenses/apache2.0.php
//
//You may elect to redistribute this code under either of these licenses.
//========================================================================
//

package org.springframework.integration.x.rollover.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RolloverFileOutputStream
 * 
 * This output stream puts content in a file that is rolled over every rolloverPeriodMs, starting from
 * rolloverStartTimeMs. The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
 * creating and rolling over the file.
 * 
 */
public class RolloverFileOutputStream extends FilterOutputStream {

	private Logger logger = LoggerFactory.getLogger(RolloverFileOutputStream.class);

	private static Timer rolloverTimer;

	final static String YYYY_MM_DD = "yyyy_mm_dd";
	final static String ROLLOVER_FILE_DATE_FORMAT = "yyyy_MM_dd";
	final static long DEFAULT_ROLLOVER_PERIOD = 1000L * 60 * 60 * 24;

	private RollTask rollTask;
	private SimpleDateFormat fileDateFormat;

	private String filePath;
	private File primaryFile;
	private boolean appendToFile;
	private long maxRolledFileSize;
	private String archivePrefix = "archive";
	private boolean compressArchive = true;
	private int bufferSize = 8192;

	private AtomicLong writtenBytesCounter = new AtomicLong(0);

	private File fileDir;

	private FileCompressor fileCompressor;

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
	 *            creating and rolling over the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param zone
	 *            the timezone for the output
	 * @param dateFormat
	 *            The format for the date file substitution. The default is "yyyy_MM_dd".
	 * @param rolloverStartTimeMs
	 *            Defines the time in [ms] of the first file roll over process to start.
	 * @param rolloverPeriodMs
	 *            Defines the frequency (in ms) of the file roll over processes.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append, TimeZone zone, String dateFormat,
			long rolloverStartTimeMs, long rolloverPeriodMs, long maxRolledFileSize, String archivePrefix,
			boolean compressArchive, int bufferSize, FileCompressor fileCompressor) throws IOException {

		super(null);

		this.bufferSize = bufferSize;
		this.compressArchive = compressArchive;
		this.archivePrefix = archivePrefix;
		this.maxRolledFileSize = maxRolledFileSize;

		if (dateFormat == null) {
			dateFormat = ROLLOVER_FILE_DATE_FORMAT;
		}

		fileDateFormat = new SimpleDateFormat(dateFormat);

		if (StringUtils.isEmpty(filename)) {
			throw new IllegalArgumentException("Invalid filename");
		}

		filePath = filename.trim();
		fileDir = new File(new File(filename).getAbsolutePath()).getParentFile();

		if (fileDir != null && (!fileDir.isDirectory() || !fileDir.canWrite())) {
			throw new IOException("Cannot write into directory: " + fileDir);
		}

		appendToFile = append;
		setFile();

		synchronized (RolloverFileOutputStream.class) {

			if (rolloverTimer == null) {
				rolloverTimer = new Timer(RolloverFileOutputStream.class.getName(), true);
			}

			rollTask = new RollTask();

			Date startTime = (rolloverStartTimeMs > 0) ? new Date(rolloverStartTimeMs) : getMidnightTime(zone);

			long rolloverPeriod = (rolloverPeriodMs <= 0) ? 86400000 : rolloverPeriodMs;

			rolloverTimer.scheduleAtFixedRate(rollTask, startTime, rolloverPeriod);
		}

		this.fileCompressor = fileCompressor;
	}

	private Date getMidnightTime(TimeZone zone) {

		Calendar now = Calendar.getInstance();
		now.setTimeZone(zone);

		GregorianCalendar midnight = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
				now.get(Calendar.DAY_OF_MONTH), 23, 0);
		midnight.setTimeZone(zone);
		midnight.add(Calendar.HOUR, 1);

		return midnight.getTime();
	}

	public String getFilename() {
		return filePath;
	}

	public String getDatedFilename() {
		return "" + primaryFile;
	}

	private synchronized void setFile() throws IOException {

		File nextFile = new File(fileDir, getNextFileName());

		if (nextFile.exists() && !nextFile.canWrite()) {
			throw new IOException("Cannot write in file: " + nextFile);
		}

		if (!appendToFile && nextFile.exists()) {
			throw new IOException("File already exists but append is disabled: " + nextFile);
		}

		File previousPrimaryFile = primaryFile;
		primaryFile = nextFile;

		OutputStream previousOut = out;
		if (bufferSize > 0) {
			out = new BufferedOutputStream(new FileOutputStream(nextFile, appendToFile), bufferSize);
		} else {
			out = new FileOutputStream(nextFile, appendToFile);
		}

		if (previousOut != null) {
			previousOut.close();
			renameAndCompress(previousPrimaryFile);
		}
	}

	private String getNextFileName() {
		Date now = new Date();
		// Is this a rollover file?
		String fileName = new File(filePath).getName();

		String nextFileName = fileName;
		int i = fileName.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
		if (i >= 0) {
			nextFileName = fileName.substring(0, i) + fileDateFormat.format(now)
					+ fileName.substring(i + YYYY_MM_DD.length());
		}
		return nextFileName;
	}

	@Override
	public void write(byte[] buf) throws IOException {
		out.write(buf);
		// checkFileSizeForRollover(buf.length);
		writtenBytesCounter.addAndGet(buf.length);
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		out.write(buf, off, len);
		// checkFileSizeForRollover(len);
		writtenBytesCounter.addAndGet(len);
	}

	@Override
	public void close() throws IOException {
		synchronized (RolloverFileOutputStream.class) {
			try {
				super.close();
				renameAndCompress(primaryFile);
			} finally {
				out = null;
				primaryFile = null;
			}
			rollTask.cancel();
		}
	}

	private class RollTask extends TimerTask {
		@Override
		public void run() {
			try {
				RolloverFileOutputStream.this.setFile();
			} catch (IOException e) {
				logger.error("Roll task failed:", e);
			}
		}
	}

	private void renameAndCompress(File file) {
		if (file != null) {
			File archiveFile = file;

			// if a archivePrefix is configured we are going to rename the file first
			if (!StringUtils.isEmpty(archivePrefix) && file != null) {
				archiveFile = new File(file.getParentFile(), archivePrefix + "." + file.getName());
				file.renameTo(archiveFile);
			}

			// compress file
			if (compressArchive) {
				fileCompressor.compressFile(archiveFile.getAbsolutePath());
			}
		}
	}

	public void rolloverOnFileSize() {
		if (maxRolledFileSize > 0) {
			long fileSize = writtenBytesCounter.get();
			if (fileSize >= maxRolledFileSize) {
				// Start file roll over
				synchronized (RolloverFileOutputStream.class) {
					try {
						setFile();
						writtenBytesCounter.set(0);
					} catch (IOException e) {
						logger.error("roll over failed:", e);
					}
				}
			}
		}
	}
}