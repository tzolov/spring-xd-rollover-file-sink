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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.ModuleType.sink;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

import java.util.TimeZone;

public class RolloverFileOptionsMetadataTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testModuleOptions() {
		ModuleOptionsMetadataResolver moduleOptionsMetadataResolver = new DefaultModuleOptionsMetadataResolver();
		String resource = "classpath:/";
		ModuleDefinition definition = ModuleDefinitions.simple("rollover-file", sink, resource);
		ModuleOptionsMetadata metadata = moduleOptionsMetadataResolver.resolve(definition);

		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed("filename"), moduleOptionNamed("append"),
						moduleOptionNamed("timeZoneID"), moduleOptionNamed("dateFormat"),
						moduleOptionNamed("bufferSize"), moduleOptionNamed("flushRate"),
						moduleOptionNamed("rolloverPeriod"), moduleOptionNamed("maxRolledFileSize"),
						moduleOptionNamed("archivePrefix"), moduleOptionNamed("compressArchive"),
						moduleOptionNamed("binary")));

		for (ModuleOption moduleOption : metadata) {
			if (moduleOption.getName().equals("filename")) {
				assertEquals(null, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("append")) {
				assertEquals(true, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("timeZoneID")) {
				assertEquals(TimeZone.getDefault().getID(), moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("dateFormat")) {
				assertEquals("yyyyMMdd_HHmmssSSS", moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("bufferSize")) {
				assertEquals(8192, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("flushRate")) {
				assertEquals(0L, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("rolloverPeriod")) {
				assertEquals(86400000L, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("maxRolledFileSize")) {
				assertEquals(-1L, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("archivePrefix")) {
				assertEquals("archive", moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("compressArchive")) {
				assertEquals(true, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("binary")) {
				assertEquals(false, moduleOption.getDefaultValue());
			}
		}
	}

	public static Matcher<ModuleOption> moduleOptionNamed(String name) {
		return hasProperty("name", equalTo(name));
	}
}
