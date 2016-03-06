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

public class RolloverFileOptionsMetadataTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testModuleOptions() {
		ModuleOptionsMetadataResolver moduleOptionsMetadataResolver = new DefaultModuleOptionsMetadataResolver();
		String resource = "classpath:/";
		ModuleDefinition definition = ModuleDefinitions.simple("rollover-file",
				sink, resource);
		ModuleOptionsMetadata metadata = moduleOptionsMetadataResolver
				.resolve(definition);

		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed("filename"),
						moduleOptionNamed("append"),
						moduleOptionNamed("retainDays"),
						moduleOptionNamed("timeZoneID"),
						moduleOptionNamed("dateFormat"),
						moduleOptionNamed("backupFormat"),
						moduleOptionNamed("bufferSize"),
						moduleOptionNamed("flushRate"),
						moduleOptionNamed("startRolloverNow"),
						moduleOptionNamed("rolloverPeriod")));

		for (ModuleOption moduleOption : metadata) {
			System.out.println(moduleOption);
			if (moduleOption.getName().equals("filename")) {
				assertEquals(null, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("append")) {
				assertEquals(true, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("retainDays")) {
				assertEquals(0, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("timeZoneID")) {
				assertEquals("Europe/Amsterdam", moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("dateFormat")) {
				assertEquals("yyyy_MM_dd", moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("backupFormat")) {
				assertEquals("HHmmssSSS", moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("bufferSize")) {
				assertEquals(8192, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("flushRate")) {
				assertEquals(0L, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("startRolloverNow")) {
				assertEquals(false, moduleOption.getDefaultValue());
			}
			if (moduleOption.getName().equals("rolloverPeriod")) {
				assertEquals(86400000L, moduleOption.getDefaultValue());
			}			
		}
	}

	public static Matcher<ModuleOption> moduleOptionNamed(String name) {
		return hasProperty("name", equalTo(name));
	}
}
