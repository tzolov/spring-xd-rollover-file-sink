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

/**
 * RolloverFileOutputStream
 * 
 * This output stream puts content in a file that is rolled over every
 * rolloverPeriodMs, starting from rolloverStartTimeMs. The filename must
 * include the string "yyyy_mm_dd", which is replaced with the actual date when
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

	private String fileName;
	private File outputFile;
	private boolean appendToFile;
	private int fileRetainDays;

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is
	 *            replaced with the actual date when creating and rolling over
	 *            the file.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename) throws IOException {
		this(filename, true, ROLLOVER_FILE_RETAIN_DAYS);
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is
	 *            replaced with the actual date when creating and rolling over
	 *            the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append)
			throws IOException {
		this(filename, append, ROLLOVER_FILE_RETAIN_DAYS);
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is
	 *            replaced with the actual date when creating and rolling over
	 *            the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param retainDays
	 *            The number of days to retain files before deleting them. 0 to
	 *            retain forever.
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append,
			int retainDays) throws IOException {
		this(filename, append, retainDays, TimeZone.getDefault());
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is
	 *            replaced with the actual date when creating and rolling over
	 *            the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param retainDays
	 *            The number of days to retain files before deleting them. 0 to
	 *            retain forever.
	 * @param zone
	 *            the timezone for the output
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append,
			int retainDays, TimeZone zone) throws IOException {

		this(filename, append, retainDays, zone, null, null, -1,
				DEFAULT_ROLLOVER_PERIOD);
	}

	/**
	 * @param filename
	 *            The filename must include the string "yyyy_mm_dd", which is
	 *            replaced with the actual date when creating and rolling over
	 *            the file.
	 * @param append
	 *            If true, existing files will be appended to.
	 * @param retainDays
	 *            The number of days to retain files before deleting them. 0 to
	 *            retain forever.
	 * @param zone
	 *            the timezone for the output
	 * @param dateFormat
	 *            The format for the date file substitution. The default is
	 *            "yyyy_MM_dd".
	 * @param backupFormat
	 *            The format for the file extension of backup files. The default
	 *            is "HHmmssSSS".
	 * @throws IOException
	 *             if unable to create output
	 */
	public RolloverFileOutputStream(String filename, boolean append,
			int retainDays, TimeZone zone, String dateFormat,
			String backupFormat, long rolloverStartTimeMs, long rolloverPeriodMs)
			throws IOException {

		super(null);

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

		if (filename != null) {
			filename = filename.trim();
			if (filename.length() == 0)
				filename = null;
		}

		if (filename == null) {
			throw new IllegalArgumentException("Invalid filename");
		}

		fileName = filename;
		appendToFile = append;
		fileRetainDays = retainDays;
		setFile();

		synchronized (RolloverFileOutputStream.class) {

			if (rolloverTimer == null) {
				rolloverTimer = new Timer(
						RolloverFileOutputStream.class.getName(), true);
			}

			rollTask = new RollTask();

			Date startTime = null;
			if (rolloverStartTimeMs <= 0) {
				Calendar now = Calendar.getInstance();
				now.setTimeZone(zone);

				GregorianCalendar midnight = new GregorianCalendar(
						now.get(Calendar.YEAR), now.get(Calendar.MONTH),
						now.get(Calendar.DAY_OF_MONTH), 23, 0);
				midnight.setTimeZone(zone);
				midnight.add(Calendar.HOUR, 1);

				startTime = midnight.getTime();
			} else {
				startTime = new Date(rolloverStartTimeMs);
			}

			rolloverTimer.scheduleAtFixedRate(rollTask, startTime,
					rolloverPeriodMs);
		}
	}

	public String getFilename() {
		return fileName;
	}

	public String getDatedFilename() {
		if (outputFile == null) {
			return null;
		}
		return outputFile.toString();
	}

	public int getRetainDays() {
		return fileRetainDays;
	}

	private synchronized void setFile() throws IOException {
		// Check directory
		File file = new File(fileName);
		fileName = file.getCanonicalPath();
		file = new File(fileName);
		File dir = new File(file.getParent());
		if (!dir.isDirectory() || !dir.canWrite()) {
			throw new IOException("Cannot write log directory " + dir);
		}

		Date now = new Date();

		// Is this a rollover file?
		String filename = file.getName();
		int i = filename.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
		if (i >= 0) {
			file = new File(dir, filename.substring(0, i)
					+ fileDateFormat.format(now)
					+ filename.substring(i + YYYY_MM_DD.length()));
		}

		if (file.exists() && !file.canWrite()) {
			throw new IOException("Cannot write log file " + file);
		}

		// Do we need to change the output stream?
		if (out == null || !file.equals(outputFile)) {
			// Yep
			outputFile = file;
			if (!appendToFile && file.exists()) {
				file.renameTo(new File(file.toString() + "."
						+ fileBackupFormat.format(now)));
			}
			OutputStream oldOut = out;
			out = new FileOutputStream(file.toString(), appendToFile);
			if (oldOut != null) {
				oldOut.close();
				// if(log.isDebugEnabled())log.debug("Opened "+_file);
			}
		}
	}

	private void removeOldFiles() {
		if (fileRetainDays > 0) {
			long now = System.currentTimeMillis();

			File file = new File(fileName);
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
				if (fn.startsWith(prefix)
						&& fn.indexOf(suffix, prefix.length()) >= 0) {
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
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		out.write(buf, off, len);
	}

	@Override
	public void close() throws IOException {
		synchronized (RolloverFileOutputStream.class) {
			try {
				super.close();
			} finally {
				out = null;
				outputFile = null;
			}

			rollTask.cancel();
		}
	}

	private class RollTask extends TimerTask {
		@Override
		public void run() {
			try {
				RolloverFileOutputStream.this.setFile();
				RolloverFileOutputStream.this.removeOldFiles();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}