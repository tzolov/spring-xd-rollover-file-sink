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

import java.util.TimeZone;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
public class RolloverFileOptionsMetadata {

	// The filename must include the string "yyyy_mm_dd" which is replaced with the actual date when creating and
	// rolling over the file.
	private String filename;

	// If true, existing files will be appended to.
	private boolean append = true;

	// The number of days to retain files before deleting them. 0 to retain forever.
	private int retainDays = 0;

	private String timeZoneID = TimeZone.getDefault().getID();

	// The format for the date file substitution. The default is "yyyy_MM_dd".
	private String dateFormat = "yyyy_MM_dd";

	// The format for the file extension of backup files. The default is "HHmmssSSS".
	private String backupFormat = "HHmmssSSS";

	// Output stream buffer size. If set to -1 no buffering is used
	private int bufferSize = 8192;

	// After how many messages the output buffer is flushed. If if zero then flush only on rollover event
	private long flushRate = 0;

	// How often to rollover files starting from NOW. If zero or non-positive the 24h period is set and it start at
	// midnight.
	private long rolloverPeriod = 1000L * 60 * 60 * 24;

	// Fix a maximal file size after which a roll over is triggered. Non positive values means that the file size roll
	// over is deactivated.
	private long maxRolledFileSize = -1;

	@NotBlank
	public String getFilename() {
		return filename;
	}

	@ModuleOption("The filename must include the string yyyy_mm_dd which is replaced with the actual date when creating and rolling over the file.")
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public boolean isAppend() {
		return append;
	}

	@ModuleOption(value = "If true, existing files will be appended to", defaultValue = "true")
	public void setAppend(boolean append) {
		this.append = append;
	}

	@Min(0)
	@NotNull
	public int getRetainDays() {
		return retainDays;
	}

	@ModuleOption(value = "The number of days to retain files before deleting them. 0 to retain forever.", defaultValue = "0")
	public void setRetainDays(int retainDays) {
		this.retainDays = retainDays;
	}

	@NotNull
	public String getDateFormat() {
		return dateFormat;
	}

	@ModuleOption(value = "The format for the date file substitution. The default is \"yyyy_MM_dd\".", defaultValue = "yyyy_MM_dd")
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	@NotNull
	public String getBackupFormat() {
		return backupFormat;
	}

	@ModuleOption(value = "The format for the file extension of backup files. The default is \"HHmmssSSS\".", defaultValue = "HHmmssSSS")
	public void setBackupFormat(String backupFormat) {
		this.backupFormat = backupFormat;
	}

	@NotNull
	public String getTimeZoneID() {
		return timeZoneID;
	}

	@ModuleOption(value = "TimeZone ID", defaultValue = "Europe/Amsterdam")
	public void setTimeZoneID(String timeZoneID) {
		this.timeZoneID = timeZoneID;
	}

	@NotNull
	public int getBufferSize() {
		return bufferSize;
	}

	@ModuleOption(value = "Output stream buffer size. If set to -1 no buffering is used", defaultValue = "8192")
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public long getFlushRate() {
		return flushRate;
	}

	@ModuleOption(value = "After how many messages the output buffer is flushed. When zero it flushes on rollover event only", defaultValue = "0")
	public void setFlushRate(long flushRate) {
		this.flushRate = flushRate;
	}

	public long getRolloverPeriod() {
		return rolloverPeriod;
	}

	@ModuleOption(value = "Time period between two consecutive roll over tasks (in milliseconds). If set to -1 then it defaults to 24 hours period starting from midnight.", defaultValue = "86400000")
	public void setRolloverPeriod(long rolloverPeriod) {
		this.rolloverPeriod = rolloverPeriod;
	}

	public long getMaxRolledFileSize() {
		return maxRolledFileSize;
	}

	@ModuleOption(value = "File size in bytes. When reached the file is rolled over. Set -1 to disable.", defaultValue = "-1")
	public void setMaxRolledFileSize(long maxRolledFileSize) {
		this.maxRolledFileSize = maxRolledFileSize;
	}
}
