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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private int retainDays;
	private String timeZoneID;
	private String dateFormat;
	private String backupFormat;

	private int bufferSize = 8192;
	private long flushRate = 0;

	private boolean startRolloverNow = false;
	private long rolloverPeriod = 1000L * 60 * 60 * 24;

	private AtomicLong messageCounter;
	private volatile boolean running = false;
	private OutputStream outputStream = null;

	public RolloverFileMessageHandler() {
	}

	@Override
	public void start() {

		if (outputStream == null) {
			try {

				long startRolloverTimeMs = -1;

				if (startRolloverNow) {
					startRolloverTimeMs = new Date().getTime();
				}

				RolloverFileOutputStream rolloverFileOutputStream = new RolloverFileOutputStream(filename, append,
						retainDays, TimeZone.getTimeZone(timeZoneID), dateFormat, backupFormat, startRolloverTimeMs,
						rolloverPeriod);

				if (bufferSize > 0) {
					outputStream = new BufferedOutputStream(rolloverFileOutputStream, bufferSize);
				} else {
					outputStream = rolloverFileOutputStream;
				}

				if (flushRate > 0) {
					messageCounter = new AtomicLong(0);
				}

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
				IOUtils.write(((String) payload), outputStream);
			} catch (IOException e) {
				logger.error("Filed to write payload to rollover output stream", e);
			}
		} else if (payload instanceof byte[]) {
			try {
				IOUtils.write(((byte[]) payload), outputStream);
			} catch (IOException e) {
				logger.error("Filed to write payload to rollover output stream", e);
			}
		} else {
			throw new MessagingException(message, "Only String and byte[] message payload are supported");
		}

		if (flushRate > 0) {
			if ((messageCounter.get() % flushRate) == 0) {
				outputStream.flush();
			}
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

	public int getRetainDays() {
		return retainDays;
	}

	public void setRetainDays(int retainDays) {
		this.retainDays = retainDays;
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

	public String getBackupFormat() {
		return backupFormat;
	}

	public void setBackupFormat(String backupFormat) {
		this.backupFormat = backupFormat;
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

	public boolean isStartRolloverNow() {
		return startRolloverNow;
	}

	public void setStartRolloverNow(boolean startRolloverNow) {
		this.startRolloverNow = startRolloverNow;
	}

	public long getRolloverPeriod() {
		return rolloverPeriod;
	}

	public void setRolloverPeriod(long rolloverPeriod) {
		this.rolloverPeriod = rolloverPeriod;
	}
}
