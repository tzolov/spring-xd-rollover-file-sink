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

/**
 * RolloverFileOutputStream
 * 
 * This output stream puts content in a file that is rolled over every rolloverPeriodMs, starting from
 * rolloverStartTimeMs. The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
 * creating and rolling over the file.
 * 
 * Old files are retained for a number of days before being deleted.
 */
public class RolloverFileOutputStream extends FilterOutputStream {

	private static Timer rolloverTimer;

	final static String YYYY_MM_DD = "yyyy_mm_dd";
	final static String ROLLOVER_FILE_DATE_FORMAT = "yyyy_MM_dd";
	final static String ROLLOVER_FILE_BACKUP_FORMAT = "HHmmssSSS";
	final static int ROLLOVER_FILE_RETAIN_DAYS = 31;
	final static long DEFAULT_ROLLOVER_PERIOD = 1000L * 60 * 60 * 24;

	private RollTask rollTask;
	private SimpleDateFormat fileBackupFormat;
	private SimpleDateFormat fileDateFormat;

	private String filePath;
	private File primaryFile;
	private boolean appendToFile;
	private int fileRetainDays;
	private long maxRolledFileSize;

	private AtomicLong writtenBytesCounter = new AtomicLong(0);

	private File fileDir;

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
	 *            creating and rolling over the file.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename) throws IOException {
		this(filename, true, ROLLOVER_FILE_RETAIN_DAYS);
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
	 *            creating and rolling over the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append) throws IOException {
		this(filename, append, ROLLOVER_FILE_RETAIN_DAYS);
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
	 *            creating and rolling over the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param retainDays
	 *            The number of days to retain files before deleting them. 0 to retain forever.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append, int retainDays) throws IOException {
		this(filename, append, retainDays, TimeZone.getDefault());
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
	 *            creating and rolling over the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param retainDays
	 *            The number of days to retain files before deleting them. 0 to retain forever.
	 * @param zone
	 *            the timezone for the output
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append, int retainDays, TimeZone zone) throws IOException {
		this(filename, append, retainDays, zone, null, null, -1, DEFAULT_ROLLOVER_PERIOD, -1);
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
	 *            creating and rolling over the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param retainDays
	 *            The number of days to retain files before deleting them. 0 to retain forever.
	 * @param zone
	 *            the timezone for the output
	 * @param dateFormat
	 *            The format for the date file substitution. The default is "yyyy_MM_dd".
	 * @param backupFormat
	 *            The format for the file extension of backup files. The default is "HHmmssSSS".
	 * @param rolloverStartTimeMs
	 *            Defines the time in [ms] of the first file roll over process to start.
	 * @param rolloverPeriodMs
	 *            Defines the frequency (in ms) of the file roll over processes.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append, int retainDays, TimeZone zone, String dateFormat,
			String backupFormat, long rolloverStartTimeMs, long rolloverPeriodMs, long maxRolledFileSize)
			throws IOException {

		super(null);

		this.maxRolledFileSize = maxRolledFileSize;

		if (dateFormat == null) {
			dateFormat = ROLLOVER_FILE_DATE_FORMAT;
		}

		fileDateFormat = new SimpleDateFormat(dateFormat);

		if (backupFormat == null) {
			backupFormat = ROLLOVER_FILE_BACKUP_FORMAT;
		}

		fileBackupFormat = new SimpleDateFormat(backupFormat);

		fileBackupFormat.setTimeZone(zone);
		fileDateFormat.setTimeZone(zone);

		if (StringUtils.isEmpty(filename)) {
			throw new IllegalArgumentException("Invalid filename");
		}

		filePath = filename.trim();
		fileDir = new File(filename).getParentFile();

		if (!fileDir.isDirectory() || !fileDir.canWrite()) {
			throw new IOException("Cannot write into directory: " + fileDir);
		}

		appendToFile = append;
		fileRetainDays = retainDays;
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

	public int getRetainDays() {
		return fileRetainDays;
	}

	private synchronized void setFile(boolean force) throws IOException {

		Date now = new Date();

		File newFile = new File(fileDir, getNewFileName(now));

		if (newFile.exists() && !newFile.canWrite()) {
			throw new IOException("Cannot write in file: " + newFile);
		}

		if (!appendToFile && newFile.exists()) {
			// Expand the file name to prevents collision with existing files
			newFile.renameTo(new File(newFile.getAbsolutePath() + "." + fileBackupFormat.format(now)));
		}

		// Do we need to change the output stream?
		if (force || !newFile.equals(primaryFile)) {

			primaryFile = newFile;

			OutputStream oldOut = out;

			out = new FileOutputStream(newFile, appendToFile);

			if (oldOut != null) {
				oldOut.close();
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

	private void removeOldFiles() {
		if (fileRetainDays > 0) {
			long now = System.currentTimeMillis();

			File file = new File(filePath);
			File dir = new File(file.getParent());
			String fn = file.getName();
			int s = fn.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
			if (s < 0) {
				return;
			}
			String prefix = fn.substring(0, s);
			String suffix = fn.substring(s + YYYY_MM_DD.length());

			String[] logList = dir.list();
			for (int i = 0; i < logList.length; i++) {
				fn = logList[i];
				if (fn.startsWith(prefix) && fn.indexOf(suffix, prefix.length()) >= 0) {
					File f = new File(dir, fn);
					long date = f.lastModified();
					if (((now - date) / (1000 * 60 * 60 * 24)) > fileRetainDays)
						f.delete();
				}
			}
		}
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
				RolloverFileOutputStream.this.removeOldFiles();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void checkFileSizeForRollover(long byteCount) {
		if (maxRolledFileSize > 0) {
			long fileSize = writtenBytesCounter.addAndGet(byteCount);
			if (fileSize >= maxRolledFileSize) {
				// Start file rolling over
				synchronized (RolloverFileOutputStream.class) {
					try {
						setFile(true);
						writtenBytesCounter.set(0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}