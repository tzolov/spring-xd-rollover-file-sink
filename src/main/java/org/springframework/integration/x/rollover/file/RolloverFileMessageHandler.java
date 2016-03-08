/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.x.rollover.file;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
public class RolloverFileMessageHandler extends AbstractMessageHandler implements Lifecycle {

	private Logger logger = LoggerFactory.getLogger(RolloverFileMessageHandler.class);

	private String filename;
	private boolean append;
	private String timeZoneID;
	private String dateFormat;

	private int bufferSize = 8192;
	private long flushRate = 0;

	private long rolloverPeriod = 1000L * 60 * 60 * 24;

	private long maxRolledFileSize = -1;
	private String archivePrefix = "";
	private boolean compressArchive = true;

	private AtomicLong messageCounter;
	private volatile boolean running = false;
	private OutputStream outputStream = null;

	@Autowired
	private FileCompressor fileCompressor;

	private boolean binary;

	public RolloverFileMessageHandler() {
	}

	@Override
	public void start() {

		if (outputStream == null) {
			try {

				long startRolloverTimeMs = (rolloverPeriod > 0) ? new Date().getTime() : -1;

				RolloverFileOutputStream rolloverFileOutputStream = new RolloverFileOutputStream(filename, append,
						TimeZone.getTimeZone(timeZoneID), dateFormat, startRolloverTimeMs, rolloverPeriod,
						maxRolledFileSize, archivePrefix, compressArchive, bufferSize, fileCompressor);

				outputStream = rolloverFileOutputStream;

				messageCounter = new AtomicLong(0);

				running = true;

				logger.info("Rollover File Sink Started");

			} catch (IOException e) {
				logger.error("Filed to create rollover output stream", e);
			}
		}
	}

	@Override
	public void stop() {

		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				logger.error("Filed to close the rollover output stream", e);
			} finally {
				running = false;
				outputStream = null;
				logger.info("Rollover File Sink Stoped");
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();

		if (payload instanceof String) {
			try {
				String s = (String) payload;
				if (!binary) {
					s += "\n";
				}
				IOUtils.write(s, outputStream);
			} catch (IOException e) {
				logger.error("Failed to write payload to rollover output stream", e);
			}
		} else if (payload instanceof byte[]) {
			try {
				IOUtils.write(((byte[]) payload), outputStream);
			} catch (IOException e) {
				logger.error("Failed to write payload to rollover output stream", e);
			}
		} else {
			throw new MessagingException(message, "Only String and byte[] message payload are supported");
		}

		if (flushRate > 0 && ((messageCounter.addAndGet(1) % flushRate) == 0)) {
			outputStream.flush();
		}
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	public String getTimeZoneID() {
		return timeZoneID;
	}

	public void setTimeZoneID(String zoneId) {
		this.timeZoneID = zoneId;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public long getFlushRate() {
		return flushRate;
	}

	public void setFlushRate(long flushRate) {
		this.flushRate = flushRate;
	}

	public long getRolloverPeriod() {
		return rolloverPeriod;
	}

	public void setRolloverPeriod(long rolloverPeriod) {
		this.rolloverPeriod = rolloverPeriod;
	}

	public long getMaxRolledFileSize() {
		return maxRolledFileSize;
	}

	public void setMaxRolledFileSize(long maxRolledFileSize) {
		this.maxRolledFileSize = maxRolledFileSize;
	}

	public String getArchivePrefix() {
		return archivePrefix;
	}

	public void setArchivePrefix(String archivePrefix) {
		this.archivePrefix = archivePrefix;
	}

	public boolean isCompressArchive() {
		return compressArchive;
	}

	public void setCompressArchive(boolean compressArchive) {
		this.compressArchive = compressArchive;
	}

	public boolean isBinary() {
		return binary;
	}

	public void setBinary(boolean binary) {
		this.binary = binary;
	}
}
