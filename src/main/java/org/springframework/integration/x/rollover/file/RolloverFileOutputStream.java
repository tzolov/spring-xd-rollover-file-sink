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
			long rolloverStartTimeMs, long rolloverPeriodMs, long maxRolledFileSize,
			String archivePrefix, boolean compressArchive, int bufferSize, FileCompressor fileCompressor)
			throws IOException {

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
		setFile(true);

		synchronized (RolloverFileOutputStream.class) {

			if (rolloverTimer == null) {
				rolloverTimer = new Timer(RolloverFileOutputStream.class.getName(), true);
			}

			rollTask = new RollTask();

			Date startTime = null;
			if (rolloverStartTimeMs <= 0) {
				Calendar now = Calendar.getInstance();
				now.setTimeZone(zone);

				GregorianCalendar midnight = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
						now.get(Calendar.DAY_OF_MONTH), 23, 0);
				midnight.setTimeZone(zone);
				midnight.add(Calendar.HOUR, 1);

				startTime = midnight.getTime();
			} else {
				startTime = new Date(rolloverStartTimeMs);
			}

			long period = (rolloverPeriodMs <= 0) ? 86400000 : rolloverPeriodMs;

			rolloverTimer.scheduleAtFixedRate(rollTask, startTime, period);
		}

		this.fileCompressor = fileCompressor;
	}

	public String getFilename() {
		return filePath;
	}

	public String getDatedFilename() {
		if (primaryFile == null) {
			return null;
		}
		return primaryFile.toString();
	}

	private synchronized void setFile(boolean force) throws IOException {

		Date now = new Date();

		File newFile = new File(fileDir, getNewFileName(now));

		if (newFile.exists() && !newFile.canWrite()) {
			throw new IOException("Cannot write in file: " + newFile);
		}

		if (!appendToFile && newFile.exists()) {
			// Expand the file name to prevents collision with existing files
			throw new IOException("File already exists but append is disabled: " + newFile);
		}

		// Do we need to change the output stream?
		if (force || !newFile.equals(primaryFile)) {

			File oldPrimaryFile = primaryFile;
			OutputStream oldOut = out;

			primaryFile = newFile;

			if (bufferSize > 0) {
				out = new BufferedOutputStream(new FileOutputStream(newFile, appendToFile), bufferSize);
			} else {
				out = new FileOutputStream(newFile, appendToFile);
			}

			if (oldOut != null) {
				oldOut.close();
				renameAndCompress(oldPrimaryFile);
			}
		}
	}

	private String getNewFileName(Date date) {
		// Is this a rollover file?
		String fileName = new File(filePath).getName();

		String newFileName = fileName;
		int i = fileName.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
		if (i >= 0) {
			newFileName = fileName.substring(0, i) + fileDateFormat.format(date)
					+ fileName.substring(i + YYYY_MM_DD.length());
		}
		return newFileName;
	}

	@Override
	public void write(byte[] buf) throws IOException {
		out.write(buf);
		checkFileSizeForRollover(buf.length);
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		out.write(buf, off, len);
		checkFileSizeForRollover(len);
	}

	@Override
	public void close() throws IOException {
		synchronized (RolloverFileOutputStream.class) {
			try {
				super.close();
				renameAndCompress(primaryFile);
				fileCompressor.compressFile(primaryFile.getAbsolutePath());
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
				RolloverFileOutputStream.this.setFile(false);
			} catch (IOException e) {
				logger.error("Rool task failed:", e);
			}
		}
	}

	private void renameAndCompress(File file) {
		if (file != null) {
			File archiveFile = file;

			// if a archivePrefix is configured we are going to
			// rename the file first
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

	private void checkFileSizeForRollover(long byteCount) {
		if (maxRolledFileSize > 0) {
			long fileSize = writtenBytesCounter.addAndGet(byteCount);
			if (fileSize >= maxRolledFileSize) {
				// Start file roll over
				synchronized (RolloverFileOutputStream.class) {
					try {
						setFile(true);
						writtenBytesCounter.set(0);
					} catch (IOException e) {
						logger.error("roll over failed:", e);
					}
				}
			}
		}
	}

}